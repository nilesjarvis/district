package com.district.jellyfinmono.data.jellyfin

import com.district.jellyfinmono.domain.Album
import com.district.jellyfinmono.domain.Artist
import com.district.jellyfinmono.domain.AuthHeaders
import com.district.jellyfinmono.domain.AuthSession
import com.district.jellyfinmono.domain.MusicLibrary
import com.district.jellyfinmono.domain.RemoteResource
import com.district.jellyfinmono.domain.SearchResults
import com.district.jellyfinmono.domain.ServerInfo
import com.district.jellyfinmono.domain.Track

interface JellyfinApi {
    suspend fun publicInfo(serverUrl: String): ServerInfo
    suspend fun authenticate(serverUrl: String, username: String, password: String, deviceId: String): AuthSession
    suspend fun libraries(session: AuthSession): List<MusicLibrary>
    suspend fun albums(session: AuthSession, parentId: String? = null): List<Album>
    suspend fun albumTracks(session: AuthSession, albumId: String): List<Track>
    suspend fun tracksByIds(session: AuthSession, ids: List<String>): List<Track>
    suspend fun search(session: AuthSession, query: String): SearchResults
}

internal data class JellyfinItem(
    val id: String,
    val type: String,
    val name: String,
    val albumArtist: String?,
    val artists: List<String>,
    val parentId: String?,
    val collectionType: String?,
    val productionYear: Int?,
    val childCount: Int?,
    val indexNumber: Int?,
    val runtimeTicks: Long?,
)

internal fun JellyfinItem.toAlbum(baseUrl: String, authHeaders: AuthHeaders?): Album =
    Album(
        id = id,
        title = name,
        artist = albumArtist ?: artists.firstOrNull().orEmpty(),
        productionYear = productionYear,
        trackCount = childCount,
        coverArt = RemoteResource(
            url = "$baseUrl/Items/$id/Images/Primary?maxWidth=300",
            authHeaders = authHeaders,
        ),
        tintArgb = stableTintArgb(id),
    )

internal fun JellyfinItem.toTrack(baseUrl: String, authHeaders: AuthHeaders?): Track =
    Track(
        id = id,
        title = name,
        artist = artists.firstOrNull().orEmpty(),
        albumId = parentId,
        indexNumber = indexNumber,
        durationMs = runtimeTicks?.ticksToMillis() ?: 0L,
        stream = RemoteResource(
            url = "$baseUrl/Audio/$id/universal",
            authHeaders = authHeaders,
        ),
        coverArt = RemoteResource(
            url = "$baseUrl/Items/$id/Images/Primary?maxWidth=96",
            authHeaders = authHeaders,
        ),
        tintArgb = stableTintArgb(id),
    )

internal fun Long.ticksToMillis(): Long = this / 10_000L

private fun stableTintArgb(id: String): Long {
    val palette = longArrayOf(
        0xFF5A4931,
        0xFF424B68,
        0xFF354A3C,
        0xFF51384F,
        0xFF5A463F,
        0xFF2F5654,
    )
    val index = id.fold(0) { acc, char -> (acc * 31 + char.code) and Int.MAX_VALUE } % palette.size
    return palette[index]
}
