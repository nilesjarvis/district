package com.district.data.jellyfin

import com.district.core.network.FixedDeviceIdProvider
import com.district.domain.AuthSession
import com.district.domain.DistrictError
import com.district.domain.DistrictResult
import com.district.domain.ServerInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultJellyfinRepositoryTest {
    @Test
    fun authenticateUsesInjectedDeviceId() = runTest {
        val api = RecordingApi()
        val repository = DefaultJellyfinRepository(api, FixedDeviceIdProvider("fixed-device"))

        val result = repository.authenticate("http://server", "demo", "pw")

        assertTrue(result is DistrictResult.Success)
        assertEquals("fixed-device", api.deviceId)
    }

    @Test
    fun mapsUnauthorizedToExpiredToken() = runTest {
        val repository = DefaultJellyfinRepository(RecordingApi(failWith = JellyfinHttpException(401, "expired")), FixedDeviceIdProvider("id"))

        val result = repository.checkServer("http://server")

        assertEquals(DistrictResult.Failure(DistrictError.ExpiredToken), result)
    }

    @Test
    fun mapsAuthenticateUnauthorizedToRejectedCredentials() = runTest {
        val repository = DefaultJellyfinRepository(RecordingApi(failWith = JellyfinHttpException(401, "bad credentials")), FixedDeviceIdProvider("id"))

        val result = repository.authenticate("http://server", "demo", "wrong")

        assertEquals(DistrictResult.Failure(DistrictError.AuthRejected), result)
    }

    @Test
    fun mapsBadServerUrlToResultFailure() = runTest {
        val repository = DefaultJellyfinRepository(RecordingApi(failWith = IllegalArgumentException("invalid url")), FixedDeviceIdProvider("id"))

        val result = repository.checkServer("not a url")

        assertTrue(result is DistrictResult.Failure)
        assertTrue((result as DistrictResult.Failure).error is DistrictError.InvalidServerUrl)
    }

    private class RecordingApi(private val failWith: Exception? = null) : JellyfinApi {
        var deviceId: String? = null
        override suspend fun publicInfo(serverUrl: String): ServerInfo {
            failWith?.let { throw it }
            return ServerInfo(serverUrl, "Jellyfin", "10.9.6")
        }

        override suspend fun authenticate(serverUrl: String, username: String, password: String, deviceId: String): AuthSession {
            failWith?.let { throw it }
            this.deviceId = deviceId
            return AuthSession(serverUrl, "token", "user", username, deviceId)
        }

        override suspend fun libraries(session: AuthSession) = emptyList<com.district.domain.MusicLibrary>()
        override suspend fun albums(session: AuthSession, parentId: String?) = emptyList<com.district.domain.Album>()
        override suspend fun artistAlbums(session: AuthSession, artistId: String) = emptyList<com.district.domain.Album>()
        override suspend fun albumTracks(session: AuthSession, albumId: String) = emptyList<com.district.domain.Track>()
        override suspend fun tracksByIds(session: AuthSession, ids: List<String>) = emptyList<com.district.domain.Track>()
        override suspend fun search(session: AuthSession, query: String) = com.district.domain.SearchResults(emptyList(), emptyList(), emptyList())
    }
}
