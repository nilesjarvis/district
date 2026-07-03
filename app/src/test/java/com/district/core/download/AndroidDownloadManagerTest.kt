package com.district.core.download

import android.content.Context
import com.district.core.persistence.InMemoryDownloadStore
import com.district.domain.Album
import com.district.domain.DownloadedAlbum
import com.district.domain.DownloadedTrack
import com.district.domain.RemoteResource
import com.district.domain.Track
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AndroidDownloadManagerTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun album() = Album(
        id = "a1",
        title = "Ghost Notes",
        artist = "Molyneux",
        productionYear = 2024,
        trackCount = 2,
        coverArt = RemoteResource(server.url("/cover").toString(), null),
    )

    private fun tracks() = listOf(
        Track("t1", "First", "Molyneux", "a1", 1, 200_000L, RemoteResource(server.url("/audio/t1").toString(), null)),
        Track("t2", "Second", "Molyneux", "a1", 2, 220_000L, RemoteResource(server.url("/audio/t2").toString(), null)),
    )

    @Test
    fun downloadsTracksAndCoverThenPersists() = runTest {
        repeat(3) { server.enqueue(MockResponse().setBody("audio-bytes-$it")) } // t1, t2, cover
        val store = InMemoryDownloadStore()
        val manager = AndroidDownloadManager(
            context = context,
            client = OkHttpClient(),
            store = store,
            scope = this,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clockMs = { 42L },
        )

        manager.enqueue(album(), tracks())
        advanceUntilIdle()

        val downloads = manager.downloads.value
        assertEquals(1, downloads.size)
        val downloaded = downloads.first()
        assertEquals("a1", downloaded.id)
        assertEquals(2, downloaded.tracks.size)
        assertEquals(42L, downloaded.downloadedAtEpochMs)
        assertTrue("expected a non-zero total size", downloaded.sizeBytes > 0)
        assertTrue("track file must exist", File(downloaded.tracks[0].filePath).exists())
        assertTrue("cover must be saved", downloaded.coverPath != null && File(downloaded.coverPath!!).exists())
        // Persisted to the store.
        assertEquals(1, store.load().size)
        // No lingering active state.
        assertTrue(manager.activeDownloads.value.isEmpty())
        // Playback tracks point at local files.
        val local = manager.playableTracks("a1")!!
        assertEquals(2, local.size)
        assertTrue(local.first().stream!!.url.startsWith("file://"))
        assertEquals(null, local.first().stream!!.authHeaders)
    }

    @Test
    fun deleteRemovesFilesAndMetadata() = runTest {
        repeat(3) { server.enqueue(MockResponse().setBody("audio-bytes")) }
        val store = InMemoryDownloadStore()
        val manager = AndroidDownloadManager(context, OkHttpClient(), store, this, StandardTestDispatcher(testScheduler))

        manager.enqueue(album(), tracks())
        advanceUntilIdle()
        val filePath = manager.downloads.value.first().tracks.first().filePath
        assertTrue(File(filePath).exists())

        manager.delete("a1")
        advanceUntilIdle()

        assertTrue(manager.downloads.value.isEmpty())
        assertFalse("track file must be deleted", File(filePath).exists())
        assertTrue(store.load().isEmpty())
        assertEquals(null, manager.playableTracks("a1"))
    }

    @Test
    fun failedTrackDownloadReportsFailureAndCleansUp() = runTest {
        server.enqueue(MockResponse().setResponseCode(500)) // t1 fails immediately
        val store = InMemoryDownloadStore()
        val manager = AndroidDownloadManager(context, OkHttpClient(), store, this, StandardTestDispatcher(testScheduler))

        manager.enqueue(album(), tracks())
        advanceUntilIdle()

        assertTrue(manager.downloads.value.isEmpty())
        assertTrue(manager.activeDownloads.value["a1"] is com.district.domain.DownloadState.Failed)
        assertFalse("partial album dir must be cleaned up", File(context.filesDir, "downloads/a1").exists())
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun enqueueIsNoOpWhenAlreadyDownloaded() = runTest {
        repeat(3) { server.enqueue(MockResponse().setBody("audio")) }
        val manager = AndroidDownloadManager(context, OkHttpClient(), InMemoryDownloadStore(), this, StandardTestDispatcher(testScheduler))
        manager.enqueue(album(), tracks())
        advanceUntilIdle()
        assertEquals(1, manager.downloads.value.size)

        // Second enqueue should not hit the server again (no responses enqueued) and not change state.
        manager.enqueue(album(), tracks())
        advanceUntilIdle()

        assertEquals(1, manager.downloads.value.size)
    }

    @Test
    fun refreshPrunesDownloadsWithMissingTrackFilesAndOrphanDirs() = runTest {
        val missingAlbumDir = File(context.filesDir, "downloads/missing-album").apply { mkdirs() }
        File(missingAlbumDir, "partial").writeText("partial")
        val orphanDir = File(context.filesDir, "downloads/orphan-album")
        orphanDir.mkdirs()
        File(orphanDir, "orphan").writeText("stale")
        val missing = DownloadedAlbum(
            id = "missing-album",
            title = "Missing",
            artist = "Molyneux",
            productionYear = 2024,
            coverPath = null,
            tracks = listOf(
                DownloadedTrack(
                    id = "missing-track",
                    title = "Missing Track",
                    artist = "Molyneux",
                    albumId = "missing-album",
                    indexNumber = 1,
                    durationMs = 200_000L,
                    filePath = File(missingAlbumDir, "missing-track").absolutePath,
                    sizeBytes = 100L,
                ),
            ),
            downloadedAtEpochMs = 10L,
        )
        val store = InMemoryDownloadStore(listOf(missing))
        val manager = AndroidDownloadManager(context, OkHttpClient(), store, this, StandardTestDispatcher(testScheduler))

        manager.refresh()

        assertTrue(manager.downloads.value.isEmpty())
        assertTrue(store.load().isEmpty())
        assertEquals(null, manager.playableTracks("missing-album"))
        assertFalse("invalid album directory must be removed", missingAlbumDir.exists())
        assertFalse("orphan directory must be removed", orphanDir.exists())
    }

    @Test
    fun refreshDropsMissingCoverButKeepsPlayableTracks() = runTest {
        val albumDir = File(context.filesDir, "downloads/coverless-album").apply { mkdirs() }
        val trackFile = File(albumDir, "track").apply { writeText("audio") }
        val downloaded = DownloadedAlbum(
            id = "coverless-album",
            title = "Coverless",
            artist = "Molyneux",
            productionYear = 2024,
            coverPath = File(albumDir, "missing-cover").absolutePath,
            tracks = listOf(
                DownloadedTrack(
                    id = "track",
                    title = "Track",
                    artist = "Molyneux",
                    albumId = "coverless-album",
                    indexNumber = 1,
                    durationMs = 200_000L,
                    filePath = trackFile.absolutePath,
                    sizeBytes = trackFile.length(),
                ),
            ),
            downloadedAtEpochMs = 10L,
        )
        val store = InMemoryDownloadStore(listOf(downloaded))
        val manager = AndroidDownloadManager(context, OkHttpClient(), store, this, StandardTestDispatcher(testScheduler))

        manager.refresh()

        val refreshed = manager.downloads.value.single()
        assertEquals("coverless-album", refreshed.id)
        assertEquals(null, refreshed.coverPath)
        assertEquals(null, manager.playableTracks("coverless-album")!!.single().coverArt)
        assertEquals(null, store.load().single().coverPath)
    }

    @Test
    fun concurrentDownloadsBothPersistToCatalog() = runTest {
        server.enqueue(MockResponse().setBody("first-audio"))
        server.enqueue(MockResponse().setBody("second-audio"))
        val store = InMemoryDownloadStore()
        val manager = AndroidDownloadManager(context, OkHttpClient(), store, this, StandardTestDispatcher(testScheduler))
        val firstAlbum = Album("concurrent-1", "First", "Molyneux", 2024, 1, null)
        val secondAlbum = Album("concurrent-2", "Second", "Molyneux", 2024, 1, null)
        val firstTracks = listOf(Track("c1-t1", "First", "Molyneux", firstAlbum.id, 1, 200_000L, RemoteResource(server.url("/audio/c1").toString(), null)))
        val secondTracks = listOf(Track("c2-t1", "Second", "Molyneux", secondAlbum.id, 1, 200_000L, RemoteResource(server.url("/audio/c2").toString(), null)))

        manager.enqueue(firstAlbum, firstTracks)
        manager.enqueue(secondAlbum, secondTracks)
        advanceUntilIdle()

        assertEquals(listOf("concurrent-1", "concurrent-2"), manager.downloads.value.map { it.id }.sorted())
        assertEquals(listOf("concurrent-1", "concurrent-2"), store.load().map { it.id }.sorted())
    }
}
