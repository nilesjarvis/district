package com.district.data.jellyfin

import com.district.core.network.DeviceIdProvider
import com.district.domain.AuthSession
import com.district.domain.DistrictError
import com.district.domain.DistrictResult
import com.district.domain.JellyfinRepository
import java.io.IOException

class DefaultJellyfinRepository(
    private val api: JellyfinApi,
    private val deviceIdProvider: DeviceIdProvider,
) : JellyfinRepository {
    override suspend fun checkServer(serverUrl: String) =
        runCatchingDistrict { api.publicInfo(serverUrl) }

    override suspend fun authenticate(serverUrl: String, username: String, password: String) =
        runCatchingDistrict(unauthorizedError = DistrictError.AuthRejected) {
            api.authenticate(
                serverUrl = serverUrl,
                username = username,
                password = password,
                deviceId = deviceIdProvider.deviceId(),
            )
        }

    override suspend fun libraries(session: AuthSession) =
        runCatchingDistrict { api.libraries(session) }

    override suspend fun albums(session: AuthSession, parentId: String?) =
        runCatchingDistrict { api.albums(session, parentId) }

    override suspend fun artistAlbums(session: AuthSession, artistId: String) =
        runCatchingDistrict { api.artistAlbums(session, artistId) }

    override suspend fun albumTracks(session: AuthSession, albumId: String) =
        runCatchingDistrict { api.albumTracks(session, albumId) }

    override suspend fun tracksByIds(session: AuthSession, ids: List<String>) =
        runCatchingDistrict { api.tracksByIds(session, ids) }

    override suspend fun search(session: AuthSession, query: String) =
        runCatchingDistrict { api.search(session, query) }
}

private inline fun <T> runCatchingDistrict(
    unauthorizedError: DistrictError = DistrictError.ExpiredToken,
    block: () -> T,
): DistrictResult<T> =
    try {
        DistrictResult.Success(block())
    } catch (error: JellyfinHttpException) {
        when (error.code) {
            401, 403 -> DistrictResult.Failure(unauthorizedError)
            else -> DistrictResult.Failure(DistrictError.Http(error.code, error.message))
        }
    } catch (error: JellyfinParseException) {
        DistrictResult.Failure(DistrictError.Parse(error.message))
    } catch (error: IOException) {
        DistrictResult.Failure(DistrictError.Network(error.message ?: "Network request failed"))
    } catch (error: IllegalArgumentException) {
        DistrictResult.Failure(DistrictError.InvalidServerUrl(error.message ?: "Invalid server URL"))
    }
