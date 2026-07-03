package com.district.core.persistence

import com.district.domain.AuthSession
import com.district.domain.DownloadedAlbum
import com.district.domain.PlaybackSnapshot

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

class InMemoryDownloadStore(initial: List<DownloadedAlbum> = emptyList()) : DownloadStore {
    private var albums: List<DownloadedAlbum> = initial

    override suspend fun load(): List<DownloadedAlbum> = albums
    override suspend fun save(albums: List<DownloadedAlbum>) {
        this.albums = albums
    }
}
