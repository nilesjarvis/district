package com.district.jellyfinmono.core.persistence

import com.district.jellyfinmono.domain.AuthSession
import com.district.jellyfinmono.domain.PlaybackSnapshot

class InMemorySessionStore(initial: AuthSession? = null) : SessionStore {
    private var session: AuthSession? = initial

    override suspend fun load(): AuthSession? = session
    override suspend fun save(session: AuthSession) {
        this.session = session
    }
    override suspend fun clear() {
        session = null
    }
}

class InMemoryPlaybackStore(initial: PlaybackSnapshot? = null) : PlaybackStore {
    private var snapshot: PlaybackSnapshot? = initial

    override suspend fun load(): PlaybackSnapshot? = snapshot
    override suspend fun save(snapshot: PlaybackSnapshot) {
        this.snapshot = snapshot
    }
    override suspend fun clear() {
        snapshot = null
    }
}
