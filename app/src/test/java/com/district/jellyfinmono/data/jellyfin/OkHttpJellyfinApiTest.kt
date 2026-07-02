package com.district.jellyfinmono.data.jellyfin

import com.district.jellyfinmono.core.network.TestDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OkHttpJellyfinApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: OkHttpJellyfinApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = OkHttpJellyfinApi(
            client = OkHttpClient(),
            dispatchers = TestDispatcherProvider(Dispatchers.Unconfined),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun publicInfoParsesServerVersion() = runTest {
        server.enqueue(json("""{"ServerName":"LAN","Version":"10.9.6"}"""))

        val info = api.publicInfo(server.url("/").toString())

        assertEquals("LAN", info.serverName)
        assertEquals("10.9.6", info.version)
        assertEquals("/System/Info/Public", server.takeRequest().path)
    }

    @Test
    fun authenticatePostsCredentialsAndDoesNotExposeTokenInToString() = runTest {
        server.enqueue(json("""{"AccessToken":"secret-token","User":{"Id":"user-1","Name":"demo"}}"""))

        val session = api.authenticate(server.url("/").toString(), "demo", "password", "device-1")

        val request = server.takeRequest()
        assertEquals("/Users/AuthenticateByName", request.path)
        assertTrue(request.getHeader("Authorization")!!.contains("Client=\"Jellyfin Mono\""))
        assertTrue(request.body.readUtf8().contains("\"Pw\":\"password\""))
        assertEquals("secret-token", session.accessToken)
        assertFalse(session.toString().contains("secret-token"))
    }

    @Test
    fun albumsTracksAndSearchParseItems() = runTest {
        val session = authSession()
        server.enqueue(json("""{"Items":[{"Id":"album-1","Type":"MusicAlbum","Name":"Ghost Notes","AlbumArtist":"Molyneux","ProductionYear":2024,"ChildCount":3}]}"""))
        server.enqueue(json("""{"Items":[{"Id":"track-1","Type":"Audio","Name":"Second Person","Artists":["Molyneux"],"ParentId":"album-1","IndexNumber":2,"RunTimeTicks":3020000000}]}"""))
        server.enqueue(json("""{"Items":[{"Id":"track-2","Type":"Audio","Name":"First Person","Artists":["Molyneux"]},{"Id":"track-1","Type":"Audio","Name":"Second Person","Artists":["Molyneux"]}]}"""))
        server.enqueue(json("""{"Items":[{"Id":"album-1","Type":"MusicAlbum","Name":"Ghost Notes","AlbumArtist":"Molyneux"},{"Id":"track-1","Type":"Audio","Name":"Second Person","Artists":["Molyneux"],"AlbumId":"album-1"},{"Id":"artist-1","Type":"MusicArtist","Name":"Molyneux"}]}"""))

        val albums = api.albums(session)
        val tracks = api.albumTracks(session, "album-1")
        val restoredTracks = api.tracksByIds(session, listOf("track-1", "track-2"))
        val results = api.search(session, "mol")

        assertEquals("Ghost Notes", albums.single().title)
        assertEquals("${session.serverUrl}/Items/album-1/Images/Primary?maxWidth=300", albums.single().coverArt!!.url)
        assertNotNull(albums.single().coverArt!!.authHeaders)
        assertEquals(302000L, tracks.single().durationMs)
        assertEquals("${session.serverUrl}/Items/album-1/Images/Primary?maxWidth=96", tracks.single().coverArt!!.url)
        val streamUrl = tracks.single().stream!!.url.toHttpUrl()
        assertEquals("/Audio/track-1/universal", streamUrl.encodedPath)
        assertEquals("user-1", streamUrl.queryParameter("UserId"))
        assertEquals("device-1", streamUrl.queryParameter("DeviceId"))
        assertTrue(streamUrl.queryParameter("Container")!!.contains("flac"))
        assertEquals("http", streamUrl.queryParameter("TranscodingProtocol"))
        assertNull("Access token must not appear in the stream URL", streamUrl.queryParameter("api_key"))
        assertFalse(tracks.single().stream!!.url.contains("token"))
        assertNotNull(tracks.single().stream!!.authHeaders)
        assertEquals(listOf("track-1", "track-2"), restoredTracks.map { it.id })
        assertEquals(3, results.totalCount)
        assertEquals("${session.serverUrl}/Items/album-1/Images/Primary?maxWidth=96", results.tracks.single().coverArt!!.url)

        val albumsRequest = server.takeRequest()
        assertEquals("/Users/user-1/Items", albumsRequest.requestUrl!!.encodedPath)
        assertEquals("MusicAlbum", albumsRequest.requestUrl!!.queryParameter("IncludeItemTypes"))
        assertEquals("DateCreated", albumsRequest.requestUrl!!.queryParameter("SortBy"))
        assertEquals("Descending", albumsRequest.requestUrl!!.queryParameter("SortOrder"))
        assertEquals("true", albumsRequest.requestUrl!!.queryParameter("Recursive"))
        assertEquals("token", albumsRequest.getHeader("X-Emby-Token"))
        assertTrue(albumsRequest.getHeader("Authorization")!!.contains("Token=\"token\""))

        val tracksRequest = server.takeRequest()
        assertEquals("/Users/user-1/Items", tracksRequest.requestUrl!!.encodedPath)
        assertEquals("album-1", tracksRequest.requestUrl!!.queryParameter("ParentId"))
        assertEquals("IndexNumber", tracksRequest.requestUrl!!.queryParameter("SortBy"))
        assertEquals("token", tracksRequest.getHeader("X-Emby-Token"))

        val restoredRequest = server.takeRequest()
        assertEquals("track-1,track-2", restoredRequest.requestUrl!!.queryParameter("Ids"))
        assertEquals("Audio", restoredRequest.requestUrl!!.queryParameter("IncludeItemTypes"))
        assertEquals("token", restoredRequest.getHeader("X-Emby-Token"))

        val searchRequest = server.takeRequest()
        assertEquals("mol", searchRequest.requestUrl!!.queryParameter("searchTerm"))
        assertEquals("MusicAlbum,Audio,MusicArtist", searchRequest.requestUrl!!.queryParameter("IncludeItemTypes"))
        assertEquals("true", searchRequest.requestUrl!!.queryParameter("Recursive"))
    }

    @Test
    fun artistAlbumsQueriesByAlbumArtistIdAndParsesArtistId() = runTest {
        val session = authSession()
        server.enqueue(json("""{"Items":[{"Id":"album-1","Type":"MusicAlbum","Name":"All Eyez On Me","ProductionYear":1996,"AlbumArtist":"2pac","AlbumArtists":[{"Id":"artist-1","Name":"2pac"}]}]}"""))

        val albums = api.artistAlbums(session, "artist-1")

        assertEquals("All Eyez On Me", albums.single().title)
        assertEquals("artist-1", albums.single().artistId)
        val request = server.takeRequest()
        assertEquals("/Users/user-1/Items", request.requestUrl!!.encodedPath)
        assertEquals("MusicAlbum", request.requestUrl!!.queryParameter("IncludeItemTypes"))
        assertEquals("artist-1", request.requestUrl!!.queryParameter("AlbumArtistIds"))
        assertEquals("true", request.requestUrl!!.queryParameter("Recursive"))
        assertEquals("ProductionYear,SortName", request.requestUrl!!.queryParameter("SortBy"))
        assertEquals("token", request.getHeader("X-Emby-Token"))
    }

    @Test
    fun albumsCanScopeToLibraryParent() = runTest {
        val session = authSession()
        server.enqueue(json("""{"Items":[]}"""))

        api.albums(session, parentId = "music-view")

        val request = server.takeRequest()
        assertEquals("music-view", request.requestUrl!!.queryParameter("ParentId"))
        assertEquals("true", request.requestUrl!!.queryParameter("Recursive"))
    }

    @Test
    fun librariesParseMusicViews() = runTest {
        val session = authSession()
        server.enqueue(json("""{"Items":[{"Id":"view-1","Name":"Music","CollectionType":"music"}]}"""))

        val libraries = api.libraries(session)

        assertEquals("Music", libraries.single().name)
        assertEquals("music", libraries.single().collectionType)
        val request = server.takeRequest()
        assertEquals("/Users/user-1/Views", request.requestUrl!!.encodedPath)
        assertEquals("token", request.getHeader("X-Emby-Token"))
    }

    private fun authSession() =
        com.district.jellyfinmono.domain.AuthSession(
            serverUrl = server.url("/").toString().trimEnd('/'),
            accessToken = "token",
            userId = "user-1",
            username = "demo",
            deviceId = "device-1",
        )

    private fun json(body: String): MockResponse =
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(body)
}
