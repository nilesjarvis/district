package com.district.core.download

import android.content.Context
import com.district.core.persistence.InMemoryDownloadStore
import com.district.domain.Album
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
}
