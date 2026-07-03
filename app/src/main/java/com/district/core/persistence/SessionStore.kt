package com.district.core.persistence

import com.district.domain.AuthSession

interface SessionStore {
    suspend fun load(): AuthSession?
    suspend fun save(session: AuthSession)
    suspend fun clear()
}
