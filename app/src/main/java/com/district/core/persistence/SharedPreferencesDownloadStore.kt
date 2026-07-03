package com.district.core.persistence

import android.content.Context
import com.district.domain.DownloadedAlbum
import com.district.domain.DownloadedTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SharedPreferencesDownloadStore(context: Context) : DownloadStore {
    private val preferences = context.applicationContext.getSharedPreferences("downloads", Context.MODE_PRIVATE)

    override suspend fun load(): List<DownloadedAlbum> = withContext(Dispatchers.IO) {
        val payload = preferences.getString(KEY_PAYLOAD, null) ?: return@withContext emptyList()
        try {
            val array = JSONArray(payload)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let(::albumFromJson)
            }
        } catch (_: Exception) {
            preferences.edit().clear().apply()
            emptyList()
        }
    }

    override suspend fun save(albums: List<DownloadedAlbum>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        albums.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_PAYLOAD, array.toString()).apply()
    }

    private fun DownloadedAlbum.toJson(): JSONObject {
        val trackArray = JSONArray()
        tracks.forEach { track ->
            trackArray.put(
                JSONObject()
                    .put("id", track.id)
                    .put("title", track.title)
                    .put("artist", track.artist)
                    .put("albumId", track.albumId ?: JSONObject.NULL)
                    .put("indexNumber", track.indexNumber ?: JSONObject.NULL)
                    .put("durationMs", track.durationMs)
                    .put("filePath", track.filePath)
                    .put("sizeBytes", track.sizeBytes),
            )
        }
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("artist", artist)
            .put("productionYear", productionYear ?: JSONObject.NULL)
            .put("coverPath", coverPath ?: JSONObject.NULL)
            .put("downloadedAtEpochMs", downloadedAtEpochMs)
            .put("tracks", trackArray)
    }

    private fun albumFromJson(json: JSONObject): DownloadedAlbum? {
        val id = json.optString("id").ifBlank { return null }
        val trackArray = json.optJSONArray("tracks") ?: JSONArray()
        val tracks = (0 until trackArray.length()).mapNotNull { index ->
            trackArray.optJSONObject(index)?.let { t ->
                val trackId = t.optString("id").ifBlank { return@mapNotNull null }
                val filePath = t.optString("filePath").ifBlank { return@mapNotNull null }
                DownloadedTrack(
                    id = trackId,
                    title = t.optString("title"),
                    artist = t.optString("artist"),
                    albumId = t.optString("albumId").ifBlank { null },
                    indexNumber = if (t.has("indexNumber") && !t.isNull("indexNumber")) t.optInt("indexNumber") else null,
                    durationMs = t.optLong("durationMs"),
                    filePath = filePath,
                    sizeBytes = t.optLong("sizeBytes"),
                )
            }
        }
        if (tracks.isEmpty()) return null
        return DownloadedAlbum(
            id = id,
            title = json.optString("title"),
            artist = json.optString("artist"),
            productionYear = if (json.has("productionYear") && !json.isNull("productionYear")) json.optInt("productionYear") else null,
            coverPath = json.optString("coverPath").ifBlank { null },
            tracks = tracks,
            downloadedAtEpochMs = json.optLong("downloadedAtEpochMs"),
        )
    }

    private companion object {
        const val KEY_PAYLOAD = "payload"
    }
}
