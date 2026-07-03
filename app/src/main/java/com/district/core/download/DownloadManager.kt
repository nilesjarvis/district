package com.district.core.download

import com.district.domain.Album
import com.district.domain.DownloadedAlbum
import com.district.domain.DownloadState
import com.district.domain.Track
import kotlinx.coroutines.flow.StateFlow

interface DownloadManager {
    /** Completed downloads, backed by persistent storage. */
    val downloads: StateFlow<List<DownloadedAlbum>>

    /** In-progress or failed downloads, keyed by album id. */
    val activeDownloads: StateFlow<Map<String, DownloadState>>

    /** Load persisted downloads into [downloads]. */
    suspend fun refresh()

    /** Start downloading an album's tracks and cover in the background. No-op if already downloaded/downloading. */
    fun enqueue(album: Album, tracks: List<Track>)

    /** Remove a downloaded album and its files. */
    suspend fun delete(albumId: String)

    fun isDownloaded(albumId: String): Boolean

    /** Local file-backed tracks for a downloaded album, or null if it isn't downloaded. */
    fun playableTracks(albumId: String): List<Track>?
}
