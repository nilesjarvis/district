package com.district.core.download

import android.content.Context
import android.net.Uri
import com.district.core.persistence.DownloadStore
import com.district.domain.Album
import com.district.domain.DownloadState
import com.district.domain.DownloadedAlbum
import com.district.domain.DownloadedTrack
import com.district.domain.RemoteResource
import com.district.domain.Track
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class AndroidDownloadManager(
    context: Context,
    private val client: OkHttpClient,
    private val store: DownloadStore,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clockMs: () -> Long = System::currentTimeMillis,
) : DownloadManager {
    private val downloadsDir = File(context.applicationContext.filesDir, "downloads")
    private val catalogMutex = Mutex()
    private val downloadJobs = mutableMapOf<String, DownloadJob>()

    private val _downloads = MutableStateFlow<List<DownloadedAlbum>>(emptyList())
    override val downloads: StateFlow<List<DownloadedAlbum>> = _downloads.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    override val activeDownloads: StateFlow<Map<String, DownloadState>> = _activeDownloads.asStateFlow()

    override suspend fun refresh() {
        catalogMutex.withLock {
            val keepActiveIds = activeAlbumIds()
            val reconciled = withContext(ioDispatcher) {
                val loaded = store.load()
                val valid = loaded.mapNotNull { it.reconciledIfPlayable() }
                cleanOrphanDirs(valid.mapTo(mutableSetOf()) { it.id } + keepActiveIds)
                if (valid != loaded) store.save(valid)
                valid
            }
            _downloads.value = reconciled
        }
    }

    override fun enqueue(album: Album, tracks: List<Track>) {
        if (isDownloaded(album.id)) return
        synchronized(downloadJobs) {
            if (downloadJobs.containsKey(album.id)) return
            if (_activeDownloads.value[album.id] is DownloadState.InProgress) return
            val entry = DownloadJob()
            downloadJobs[album.id] = entry
            entry.job = scope.launch {
                try {
                    downloadInternal(album, tracks)
                } finally {
                    synchronized(downloadJobs) {
                        if (downloadJobs[album.id] === entry) {
                            downloadJobs.remove(album.id)
                        }
                    }
                }
            }
        }
    }

    private suspend fun downloadInternal(album: Album, tracks: List<Track>) {
        val playable = tracks.filter { it.stream != null }
        if (playable.isEmpty()) {
            _activeDownloads.update { it + (album.id to DownloadState.Failed("No downloadable tracks")) }
            return
        }
        _activeDownloads.update { it + (album.id to DownloadState.InProgress(0, playable.size)) }
        val albumDir = File(downloadsDir, album.id)
        try {
            val downloaded = withContext(ioDispatcher) {
                albumDir.mkdirs()
                val downloadedTracks = ArrayList<DownloadedTrack>(playable.size)
                playable.forEachIndexed { index, track ->
                    val file = File(albumDir, track.id)
                    fetchTo(track.stream!!, file)
                    downloadedTracks += DownloadedTrack(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        albumId = track.albumId,
                        indexNumber = track.indexNumber,
                        durationMs = track.durationMs,
                        filePath = file.absolutePath,
                        sizeBytes = file.length(),
                    )
                    _activeDownloads.update { it + (album.id to DownloadState.InProgress(index + 1, playable.size)) }
                }
                val coverPath = album.coverArt?.let { resource ->
                    val coverFile = File(albumDir, "cover")
                    runCatching { fetchTo(resource, coverFile) }.map { coverFile.absolutePath }.getOrNull()
                }
                DownloadedAlbum(
                    id = album.id,
                    title = album.title,
                    artist = album.artist,
                    productionYear = album.productionYear,
                    coverPath = coverPath,
                    tracks = downloadedTracks,
                    downloadedAtEpochMs = clockMs(),
                )
            }
            catalogMutex.withLock {
                val updated = _downloads.value.filterNot { it.id == album.id } + downloaded
                withContext(ioDispatcher) { store.save(updated) }
                _downloads.value = updated
            }
            _activeDownloads.update { it - album.id }
        } catch (cancellation: CancellationException) {
            withContext(ioDispatcher) { albumDir.deleteRecursively() }
            _activeDownloads.update { it - album.id }
            throw cancellation
        } catch (error: Exception) {
            withContext(ioDispatcher) { albumDir.deleteRecursively() }
            _activeDownloads.update { it + (album.id to DownloadState.Failed(error.message ?: "Download failed")) }
        }
    }

    private fun fetchTo(resource: RemoteResource, file: File) {
        val builder = Request.Builder().url(resource.url)
        resource.authHeaders?.asMap()?.forEach { (name, value) -> builder.header(name, value) }
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty response body")
            file.outputStream().use { output -> body.byteStream().copyTo(output) }
        }
        if (file.length() <= 0L) throw IOException("Downloaded file was empty")
    }

    override suspend fun delete(albumId: String) {
        synchronized(downloadJobs) {
            downloadJobs[albumId]?.job?.cancel()
        }
        catalogMutex.withLock {
            val updated = withContext(ioDispatcher) {
                File(downloadsDir, albumId).deleteRecursively()
                val next = _downloads.value.filterNot { it.id == albumId }
                store.save(next)
                next
            }
            _downloads.value = updated
        }
        _activeDownloads.update { it - albumId }
    }

    override fun isDownloaded(albumId: String): Boolean = _downloads.value.any { it.id == albumId }

    override fun playableTracks(albumId: String): List<Track>? {
        val album = _downloads.value.firstOrNull { it.id == albumId } ?: return null
        val cover = album.coverPath?.let { RemoteResource(url = fileUri(it), authHeaders = null) }
        return album.tracks.map { track ->
            Track(
                id = track.id,
                title = track.title,
                artist = track.artist,
                albumId = track.albumId,
                indexNumber = track.indexNumber,
                durationMs = track.durationMs,
                stream = RemoteResource(url = fileUri(track.filePath), authHeaders = null),
                coverArt = cover,
            )
        }
    }

    private fun fileUri(path: String): String = Uri.fromFile(File(path)).toString()

    private fun activeAlbumIds(): Set<String> =
        synchronized(downloadJobs) { downloadJobs.keys.toSet() }

    private fun DownloadedAlbum.reconciledIfPlayable(): DownloadedAlbum? {
        if (!hasPlayableTrackFiles()) return null
        val validCoverPath = coverPath?.takeIf { fileIsUsable(it) }
        return if (validCoverPath == coverPath) this else copy(coverPath = validCoverPath)
    }

    private fun DownloadedAlbum.hasPlayableTrackFiles(): Boolean =
        tracks.isNotEmpty() && tracks.all { fileIsUsable(it.filePath) }

    private fun fileIsUsable(path: String): Boolean =
        File(path).let { it.isFile && it.length() > 0L }

    private fun cleanOrphanDirs(keepAlbumIds: Set<String>) {
        downloadsDir.listFiles()
            ?.filter { it.isDirectory && it.name !in keepAlbumIds }
            ?.forEach { it.deleteRecursively() }
    }

    private class DownloadJob {
        var job: Job? = null
    }
}
