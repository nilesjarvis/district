package com.district.core.persistence

import android.content.Context
import com.district.domain.PlaybackSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SharedPreferencesPlaybackStore(context: Context) : PlaybackStore {
    private val preferences = context.applicationContext.getSharedPreferences("playback", Context.MODE_PRIVATE)

    override suspend fun load(): PlaybackSnapshot? = withContext(Dispatchers.IO) {
        val payload = preferences.getString(KEY_PAYLOAD, null) ?: return@withContext null
        try {
            val json = JSONObject(payload)
            val queue = json.optJSONArray("queueIds") ?: JSONArray()
            val queueIds = (0 until queue.length()).mapNotNull { queue.optString(it).takeIf(String::isNotBlank) }
            val currentTrackId = json.optString("currentTrackId").ifBlank { null }
            if (queueIds.isEmpty() || currentTrackId.isNullOrBlank()) {
                preferences.edit().clear().apply()
                return@withContext null
            }
            PlaybackSnapshot(
                queueIds = queueIds,
                currentTrackId = currentTrackId,
                positionMs = json.optLong("positionMs").coerceAtLeast(0L),
                updatedAtEpochMs = json.optLong("updatedAtEpochMs"),
            )
        } catch (_: Exception) {
            preferences.edit().clear().apply()
            null
        }
    }

    override suspend fun save(snapshot: PlaybackSnapshot) = withContext(Dispatchers.IO) {
        val json = JSONObject()
            .put("queueIds", JSONArray(snapshot.queueIds))
            .put("currentTrackId", snapshot.currentTrackId)
            .put("positionMs", snapshot.positionMs)
            .put("updatedAtEpochMs", snapshot.updatedAtEpochMs)
            .toString()
        preferences.edit().putString(KEY_PAYLOAD, json).apply()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_PAYLOAD = "payload"
    }
}
