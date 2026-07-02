package com.district.jellyfinmono.core.persistence

import com.district.jellyfinmono.domain.PlaybackSnapshot

interface PlaybackStore {
    suspend fun load(): PlaybackSnapshot?
    suspend fun save(snapshot: PlaybackSnapshot)
    suspend fun clear()
}
