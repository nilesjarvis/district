package com.district.jellyfinmono.domain

interface JellyfinRepository {
    suspend fun checkServer(serverUrl: String): DistrictResult<ServerInfo>
    suspend fun authenticate(serverUrl: String, username: String, password: String): DistrictResult<AuthSession>
    suspend fun libraries(session: AuthSession): DistrictResult<List<MusicLibrary>>
    suspend fun albums(session: AuthSession, parentId: String? = null): DistrictResult<List<Album>>
    suspend fun artistAlbums(session: AuthSession, artistId: String): DistrictResult<List<Album>>
    suspend fun albumTracks(session: AuthSession, albumId: String): DistrictResult<List<Track>>
    suspend fun tracksByIds(session: AuthSession, ids: List<String>): DistrictResult<List<Track>>
    suspend fun search(session: AuthSession, query: String): DistrictResult<SearchResults>
}
