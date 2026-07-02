package com.district.jellyfinmono.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.district.jellyfinmono.domain.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class Media3PlaybackController(context: Context) : PlaybackController {
    private val appContext = context.applicationContext
    private val controllerJob = SupervisorJob()
    private val scope = CoroutineScope(controllerJob + Dispatchers.Main.immediate)
    private var currentQueue: List<Track> = emptyList()
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)

    private val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(httpDataSourceFactory),
        )
        .build()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) = updateState()
            override fun onIsPlayingChanged(isPlaying: Boolean) = updateState()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.value = _state.value.copy(errorMessage = error.message)
            }
        })
        scope.launch {
            while (true) {
                updateState()
                delay(500)
            }
        }
    }

    override fun playQueue(queue: List<Track>, startIndex: Int, positionMs: Long, playWhenReady: Boolean) {
        val requestedStartId = queue.getOrNull(startIndex)?.id
        val playable = queue.filter { it.stream != null }
        if (playable.isEmpty()) return
        _state.value = _state.value.copy(errorMessage = null)
        playable.firstOrNull()?.stream?.authHeaders?.asMap()?.let { headers ->
            httpDataSourceFactory.setDefaultRequestProperties(headers)
        }
        currentQueue = playable
        val start = playable.indexOfFirst { it.id == requestedStartId }
            .takeIf { it >= 0 }
            ?: startIndex.coerceIn(0, playable.lastIndex)
        player.setMediaItems(
            playable.map { track ->
                MediaItem.Builder()
                    .setUri(track.stream!!.url)
                    .setMediaId(track.id)
                    .setRequestMetadata(
                        MediaItem.RequestMetadata.Builder()
                            .setMediaUri(android.net.Uri.parse(track.stream.url))
                            .build(),
                    )
                    .build()
            },
            start,
            positionMs.coerceAtLeast(0L),
        )
        player.prepare()
        player.playWhenReady = playWhenReady
        if (playWhenReady) player.play() else player.pause()
        updateState()
    }

    override fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
        updateState()
    }

    override fun next() {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
        updateState()
    }

    override fun previous() {
        if (player.currentPosition > 3000L) player.seekTo(0L) else if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
        updateState()
    }

    override fun seekToFraction(fraction: Float) {
        val duration = player.duration.takeIf { it > 0 } ?: return
        player.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
        updateState()
    }

    override fun setVolumeFraction(fraction: Float) {
        player.volume = fraction.coerceIn(0f, 1f)
        updateState()
    }

    override fun release() {
        controllerJob.cancel()
        player.release()
    }

    private fun updateState() {
        val current = currentQueue.getOrNull(player.currentMediaItemIndex)
        _state.value = PlayerState(
            queue = currentQueue,
            currentTrack = current,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0 } ?: current?.durationMs ?: 0L,
            volume = player.volume,
            errorMessage = _state.value.errorMessage,
        )
    }
}
