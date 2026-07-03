package com.district.app

import com.district.core.network.TestDispatcherProvider
import com.district.core.media.PlaybackController
import com.district.core.download.DownloadManager
import com.district.core.media.PlayerState
import com.district.domain.DownloadState
import com.district.domain.DownloadedAlbum
import com.district.domain.DownloadedTrack
import com.district.domain.RemoteResource
import com.district.core.persistence.InMemoryPlaybackStore
import com.district.core.persistence.InMemorySessionStore
import com.district.core.persistence.SessionStore
import com.district.domain.Album
import com.district.domain.Artist
import com.district.domain.AuthSession
import com.district.domain.DistrictError
import com.district.domain.DistrictResult
import com.district.domain.JellyfinRepository
import com.district.domain.MusicLibrary
import com.district.domain.PlaybackSnapshot
import com.district.domain.SearchResults
import com.district.domain.ServerInfo
import com.district.domain.Track
import com.district.feature.onboarding.OnboardingStep
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppViewModelTest {
    @Test
    fun startsAtWelcomeWhenNoSessionExists() = runTest {
        val viewModel = viewModel()

        val state = viewModel.uiState.value as AppUiState.Onboarding

        assertEquals(OnboardingStep.Welcome, state.state.step)
    }

    @Test
    fun checkServerAdvancesToSignInWithServerInfo() = runTest {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)

        viewModel.connectServer()
        viewModel.updateServerAddress("192.168.1.50")
        viewModel.checkServer()

        val state = viewModel.uiState.value as AppUiState.Onboarding
        assertEquals(OnboardingStep.SignIn, state.state.step)
        assertEquals("10.9.6", state.state.serverInfo!!.version)
        assertEquals("http://192.168.1.50:8096", repository.checkedUrl)
    }

    @Test
    fun signInRejectedStaysOnSignInWithAuthError() = runTest {
        val repository = FakeRepository(authResult = DistrictResult.Failure(DistrictError.AuthRejected))
        val viewModel = viewModel(repository)

        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()

        val state = viewModel.uiState.value as AppUiState.Onboarding
        assertEquals(OnboardingStep.SignIn, state.state.step)
        assertEquals(DistrictError.AuthRejected, state.state.error)
    }

    @Test
    fun signInSuccessLoadsLibrariesAndPersistsSession() = runTest {
        val sessionStore = InMemorySessionStore()
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val repository = FakeRepository(authResult = DistrictResult.Success(session))
        val viewModel = viewModel(repository, sessionStore)

        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.updatePassword("pw")
        viewModel.signIn()

        val state = viewModel.uiState.value as AppUiState.Onboarding
        assertEquals(OnboardingStep.Connected, state.state.step)
        assertEquals(listOf("Music"), state.state.libraries.map { it.name })
        assertEquals(session, sessionStore.load())

        viewModel.enterLibrary()
        val library = viewModel.uiState.value as AppUiState.Library
        assertEquals(session, library.state.session)
        assertEquals("music", library.state.selectedLibraryId)
    }

    @Test
    fun secureSessionSaveFailureStopsSignInWithStorageError() = runTest {
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val viewModel = viewModel(
            repository = FakeRepository(authResult = DistrictResult.Success(session)),
            sessionStore = ThrowingSessionStore(),
        )

        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()

        val state = viewModel.uiState.value as AppUiState.Onboarding
        assertEquals(OnboardingStep.SignIn, state.state.step)
        assertFalse(state.state.isLoading)
        assertTrue(state.state.error is DistrictError.Storage)
    }

    @Test
    fun failedLibraryLoadDoesNotPersistSession() = runTest {
        val sessionStore = InMemorySessionStore()
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val repository = FakeRepository(
            authResult = DistrictResult.Success(session),
            libraryResult = DistrictResult.Failure(DistrictError.Network("down")),
        )
        val viewModel = viewModel(repository, sessionStore)

        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()

        val state = viewModel.uiState.value as AppUiState.Onboarding
        assertEquals(OnboardingStep.SignIn, state.state.step)
        assertEquals(DistrictError.Network("down"), state.state.error)
        assertEquals(null, sessionStore.load())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loadingGuardPreventsDuplicateServerChecks() = runTest {
        val repository = FakeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = viewModel(repository, dispatcher = dispatcher)

        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.checkServer()

        assertEquals(0, repository.checkCount)
        runCurrent()
        assertEquals(1, repository.checkCount)
    }

    @Test
    fun restoresPersistedSessionToLibrary() = runTest {
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val viewModel = viewModel(sessionStore = InMemorySessionStore(session))

        assertEquals(session, (viewModel.uiState.value as AppUiState.Library).state.session)
    }

    @Test
    fun expiredPersistedSessionIsClearedAndReturnsToSignIn() = runTest {
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val sessionStore = InMemorySessionStore(session)
        val viewModel = viewModel(
            repository = FakeRepository(libraryResult = DistrictResult.Failure(DistrictError.ExpiredToken)),
            sessionStore = sessionStore,
        )

        val state = viewModel.uiState.value as AppUiState.Onboarding
        assertEquals(OnboardingStep.SignIn, state.state.step)
        assertEquals("http://server", state.state.serverAddress)
        assertEquals("demo", state.state.username)
        assertEquals(DistrictError.ExpiredToken, state.state.error)
        assertEquals(null, sessionStore.load())
    }

    @Test
    fun openAlbumLoadsTrackLedger() = runTest {
        val album = Album("album-1", "Ghost Notes", "Molyneux", 2024, 2, null)
        val track = Track("track-1", "Second Person", "Molyneux", "album-1", 2, 302000L, null)
        val repository = FakeRepository(
            albumsResult = DistrictResult.Success(listOf(album)),
            tracksResult = DistrictResult.Success(listOf(track)),
        )
        val viewModel = viewModel(repository)

        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()
        viewModel.enterLibrary()
        viewModel.openAlbum(album)

        val state = (viewModel.uiState.value as AppUiState.Library).state
        assertEquals(LibraryRoute.AlbumDetail, state.route)
        assertEquals(album, state.selectedAlbum)
        assertEquals(listOf(track), state.albumTracks)
    }

    @Test
    fun albumTrackFailureStopsLoadingAndSurfacesError() = runTest {
        val album = Album("album-1", "Ghost Notes", "Molyneux", 2024, 2, null)
        val viewModel = viewModel(
            repository = FakeRepository(tracksResult = DistrictResult.Failure(DistrictError.Network("tracks down"))),
        )

        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()
        viewModel.enterLibrary()
        viewModel.openAlbum(album)

        val state = (viewModel.uiState.value as AppUiState.Library).state
        assertFalse(state.isAlbumLoading)
        assertEquals(DistrictError.Network("tracks down"), state.error)
        assertEquals(emptyList<Track>(), state.albumTracks)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun searchDebouncesLiveQueries() = runTest {
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val repository = FakeRepository(
            searchResult = DistrictResult.Success(
                SearchResults(
                    albums = listOf(Album("album-1", "Ghost Notes", "Molyneux", 2024, 3, null)),
                    tracks = emptyList(),
                    artists = emptyList(),
                ),
            ),
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = viewModel(
            repository = repository,
            sessionStore = InMemorySessionStore(session),
            dispatcher = dispatcher,
        )
        runCurrent()

        viewModel.activateSearch()
        viewModel.updateSearchQuery("g")
        viewModel.updateSearchQuery("gh")
        advanceTimeBy(149)
        runCurrent()
        assertEquals(0, repository.searchCount)

        advanceTimeBy(1)
        runCurrent()

        val state = (viewModel.uiState.value as AppUiState.Library).state
        assertEquals(1, repository.searchCount)
        assertEquals("gh", repository.lastSearchQuery)
        assertEquals("Ghost Notes", state.searchResults.albums.single().title)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun searchFailureStopsLoadingAndSurfacesError() = runTest {
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = viewModel(
            repository = FakeRepository(searchResult = DistrictResult.Failure(DistrictError.Network("search down"))),
            sessionStore = InMemorySessionStore(session),
            dispatcher = dispatcher,
        )
        runCurrent()

        viewModel.activateSearch()
        viewModel.updateSearchQuery("ghost")
        advanceTimeBy(150)
        runCurrent()

        val state = (viewModel.uiState.value as AppUiState.Library).state
        assertFalse(state.isSearching)
        assertEquals(DistrictError.Network("search down"), state.error)
        assertEquals(0, state.searchResults.totalCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun searchFailureAfterSuccessClearsStaleResults() = runTest {
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val repository = FakeRepository(
            searchResult = DistrictResult.Success(
                SearchResults(
                    albums = listOf(Album("album-1", "Ghost Notes", "Molyneux", 2024, 3, null)),
                    tracks = emptyList(),
                    artists = emptyList(),
                ),
            ),
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = viewModel(
            repository = repository,
            sessionStore = InMemorySessionStore(session),
            dispatcher = dispatcher,
        )
        runCurrent()

        viewModel.activateSearch()
        viewModel.updateSearchQuery("ghost")
        advanceTimeBy(150)
        runCurrent()
        repository.searchResult = DistrictResult.Failure(DistrictError.Network("search down"))
        viewModel.updateSearchQuery("ghosts")
        advanceTimeBy(150)
        runCurrent()

        val state = (viewModel.uiState.value as AppUiState.Library).state
        assertEquals(DistrictError.Network("search down"), state.error)
        assertEquals(0, state.searchResults.totalCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun expiredTokenDuringSearchClearsSessionAndReturnsToSignIn() = runTest {
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val sessionStore = InMemorySessionStore(session)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = viewModel(
            repository = FakeRepository(searchResult = DistrictResult.Failure(DistrictError.ExpiredToken)),
            sessionStore = sessionStore,
            dispatcher = dispatcher,
        )
        runCurrent()

        viewModel.activateSearch()
        viewModel.updateSearchQuery("ghost")
        advanceTimeBy(150)
        runCurrent()

        val state = viewModel.uiState.value as AppUiState.Onboarding
        assertEquals(OnboardingStep.SignIn, state.state.step)
        assertEquals(DistrictError.ExpiredToken, state.state.error)
        assertEquals(null, sessionStore.load())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun albumOpenedFromSearchReturnsToSearchResults() = runTest {
        val album = Album("album-1", "Ghost Notes", "Molyneux", 2024, 3, null)
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val repository = FakeRepository(
            searchResult = DistrictResult.Success(SearchResults(listOf(album), emptyList(), emptyList())),
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = viewModel(
            repository = repository,
            sessionStore = InMemorySessionStore(session),
            dispatcher = dispatcher,
        )
        runCurrent()

        viewModel.activateSearch()
        viewModel.updateSearchQuery("ghost")
        advanceTimeBy(150)
        runCurrent()
        viewModel.openAlbum(album)
        runCurrent()
        viewModel.backToLibrary()

        val state = (viewModel.uiState.value as AppUiState.Library).state
        assertEquals(LibraryRoute.Search, state.route)
        assertEquals("ghost", state.searchQuery)
    }

    @Test
    fun playAlbumFromStartQueuesLoadedAlbumTracksAtStart() = runTest {
        val album = Album("album-1", "Ghost Notes", "Molyneux", 2024, 2, null)
        val first = Track("track-1", "First", "Molyneux", "album-1", 1, 200000L, null)
        val second = Track("track-2", "Second", "Molyneux", "album-1", 2, 220000L, null)
        val playbackController = FakePlaybackController()
        val viewModel = viewModel(
            repository = FakeRepository(tracksResult = DistrictResult.Success(listOf(first, second))),
            playbackController = playbackController,
        )

        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()
        viewModel.enterLibrary()
        viewModel.openAlbum(album)
        viewModel.playAlbumFromStart()

        assertEquals(listOf("track-1", "track-2"), playbackController.lastQueue.map { it.id })
        assertEquals(0, playbackController.lastStartIndex)
        assertTrue(playbackController.lastPlayWhenReady)
    }

    @Test
    fun playTrackFromAlbumUsesAlbumQueueAndTappedIndex() = runTest {
        val album = Album("album-1", "Ghost Notes", "Molyneux", 2024, 2, null)
        val first = Track("track-1", "First", "Molyneux", "album-1", 1, 200000L, null)
        val second = Track("track-2", "Second", "Molyneux", "album-1", 2, 220000L, null)
        val playbackController = FakePlaybackController()
        val viewModel = viewModel(
            repository = FakeRepository(tracksResult = DistrictResult.Success(listOf(first, second))),
            playbackController = playbackController,
        )

        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()
        viewModel.enterLibrary()
        viewModel.openAlbum(album)
        viewModel.playTrack(second)

        assertEquals(listOf("track-1", "track-2"), playbackController.lastQueue.map { it.id })
        assertEquals(1, playbackController.lastStartIndex)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun playTrackFromSearchUsesSearchTrackQueueAndTappedIndex() = runTest {
        val first = Track("track-1", "First", "Molyneux", "album-1", 1, 200000L, null)
        val second = Track("track-2", "Second", "Molyneux", "album-1", 2, 220000L, null)
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val repository = FakeRepository(
            searchResult = DistrictResult.Success(SearchResults(emptyList(), listOf(first, second), emptyList())),
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val playbackController = FakePlaybackController()
        val viewModel = viewModel(
            repository = repository,
            sessionStore = InMemorySessionStore(session),
            playbackController = playbackController,
            dispatcher = dispatcher,
        )
        runCurrent()

        viewModel.activateSearch()
        viewModel.updateSearchQuery("sec")
        advanceTimeBy(150)
        runCurrent()
        viewModel.playTrack(second)

        assertEquals(listOf("track-1", "track-2"), playbackController.lastQueue.map { it.id })
        assertEquals(1, playbackController.lastStartIndex)
    }

    @Test
    fun playTrackNotInQueueFallsBackToSingleTrackQueue() = runTest {
        val album = Album("album-1", "Ghost Notes", "Molyneux", 2024, 2, null)
        val first = Track("track-1", "First", "Molyneux", "album-1", 1, 200000L, null)
        val other = Track("track-9", "Elsewhere", "Molyneux", "album-2", 1, 180000L, null)
        val playbackController = FakePlaybackController()
        val viewModel = viewModel(
            repository = FakeRepository(tracksResult = DistrictResult.Success(listOf(first))),
            playbackController = playbackController,
        )

        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()
        viewModel.enterLibrary()
        viewModel.openAlbum(album)
        viewModel.playTrack(other)

        assertEquals(listOf("track-9"), playbackController.lastQueue.map { it.id })
        assertEquals(0, playbackController.lastStartIndex)
    }

    @Test
    fun streamAuthErrorClearsSessionAndReturnsToSignIn() = runTest {
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val sessionStore = InMemorySessionStore(session)
        val playbackController = FakePlaybackController()
        val viewModel = viewModel(
            sessionStore = sessionStore,
            playbackController = playbackController,
        )

        playbackController.emit(PlayerState(isAuthError = true, errorMessage = "Session expired"))

        val state = viewModel.uiState.value as AppUiState.Onboarding
        assertEquals(OnboardingStep.SignIn, state.state.step)
        assertEquals(DistrictError.ExpiredToken, state.state.error)
        assertEquals(null, sessionStore.load())
    }

    @Test
    fun playbackStatePersistsQueueCurrentTrackAndPosition() = runTest {
        val playbackStore = InMemoryPlaybackStore()
        val playbackController = FakePlaybackController()
        viewModel(
            playbackStore = playbackStore,
            playbackController = playbackController,
            clockMs = { 1234L },
        )
        val track = Track("track-1", "Second Person", "Molyneux", "album-1", 2, 302000L, null)

        playbackController.emit(PlayerState(queue = listOf(track), currentTrack = track, positionMs = 4200L))

        assertEquals(
            PlaybackSnapshot(
                queueIds = listOf("track-1"),
                currentTrackId = "track-1",
                positionMs = 4200L,
                updatedAtEpochMs = 1234L,
            ),
            playbackStore.load(),
        )
    }

    @Test
    fun restoredPlaybackSnapshotResolvesTracksAndQueuesPausedAtSavedPosition() = runTest {
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val first = Track("track-1", "First", "Molyneux", "album-1", 1, 200000L, null)
        val second = Track("track-2", "Second", "Molyneux", "album-1", 2, 220000L, null)
        val playbackStore = InMemoryPlaybackStore(
            PlaybackSnapshot(
                queueIds = listOf("track-1", "track-2"),
                currentTrackId = "track-2",
                positionMs = 64000L,
                updatedAtEpochMs = 55L,
            ),
        )
        val repository = FakeRepository(tracksByIdsResult = DistrictResult.Success(listOf(second, first)))
        val playbackController = FakePlaybackController()

        viewModel(
            repository = repository,
            sessionStore = InMemorySessionStore(session),
            playbackStore = playbackStore,
            playbackController = playbackController,
        )

        assertEquals(listOf("track-1", "track-2"), playbackController.lastQueue.map { it.id })
        assertEquals(1, playbackController.lastStartIndex)
        assertEquals(64000L, playbackController.lastPositionMs)
        assertFalse(playbackController.lastPlayWhenReady)
    }

    @Test
    fun restoredPlaybackSnapshotClearsWhenCurrentTrackCannotBeResolved() = runTest {
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val playbackStore = InMemoryPlaybackStore(
            PlaybackSnapshot(
                queueIds = listOf("track-1", "track-2"),
                currentTrackId = "missing-track",
                positionMs = 64000L,
                updatedAtEpochMs = 55L,
            ),
        )
        val repository = FakeRepository(
            tracksByIdsResult = DistrictResult.Success(
                listOf(Track("track-1", "First", "Molyneux", "album-1", 1, 200000L, null)),
            ),
        )
        val playbackController = FakePlaybackController()

        viewModel(
            repository = repository,
            sessionStore = InMemorySessionStore(session),
            playbackStore = playbackStore,
            playbackController = playbackController,
        )

        assertEquals(null, playbackStore.load())
        assertEquals(emptyList<Track>(), playbackController.lastQueue)
    }

    @Test
    fun restoredPlaybackSnapshotClearsWhenShapeIsInvalid() = runTest {
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val playbackStore = InMemoryPlaybackStore(
            PlaybackSnapshot(
                queueIds = emptyList(),
                currentTrackId = null,
                positionMs = 0L,
                updatedAtEpochMs = 55L,
            ),
        )

        viewModel(
            sessionStore = InMemorySessionStore(session),
            playbackStore = playbackStore,
        )

        assertEquals(null, playbackStore.load())
    }

    @Test
    fun openArtistLoadsDiscographyAndNavigatesToArtistDetail() = runTest {
        val album = Album("album-1", "All Eyez On Me", "2pac", 1996, 16, null)
        val repository = FakeRepository(artistAlbumsResult = DistrictResult.Success(listOf(album)))
        val viewModel = viewModel(repository = repository)

        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()
        viewModel.enterLibrary()
        viewModel.openArtist(Artist("artist-1", "2pac"))

        val state = (viewModel.uiState.value as AppUiState.Library).state
        assertEquals(LibraryRoute.ArtistDetail, state.route)
        assertEquals("artist-1", repository.lastArtistId)
        assertEquals("2pac", state.selectedArtist?.name)
        assertEquals(listOf("album-1"), state.artistAlbums.map { it.id })
        assertFalse(state.isArtistLoading)
    }

    @Test
    fun blankArtistIdDoesNotNavigate() = runTest {
        val repository = FakeRepository()
        val viewModel = viewModel(repository = repository)
        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()
        viewModel.enterLibrary()

        viewModel.openArtist(Artist("", "Unknown"))

        val state = (viewModel.uiState.value as AppUiState.Library).state
        assertEquals(LibraryRoute.Albums, state.route)
        assertEquals(null, repository.lastArtistId)
    }

    @Test
    fun backStackReturnsThroughArtistDetailThenToAlbums() = runTest {
        val album = Album("album-1", "All Eyez On Me", "2pac", 1996, 16, null)
        val repository = FakeRepository(
            artistAlbumsResult = DistrictResult.Success(listOf(album)),
            tracksResult = DistrictResult.Success(listOf(Track("t1", "Ambitionz", "2pac", "album-1", 1, 200000L, null))),
        )
        val viewModel = viewModel(repository = repository)
        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()
        viewModel.enterLibrary()

        viewModel.openArtist(Artist("artist-1", "2pac"))
        viewModel.openAlbum(album)
        assertEquals(LibraryRoute.AlbumDetail, (viewModel.uiState.value as AppUiState.Library).state.route)

        viewModel.backToLibrary()
        val backToArtist = (viewModel.uiState.value as AppUiState.Library).state
        assertEquals(LibraryRoute.ArtistDetail, backToArtist.route)
        assertEquals(listOf("album-1"), backToArtist.artistAlbums.map { it.id })

        viewModel.backToLibrary()
        assertEquals(LibraryRoute.Albums, (viewModel.uiState.value as AppUiState.Library).state.route)
    }

    @Test
    fun downloadSelectedAlbumEnqueuesLoadedTracks() = runTest {
        val album = Album("album-1", "Ghost Notes", "Molyneux", 2024, 2, null)
        val first = Track("track-1", "First", "Molyneux", "album-1", 1, 200000L, null)
        val second = Track("track-2", "Second", "Molyneux", "album-1", 2, 220000L, null)
        val downloadManager = FakeDownloadManager()
        val viewModel = viewModel(
            repository = FakeRepository(tracksResult = DistrictResult.Success(listOf(first, second))),
            downloadManager = downloadManager,
        )
        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()
        viewModel.enterLibrary()
        viewModel.openAlbum(album)

        viewModel.downloadSelectedAlbum()

        assertEquals("album-1", downloadManager.enqueuedAlbumId)
        assertEquals(2, downloadManager.enqueuedTrackCount)
    }

    @Test
    fun networkFailureOnStartupEntersOfflineDownloadsMode() = runTest {
        val session = AuthSession("http://server", "token", "user", "demo", "device")
        val downloadManager = FakeDownloadManager(initialDownloads = listOf(downloadedAlbum()))
        val viewModel = viewModel(
            repository = FakeRepository(libraryResult = DistrictResult.Failure(DistrictError.Network("unreachable"))),
            sessionStore = InMemorySessionStore(session),
            downloadManager = downloadManager,
        )

        val state = (viewModel.uiState.value as AppUiState.Library).state
        assertTrue(state.isOffline)
        assertEquals(LibraryRoute.Downloads, state.route)
        assertEquals(listOf("album-1"), state.downloads.map { it.id })
    }

    @Test
    fun openDownloadsNavigatesToDownloadsRoute() = runTest {
        val viewModel = viewModel()
        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()
        viewModel.enterLibrary()

        viewModel.openDownloads()

        assertEquals(LibraryRoute.Downloads, (viewModel.uiState.value as AppUiState.Library).state.route)
    }

    @Test
    fun openDownloadedAlbumShowsLocalFileTracks() = runTest {
        val downloadManager = FakeDownloadManager(initialDownloads = listOf(downloadedAlbum()))
        val viewModel = viewModel(downloadManager = downloadManager)
        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()
        viewModel.enterLibrary()

        viewModel.openDownloadedAlbum(downloadedAlbum())

        val state = (viewModel.uiState.value as AppUiState.Library).state
        assertEquals(LibraryRoute.AlbumDetail, state.route)
        assertEquals(listOf("t1"), state.albumTracks.map { it.id })
        assertTrue(state.albumTracks.first().stream!!.url.startsWith("file://"))
    }

    @Test
    fun openAlbumUsesLocalTracksWhenDownloaded() = runTest {
        // Repository would return no tracks; the local (downloaded) tracks must win.
        val downloadManager = FakeDownloadManager(initialDownloads = listOf(downloadedAlbum()))
        val viewModel = viewModel(downloadManager = downloadManager)
        viewModel.connectServer()
        viewModel.checkServer()
        viewModel.signIn()
        viewModel.enterLibrary()

        viewModel.openAlbum(Album("album-1", "Ghost Notes", "Molyneux", 2024, 1, null))

        val state = (viewModel.uiState.value as AppUiState.Library).state
        assertEquals(listOf("t1"), state.albumTracks.map { it.id })
        assertFalse(state.isAlbumLoading)
    }

    @Test
    fun deleteDownloadDelegatesToManager() = runTest {
        val downloadManager = FakeDownloadManager(initialDownloads = listOf(downloadedAlbum()))
        val viewModel = viewModel(downloadManager = downloadManager)

        viewModel.deleteDownload("album-1")

        assertEquals("album-1", downloadManager.deletedAlbumId)
    }

    private fun downloadedAlbum() = DownloadedAlbum(
        id = "album-1",
        title = "Ghost Notes",
        artist = "Molyneux",
        productionYear = 2024,
        coverPath = null,
        tracks = listOf(
            DownloadedTrack("t1", "First", "Molyneux", "album-1", 1, 200000L, "/downloads/album-1/t1", 1_000L),
        ),
        downloadedAtEpochMs = 55L,
    )

    private fun viewModel(
        repository: FakeRepository = FakeRepository(),
        sessionStore: SessionStore = InMemorySessionStore(),
        playbackStore: InMemoryPlaybackStore = InMemoryPlaybackStore(),
        playbackController: FakePlaybackController = FakePlaybackController(),
        downloadManager: FakeDownloadManager = FakeDownloadManager(),
        dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
        clockMs: () -> Long = { 1L },
    ) = AppViewModel(
        repository = repository,
        sessionStore = sessionStore,
        playbackStore = playbackStore,
        playbackController = playbackController,
        downloadManager = downloadManager,
        dispatchers = TestDispatcherProvider(dispatcher),
        clockMs = clockMs,
    )

    private class FakeDownloadManager(
        initialDownloads: List<DownloadedAlbum> = emptyList(),
    ) : DownloadManager {
        private val _downloads = MutableStateFlow(initialDownloads)
        override val downloads: StateFlow<List<DownloadedAlbum>> = _downloads
        private val _active = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
        override val activeDownloads: StateFlow<Map<String, DownloadState>> = _active

        var enqueuedAlbumId: String? = null
        var enqueuedTrackCount: Int = 0
        var deletedAlbumId: String? = null

        override suspend fun refresh() {}

        override fun enqueue(album: Album, tracks: List<Track>) {
            enqueuedAlbumId = album.id
            enqueuedTrackCount = tracks.size
        }

        override suspend fun delete(albumId: String) {
            deletedAlbumId = albumId
            _downloads.value = _downloads.value.filterNot { it.id == albumId }
        }

        override fun isDownloaded(albumId: String): Boolean = _downloads.value.any { it.id == albumId }

        override fun playableTracks(albumId: String): List<Track>? {
            val album = _downloads.value.firstOrNull { it.id == albumId } ?: return null
            return album.tracks.map { t ->
                Track(t.id, t.title, t.artist, t.albumId, t.indexNumber, t.durationMs, RemoteResource("file://${t.filePath}", null))
            }
        }
    }

    private class FakeRepository(
        private val serverResult: DistrictResult<ServerInfo> = DistrictResult.Success(ServerInfo("http://192.168.1.50:8096", "LAN", "10.9.6")),
        private val authResult: DistrictResult<AuthSession> = DistrictResult.Success(AuthSession("http://server", "token", "user", "demo", "device")),
        private val libraryResult: DistrictResult<List<MusicLibrary>> = DistrictResult.Success(listOf(MusicLibrary("music", "Music", "music"))),
        private val albumsResult: DistrictResult<List<Album>> = DistrictResult.Success(emptyList()),
        private val tracksResult: DistrictResult<List<Track>> = DistrictResult.Success(emptyList()),
        private val tracksByIdsResult: DistrictResult<List<Track>> = DistrictResult.Success(emptyList()),
        private val artistAlbumsResult: DistrictResult<List<Album>> = DistrictResult.Success(emptyList()),
        var searchResult: DistrictResult<SearchResults> = DistrictResult.Success(SearchResults(emptyList(), emptyList(), emptyList())),
    ) : JellyfinRepository {
        var checkedUrl: String? = null
        var checkCount: Int = 0
        var searchCount: Int = 0
        var lastSearchQuery: String? = null
        var lastArtistId: String? = null

        override suspend fun checkServer(serverUrl: String): DistrictResult<ServerInfo> {
            checkCount += 1
            checkedUrl = serverUrl
            return serverResult
        }

        override suspend fun authenticate(serverUrl: String, username: String, password: String): DistrictResult<AuthSession> =
            authResult

        override suspend fun libraries(session: AuthSession): DistrictResult<List<MusicLibrary>> =
            libraryResult

        override suspend fun albums(session: AuthSession, parentId: String?): DistrictResult<List<Album>> =
            albumsResult

        override suspend fun artistAlbums(session: AuthSession, artistId: String): DistrictResult<List<Album>> {
            lastArtistId = artistId
            return artistAlbumsResult
        }

        override suspend fun albumTracks(session: AuthSession, albumId: String): DistrictResult<List<Track>> =
            tracksResult

        override suspend fun tracksByIds(session: AuthSession, ids: List<String>): DistrictResult<List<Track>> =
            tracksByIdsResult

        override suspend fun search(session: AuthSession, query: String): DistrictResult<SearchResults> {
            searchCount += 1
            lastSearchQuery = query
            return searchResult
        }
    }

    private class ThrowingSessionStore : SessionStore {
        override suspend fun load(): AuthSession? = null
        override suspend fun save(session: AuthSession) {
            error("secure storage unavailable")
        }
        override suspend fun clear() = Unit
    }

    private class FakePlaybackController : PlaybackController {
        private val mutableState = MutableStateFlow(PlayerState())
        override val state: StateFlow<PlayerState> = mutableState
        var lastQueue: List<Track> = emptyList()
        var lastStartIndex: Int = -1
        var lastPositionMs: Long = -1L
        var lastPlayWhenReady: Boolean = true

        fun emit(playerState: PlayerState) {
            mutableState.value = playerState
        }

        override fun playQueue(queue: List<Track>, startIndex: Int, positionMs: Long, playWhenReady: Boolean) {
            lastQueue = queue
            lastStartIndex = startIndex
            lastPositionMs = positionMs
            lastPlayWhenReady = playWhenReady
        }

        override fun playPause() {}
        override fun next() {}
        override fun previous() {}
        override fun seekToFraction(fraction: Float) {}
        override fun setVolumeFraction(fraction: Float) {}
        override fun release() {}
    }
}
