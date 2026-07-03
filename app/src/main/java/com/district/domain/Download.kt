package com.district.domain

data class DownloadedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val albumId: String?,
    val indexNumber: Int?,
    val durationMs: Long,
    val filePath: String,
    val sizeBytes: Long,
)

data class DownloadedAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val productionYear: Int?,
    val coverPath: String?,
    val tracks: List<DownloadedTrack>,
    val downloadedAtEpochMs: Long,
) {
    val sizeBytes: Long get() = tracks.sumOf { it.sizeBytes }
    val trackCount: Int get() = tracks.size
    val durationMs: Long get() = tracks.sumOf { it.durationMs }
}

/** Transient status of an album that is downloading or failed; downloaded albums live in the store. */
sealed interface DownloadState {
    data class InProgress(val completedTracks: Int, val totalTracks: Int) : DownloadState {
        val fraction: Float get() = if (totalTracks <= 0) 0f else completedTracks.toFloat() / totalTracks.toFloat()
    }

    data class Failed(val message: String) : DownloadState
}
