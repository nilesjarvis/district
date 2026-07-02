package com.district.jellyfinmono.core.media

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Shares stream auth state between the UI-side playback controller and [PlaybackService].
 * The service is instantiated by the system, so it cannot receive constructor dependencies;
 * both sides run in the same process.
 */
object PlaybackSessionBridge {
    @Volatile
    private var streamHeaders: Map<String, String> = emptyMap()

    private val _authErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val authErrors: SharedFlow<Unit> = _authErrors

    fun setStreamHeaders(headers: Map<String, String>?) {
        streamHeaders = headers.orEmpty()
    }

    fun streamHeaders(): Map<String, String> = streamHeaders

    fun reportAuthError() {
        _authErrors.tryEmit(Unit)
    }
}
