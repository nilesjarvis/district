package com.district.jellyfinmono.core.persistence

import com.district.jellyfinmono.domain.AuthSession

interface SessionStore {
    suspend fun load(): AuthSession?
    suspend fun save(session: AuthSession)
    suspend fun clear()
}
