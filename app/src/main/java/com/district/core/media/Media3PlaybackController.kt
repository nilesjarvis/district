package com.district.core.media

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.district.domain.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UI-side client for [PlaybackService]. Playback itself lives in the service so it survives
 * activity teardown; this class must only be touched from the main thread.
 */
class Media3PlaybackController(context: Context) : PlaybackController {
    private val appContext = context.applicationContext
    private val controllerJob = SupervisorJob()
    private val scope = CoroutineScope(controllerJob + Dispatchers.Main.immediate)
    private var currentQueue: List<Track> = emptyList()
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state

    private var controller: MediaController? = null
    private val pendingCommands = ArrayDeque<(MediaController) -> Unit>()
    private var positionTicker: Job? = null

    private val controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)),
    ).buildAsync()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _state.value = _state.value.copy(errorMessage = null, isAuthError = false)
            }
            updateState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) startPositionTicker() else stopPositionTicker()
            updateState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                isPlaying = false,
                errorMessage = error.message ?: "Playback failed",
            )
        }
    }

    init {
        controllerFuture.addListener({
            val connected = runCatching { controllerFuture.get() }.getOrNull()
            if (connected == null) {
                _state.value = _state.value.copy(errorMessage = "Playback service unavailable")
                return@addListener
            }
            controller = connected
            connected.addListener(playerListener)
            while (pendingCommands.isNotEmpty()) pendingCommands.removeFirst()(connected)
            updateState()
        }, ContextCompat.getMainExecutor(appContext))
        scope.launch {
            PlaybackSessionBridge.authErrors.collect {
                controller?.pause()
                _state.value = _state.value.copy(
                    isPlaying = false,
                    playWhenReady = false,
                    isAuthError = true,
                    errorMessage = "Session expired",
                )
            }
        }
    }

    override fun playQueue(queue: List<Track>, startIndex: Int, positionMs: Long, playWhenReady: Boolean) {
        val requestedStartId = queue.getOrNull(startIndex)?.id
        val playable = queue.filter { it.stream != null }
        if (playable.isEmpty()) return
        PlaybackSessionBridge.setStreamHeaders(playable.first().stream?.authHeaders?.asMap())
        currentQueue = playable
        _state.value = _state.value.copy(errorMessage = null, isAuthError = false)
        val start = playable.indexOfFirst { it.id == requestedStartId }
            .takeIf { it >= 0 }
            ?: startIndex.coerceIn(0, playable.lastIndex)
        val items = playable.map { track ->
            MediaItem.Builder()
                .setUri(track.stream!!.url)
                .setMediaId(track.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .build(),
                )
                .build()
        }
        runWhenConnected { player ->
            player.setMediaItems(items, start, positionMs.coerceAtLeast(0L))
            player.prepare()
            player.playWhenReady = playWhenReady
            updateState()
        }
    }

    override fun playPause() = runWhenConnected { player ->
        when {
            player.isPlaying -> player.pause()
            player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0 -> {
                // IDLE with a queue means playback failed; prepare() retries from the current position.
                _state.value = _state.value.copy(errorMessage = null, isAuthError = false)
                player.prepare()
                player.play()
            }
            player.playbackState == Player.STATE_ENDED -> {
                player.seekTo(0, 0L)
                player.play()
            }
            else -> player.play()
        }
        updateState()
    }

    override fun next() = runWhenConnected { player ->
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
        updateState()
    }

    override fun previous() = runWhenConnected { player ->
        if (player.currentPosition > 3000L) player.seekTo(0L) else if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
        updateState()
    }

    override fun seekToFraction(fraction: Float) = runWhenConnected { player ->
        val duration = player.duration.takeIf { it > 0 } ?: return@runWhenConnected
        player.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
        updateState()
    }

    override fun setVolumeFraction(fraction: Float) = runWhenConnected { player ->
        player.volume = fraction.coerceIn(0f, 1f)
        updateState()
    }

    override fun release() {
        stopPositionTicker()
        controllerJob.cancel()
        controller?.removeListener(playerListener)
        controller = null
        MediaController.releaseFuture(controllerFuture)
    }

    private fun runWhenConnected(command: (MediaController) -> Unit) {
        val connected = controller
        if (connected != null) command(connected) else pendingCommands.addLast(command)
    }

    private fun startPositionTicker() {
        if (positionTicker?.isActive == true) return
        positionTicker = scope.launch {
            while (isActive) {
                updateState()
                delay(500)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTicker?.cancel()
        positionTicker = null
    }

    private fun updateState() {
        val player = controller ?: return
        val currentId = player.currentMediaItem?.mediaId
        val current = currentQueue.firstOrNull { it.id == currentId }
            ?: currentQueue.getOrNull(player.currentMediaItemIndex)
        _state.value = _state.value.copy(
            queue = currentQueue,
            currentTrack = current,
            isPlaying = player.isPlaying,
            playWhenReady = player.playWhenReady,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0 } ?: current?.durationMs ?: 0L,
            volume = player.volume,
        )
    }
}
