package com.district.core.persistence

import com.district.domain.AuthSession
import com.district.domain.PlaybackSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryStoresTest {
    @Test
    fun sessionStoreSavesAndClearsSession() = runTest {
        val store = InMemorySessionStore()
        val session = AuthSession("http://server", "token", "user", "demo", "device")

        store.save(session)
        assertEquals(session, store.load())
        store.clear()

        assertNull(store.load())
    }

    @Test
    fun playbackStoreSavesAndClearsSnapshot() = runTest {
        val store = InMemoryPlaybackStore()
        val snapshot = PlaybackSnapshot(listOf("a", "b"), "b", 1200L, 42L)

        store.save(snapshot)
        assertEquals(snapshot, store.load())
        store.clear()

        assertNull(store.load())
    }
}
