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
import okhttp3.HttpUrl.Companion.toHttpUrl

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

internal fun JellyfinItem.toTrack(
    session: AuthSession,
    authHeaders: AuthHeaders?,
    albumIdOverride: String? = null,
): Track {
    val albumItemId = albumIdOverride?.takeIf { it.isNotBlank() } ?: parentId
    val coverItemId = albumItemId ?: id
    return Track(
        id = id,
        title = name,
        artist = artists.firstOrNull().orEmpty(),
        albumId = albumItemId,
        indexNumber = indexNumber,
        durationMs = runtimeTicks?.ticksToMillis() ?: 0L,
        stream = RemoteResource(
            url = buildUniversalAudioUrl(session, id),
            authHeaders = authHeaders,
        ),
        coverArt = RemoteResource(
            url = "${session.serverUrl}/Items/$coverItemId/Images/Primary?maxWidth=96",
            authHeaders = authHeaders,
        ),
        tintArgb = stableTintArgb(coverItemId),
    )
}

// The /Audio/{id}/universal endpoint returns an empty 200 unless the request states what the
// client can play. Container drives direct play; the Transcoding* fields give a progressive
// (non-HLS) fallback the core ExoPlayer can read without the HLS module. Auth stays in headers,
// so the access token never lands in the URL (and thus never in logs).
internal const val SUPPORTED_AUDIO_CONTAINERS =
    "mp3,aac,m4a,alac,flac,ogg,oga,opus,wav,webma,mka"

internal fun buildUniversalAudioUrl(session: AuthSession, itemId: String): String =
    session.serverUrl.toHttpUrl().newBuilder()
        .addPathSegment("Audio")
        .addPathSegment(itemId)
        .addPathSegment("universal")
        .addQueryParameter("UserId", session.userId)
        .addQueryParameter("DeviceId", session.deviceId)
        .addQueryParameter("Container", SUPPORTED_AUDIO_CONTAINERS)
        .addQueryParameter("TranscodingContainer", "mp3")
        .addQueryParameter("TranscodingProtocol", "http")
        .addQueryParameter("AudioCodec", "mp3")
        .addQueryParameter("MaxStreamingBitrate", "140000000")
        .build()
        .toString()

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
