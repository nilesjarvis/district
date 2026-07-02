package com.district.jellyfinmono.data.jellyfin

import com.district.jellyfinmono.core.network.DispatcherProvider
import com.district.jellyfinmono.domain.Artist
import com.district.jellyfinmono.domain.AuthHeaders
import com.district.jellyfinmono.domain.AuthSession
import com.district.jellyfinmono.domain.MusicLibrary
import com.district.jellyfinmono.domain.SearchResults
import com.district.jellyfinmono.domain.ServerInfo
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class JellyfinHttpException(val code: Int, override val message: String) : IOException(message)
class JellyfinParseException(override val message: String) : IOException(message)

class OkHttpJellyfinApi(
    private val client: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) : JellyfinApi {
    override suspend fun publicInfo(serverUrl: String): ServerInfo = withContext(dispatchers.io) {
        val baseUrl = normalizeServerUrl(serverUrl)
        val json = getJson(baseUrl, "/System/Info/Public", session = null)
        ServerInfo(
            serverUrl = baseUrl,
            serverName = json.optString("ServerName").ifBlank { "Jellyfin" },
            version = json.optString("Version").ifBlank { "unknown" },
        )
    }

    override suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String,
        deviceId: String,
    ): AuthSession = withContext(dispatchers.io) {
        val baseUrl = normalizeServerUrl(serverUrl)
        val body = JSONObject()
            .put("Username", username)
            .put("Pw", password)
            .toString()
            .toRequestBody(JSON)
        val request = Request.Builder()
            .url(url(baseUrl, "/Users/AuthenticateByName"))
            .post(body)
            .header("Authorization", authorizationHeader(deviceId = deviceId, token = null))
            .build()
        val json = executeJson(request)
        val token = json.optString("AccessToken")
        val user = json.optJSONObject("User")
        val userId = user?.optString("Id").orEmpty()
        if (token.isBlank() || userId.isBlank()) {
            throw JellyfinParseException("Authentication response omitted token or user id")
        }
        AuthSession(
            serverUrl = baseUrl,
            accessToken = token,
            userId = userId,
            username = user?.optString("Name").orEmpty().ifBlank { username },
            deviceId = deviceId,
        )
    }

    override suspend fun libraries(session: AuthSession): List<MusicLibrary> = withContext(dispatchers.io) {
        val json = getJson(session.serverUrl, "/Users/${session.userId}/Views", session)
        json.items().map {
            MusicLibrary(
                id = it.optString("Id"),
                name = it.optString("Name"),
                collectionType = it.optString("CollectionType").ifBlank { null },
            )
        }.filter { it.id.isNotBlank() }
    }

    override suspend fun albums(session: AuthSession, parentId: String?) = withContext(dispatchers.io) {
        val json = getJson(
            baseUrl = session.serverUrl,
            path = "/Users/${session.userId}/Items",
            session = session,
            query = buildMap {
                put("IncludeItemTypes", "MusicAlbum")
                put("SortBy", "DateCreated")
                put("SortOrder", "Descending")
                put("Recursive", "true")
                if (!parentId.isNullOrBlank()) put("ParentId", parentId)
            },
        )
        json.items().map { it.toJellyfinItem().toAlbum(session.serverUrl, session.authHeaders()) }
    }

    override suspend fun albumTracks(session: AuthSession, albumId: String) = withContext(dispatchers.io) {
        val json = getJson(
            baseUrl = session.serverUrl,
            path = "/Users/${session.userId}/Items",
            session = session,
            query = mapOf(
                "ParentId" to albumId,
                "SortBy" to "IndexNumber",
            ),
        )
        json.items().map { it.toJellyfinItem().toTrack(session.serverUrl, session.authHeaders()) }
    }

    override suspend fun tracksByIds(session: AuthSession, ids: List<String>) = withContext(dispatchers.io) {
        if (ids.isEmpty()) return@withContext emptyList()
        val json = getJson(
            baseUrl = session.serverUrl,
            path = "/Users/${session.userId}/Items",
            session = session,
            query = mapOf(
                "Ids" to ids.joinToString(","),
                "IncludeItemTypes" to "Audio",
            ),
        )
        val byId = json.items()
            .map { it.toJellyfinItem().toTrack(session.serverUrl, session.authHeaders()) }
            .associateBy { it.id }
        ids.mapNotNull { byId[it] }
    }

    override suspend fun search(session: AuthSession, query: String): SearchResults = withContext(dispatchers.io) {
        val json = getJson(
            baseUrl = session.serverUrl,
            path = "/Users/${session.userId}/Items",
            session = session,
            query = mapOf(
                "searchTerm" to query,
                "IncludeItemTypes" to "MusicAlbum,Audio,MusicArtist",
                "Recursive" to "true",
            ),
        )
        val albums = mutableListOf<com.district.jellyfinmono.domain.Album>()
        val tracks = mutableListOf<com.district.jellyfinmono.domain.Track>()
        val artists = mutableListOf<com.district.jellyfinmono.domain.Artist>()
        json.items().forEach { itemJson ->
            val item = itemJson.toJellyfinItem()
            when (item.type) {
                "MusicAlbum" -> albums += item.toAlbum(session.serverUrl, session.authHeaders())
                "Audio" -> tracks += item.toTrack(session.serverUrl, session.authHeaders())
                "MusicArtist" -> artists += Artist(id = item.id, name = item.name)
            }
        }
        SearchResults(albums = albums, tracks = tracks, artists = artists)
    }

    private fun getJson(
        baseUrl: String,
        path: String,
        session: AuthSession?,
        query: Map<String, String> = emptyMap(),
    ): JSONObject {
        val builder = Request.Builder()
            .url(url(baseUrl, path, query))
            .get()
        if (session != null) {
            builder.header("Authorization", authorizationHeader(session.deviceId, session.accessToken))
            builder.header("X-Emby-Token", session.accessToken)
        }
        return executeJson(builder.build())
    }

    private fun executeJson(request: Request): JSONObject {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw JellyfinHttpException(response.code, body.ifBlank { response.message })
            }
            return try {
                JSONObject(body)
            } catch (error: Exception) {
                throw JellyfinParseException(error.message ?: "Invalid JSON response")
            }
        }
    }

    private fun url(baseUrl: String, path: String, query: Map<String, String> = emptyMap()): String {
        val builder = baseUrl.toHttpUrl().newBuilder()
        path.trim('/').split('/').filter { it.isNotBlank() }.forEach { builder.addPathSegment(it) }
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    private fun JSONObject.items(): List<JSONObject> {
        val array = optJSONArray("Items") ?: JSONArray()
        return (0 until array.length()).mapNotNull { index -> array.optJSONObject(index) }
    }

    private fun JSONObject.toJellyfinItem(): JellyfinItem =
        JellyfinItem(
            id = optString("Id"),
            type = optString("Type"),
            name = optString("Name"),
            albumArtist = optString("AlbumArtist").ifBlank { null },
            artists = optJSONArray("Artists")?.strings().orEmpty(),
            parentId = optString("ParentId").ifBlank { optString("AlbumId").ifBlank { null } },
            collectionType = optString("CollectionType").ifBlank { null },
            productionYear = if (has("ProductionYear")) optInt("ProductionYear") else null,
            childCount = if (has("ChildCount")) optInt("ChildCount") else null,
            indexNumber = if (has("IndexNumber")) optInt("IndexNumber") else null,
            runtimeTicks = if (has("RunTimeTicks")) optLong("RunTimeTicks") else null,
        )

    private fun JSONArray.strings(): List<String> =
        (0 until length()).mapNotNull { index -> optString(index).takeIf { it.isNotBlank() } }

    private fun authorizationHeader(deviceId: String, token: String?): String =
        buildString {
            append("MediaBrowser Client=\"Jellyfin Mono\", Device=\"Android\", ")
            append("DeviceId=\"$deviceId\", Version=\"1.0\"")
            if (!token.isNullOrBlank()) append(", Token=\"$token\"")
        }

    private fun AuthSession.authHeaders(): AuthHeaders =
        AuthHeaders(
            authorization = authorizationHeader(deviceId, accessToken),
            token = accessToken,
        )

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

fun normalizeServerUrl(serverUrl: String): String =
    serverUrl.trim().trimEnd('/')
