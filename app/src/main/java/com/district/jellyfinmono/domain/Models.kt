package com.district.jellyfinmono.domain

class AuthSession(
    val serverUrl: String,
    val accessToken: String,
    val userId: String,
    val username: String,
    val deviceId: String,
) {
    override fun toString(): String =
        "AuthSession(serverUrl=$serverUrl, userId=$userId, username=$username, deviceId=$deviceId)"
}

data class ServerInfo(
    val serverUrl: String,
    val serverName: String,
    val version: String,
)

data class MusicLibrary(
    val id: String,
    val name: String,
    val collectionType: String?,
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val productionYear: Int?,
    val trackCount: Int?,
    val coverArt: RemoteResource?,
    val tintArgb: Long? = null,
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val albumId: String?,
    val indexNumber: Int?,
    val durationMs: Long,
    val stream: RemoteResource?,
    val coverArt: RemoteResource? = null,
    val tintArgb: Long? = null,
)

class AuthHeaders(
    private val authorization: String,
    private val token: String,
) {
    fun asMap(): Map<String, String> =
        mapOf(
            "Authorization" to authorization,
            "X-Emby-Token" to token,
        )

    override fun toString(): String = "AuthHeaders(redacted=true)"
}

data class RemoteResource(
    val url: String,
    val authHeaders: AuthHeaders?,
) {
    override fun toString(): String =
        "RemoteResource(url=$url, authHeaders=${authHeaders != null})"
}

data class Artist(
    val id: String,
    val name: String,
)

data class SearchResults(
    val albums: List<Album>,
    val tracks: List<Track>,
    val artists: List<Artist>,
) {
    val totalCount: Int get() = albums.size + tracks.size + artists.size
}

data class PlaybackSnapshot(
    val queueIds: List<String>,
    val currentTrackId: String?,
    val positionMs: Long,
    val updatedAtEpochMs: Long,
)
