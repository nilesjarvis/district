package com.district.core.persistence

import com.district.domain.PlaybackSnapshot

interface PlaybackStore {
    suspend fun load(): PlaybackSnapshot?
    suspend fun save(snapshot: PlaybackSnapshot)
    suspend fun clear()
}
