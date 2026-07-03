package com.district.core.persistence

import android.content.Context
import com.district.domain.PlaybackSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesPlaybackStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun savesAndLoadsValidSnapshot() = runTest {
        val store = SharedPreferencesPlaybackStore(context)
        val snapshot = PlaybackSnapshot(
            queueIds = listOf("track-1", "track-2"),
            currentTrackId = "track-2",
            positionMs = 42_000L,
            updatedAtEpochMs = 99L,
        )

        store.save(snapshot)

        assertEquals(snapshot, store.load())
    }

    @Test
    fun clearsMalformedPayload() = runTest {
        val preferences = playbackPreferences()
        preferences.edit().putString("payload", "{not json").commit()
        val store = SharedPreferencesPlaybackStore(context)

        assertNull(store.load())
        assertNull(preferences.getString("payload", null))
    }

    @Test
    fun clearsSemanticallyInvalidPayload() = runTest {
        val preferences = playbackPreferences()
        preferences.edit()
            .putString(
                "payload",
                """{"queueIds":[],"currentTrackId":"","positionMs":100,"updatedAtEpochMs":99}""",
            )
            .commit()
        val store = SharedPreferencesPlaybackStore(context)

        assertNull(store.load())
        assertNull(preferences.getString("payload", null))
    }

    private fun playbackPreferences() =
        context.getSharedPreferences("playback", Context.MODE_PRIVATE)
}
