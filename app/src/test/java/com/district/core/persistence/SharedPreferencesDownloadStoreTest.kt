package com.district.core.persistence

import android.content.Context
import com.district.domain.DownloadedAlbum
import com.district.domain.DownloadedTrack
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesDownloadStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun savesAndLoadsAlbumsRoundTrip() = runTest {
        val store = SharedPreferencesDownloadStore(context)
        val album = DownloadedAlbum(
            id = "a1",
            title = "Ghost Notes",
            artist = "Molyneux",
            productionYear = 2024,
            coverPath = "/data/downloads/a1/cover",
            tracks = listOf(
                DownloadedTrack("t1", "First", "Molyneux", "a1", 1, 200_000L, "/data/downloads/a1/t1", 1_234L),
                DownloadedTrack("t2", "Second", "Molyneux", "a1", 2, 220_000L, "/data/downloads/a1/t2", 4_321L),
            ),
            downloadedAtEpochMs = 55L,
        )

        store.save(listOf(album))
        val loaded = store.load()

        assertEquals(1, loaded.size)
        val reloaded = loaded.first()
        assertEquals("a1", reloaded.id)
        assertEquals("Ghost Notes", reloaded.title)
        assertEquals(2024, reloaded.productionYear)
        assertEquals("/data/downloads/a1/cover", reloaded.coverPath)
        assertEquals(2, reloaded.tracks.size)
        assertEquals(5_555L, reloaded.sizeBytes)
        assertEquals("t2", reloaded.tracks[1].id)
        assertEquals(4_321L, reloaded.tracks[1].sizeBytes)
    }

    @Test
    fun loadsEmptyWhenNothingSaved() = runTest {
        assertTrue(SharedPreferencesDownloadStore(context).load().isEmpty())
    }

    @Test
    fun recoversFromMalformedPayload() = runTest {
        val preferences = context.getSharedPreferences("downloads", Context.MODE_PRIVATE)
        preferences.edit().putString("payload", "[not json").commit()

        assertTrue(SharedPreferencesDownloadStore(context).load().isEmpty())
        assertNull(preferences.getString("payload", null))
    }
}
