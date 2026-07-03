package com.district.jellyfinmono.core.media

import com.district.jellyfinmono.domain.Track
import kotlinx.coroutines.flow.StateFlow

data class PlayerState(
    val queue: List<Track> = emptyList(),
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    // True whenever the user intends playback to continue, even while momentarily buffering or
    // seeking. isPlaying drops to false during a scrub's re-buffer; playWhenReady does not, so UI
    // that should persist through a seek (the control zone) must key off this, not isPlaying.
    val playWhenReady: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
    val errorMessage: String? = null,
    val isAuthError: Boolean = false,
)

interface PlaybackController {
    val state: StateFlow<PlayerState>
    fun playQueue(queue: List<Track>, startIndex: Int, positionMs: Long = 0L, playWhenReady: Boolean = true)
    fun playPause()
    fun next()
    fun previous()
    fun seekToFraction(fraction: Float)
    fun setVolumeFraction(fraction: Float)
    fun release()
}
