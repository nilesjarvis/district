package com.district.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.district.core.download.DownloadManager
import com.district.core.download.toAlbum
import com.district.core.media.PlaybackController
import com.district.core.media.PlayerState
import com.district.core.network.DefaultDispatcherProvider
import com.district.core.network.DispatcherProvider
import com.district.core.persistence.PlaybackStore
import com.district.core.persistence.SessionStore
import com.district.domain.Artist
import com.district.domain.AuthSession
import com.district.domain.Album
import com.district.domain.DistrictError
import com.district.domain.DownloadedAlbum
import com.district.domain.DistrictResult
import com.district.domain.JellyfinRepository
import com.district.domain.MusicLibrary
import com.district.domain.PlaybackSnapshot
import com.district.domain.SearchResults
import com.district.domain.Track
import com.district.feature.onboarding.OnboardingStep
import com.district.feature.onboarding.OnboardingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class AppViewModel(
    private val repository: JellyfinRepository,
    private val sessionStore: SessionStore,
    private val playbackStore: PlaybackStore,
    private val playbackController: PlaybackController,
    private val downloadManager: DownloadManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val clockMs: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AppUiState>(AppUiState.Onboarding())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.main) {
            playbackController.state.collectLatest { playerState ->
                updateLibrary { copy(playerState = playerState) }
                if (playerState.isAuthError) {
                    libraryState()?.let { handleExpiredSession(it.session) }
                } else {
                    persistPlayback(playerState)
                }
            }
        }
        viewModelScope.launch(dispatchers.main) {
            downloadManager.downloads.collect { list -> updateLibrary { copy(downloads = list) } }
        }
        viewModelScope.launch(dispatchers.main) {
            downloadManager.activeDownloads.collect { states -> updateLibrary { copy(downloadStates = states) } }
        }
        viewModelScope.launch(dispatchers.io) {
            downloadManager.refresh()
            sessionStore.load()?.let { session ->
                _uiState.value = AppUiState.Library(baseLibraryState(session, isLoading = true))
                if (loadLibrary(session)) {
                    restorePlayback(session)
                }
            }
        }
    }

    private fun baseLibraryState(session: AuthSession, isLoading: Boolean = false): LibraryUiState =
        LibraryUiState(
            session = session,
            isLoading = isLoading,
            downloads = downloadManager.downloads.value,
            downloadStates = downloadManager.activeDownloads.value,
        )

    fun connectServer() {
        setOnboarding { copy(step = OnboardingStep.Server, error = null) }
    }

    fun back() {
        setOnboarding {
            when (step) {
                OnboardingStep.Welcome -> this
                OnboardingStep.Server -> copy(step = OnboardingStep.Welcome, error = null)
                OnboardingStep.SignIn -> copy(step = OnboardingStep.Server, error = null)
                OnboardingStep.Connected -> copy(step = OnboardingStep.SignIn, error = null)
            }
        }
    }

    fun updateServerAddress(value: String) = setOnboarding { copy(serverAddress = value, error = null) }
    fun updatePort(value: String) = setOnboarding { copy(port = value.filter(Char::isDigit), error = null) }
    fun updateProtocol(value: String) = setOnboarding { copy(protocol = value, error = null) }
    fun updateUsername(value: String) = setOnboarding { copy(username = value, error = null) }
    fun updatePassword(value: String) = setOnboarding { copy(password = value, error = null) }
    fun toggleShowPassword() = setOnboarding { copy(showPassword = !showPassword) }
    fun toggleRememberDevice() = setOnboarding { copy(rememberDevice = !rememberDevice) }

    fun checkServer() {
        val state = onboardingState() ?: return
        if (state.isLoading) return
        setOnboarding { copy(isLoading = true, error = null) }
        viewModelScope.launch(dispatchers.io) {
            when (val result = repository.checkServer(state.serverUrl)) {
                is DistrictResult.Success -> setOnboarding {
                    copy(
                        step = OnboardingStep.SignIn,
                        serverInfo = result.value,
                        isLoading = false,
                        error = null,
                    )
                }
                is DistrictResult.Failure -> setOnboarding {
                    copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun signIn() {
        val state = onboardingState() ?: return
        if (state.isLoading) return
        setOnboarding { copy(isLoading = true, error = null) }
        viewModelScope.launch(dispatchers.io) {
            when (val auth = repository.authenticate(state.serverUrl, state.username, state.password)) {
                is DistrictResult.Failure -> setOnboarding {
                    copy(isLoading = false, error = auth.error)
                }
                is DistrictResult.Success -> handleAuthenticated(auth.value)
            }
        }
    }

    fun enterLibrary() {
        val state = onboardingState()
        val session = _lastSession ?: return
        val libraries = state?.libraries.orEmpty()
        _uiState.value = AppUiState.Library(
            baseLibraryState(session, isLoading = true).copy(
                libraries = libraries,
                selectedLibraryId = libraries.musicLibraryId(),
            ),
        )
        viewModelScope.launch(dispatchers.io) {
            loadAlbums(session, libraries)
        }
    }

    private var _lastSession: AuthSession? = null
    private var searchJob: Job? = null
    private var lastPersistedPlayback: PlaybackSnapshot? = null

    fun activateSearch() {
        updateLibrary {
            if (route == LibraryRoute.Search) this
            else copy(route = LibraryRoute.Search, backStack = backStack + route, error = null)
        }
        val state = libraryState() ?: return
        if (state.searchQuery.isBlank()) return
        scheduleSearch(state.searchQuery)
    }

    fun updateSearchQuery(query: String) {
        updateLibrary {
            copy(
                route = LibraryRoute.Search,
                searchQuery = query,
                isSearching = query.isNotBlank(),
                error = null,
            )
        }
        scheduleSearch(query)
    }

    fun openAlbum(album: Album) {
        val state = libraryState() ?: return
        val localTracks = downloadManager.playableTracks(album.id)
        if (localTracks != null) {
            updateLibrary {
                copy(
                    route = LibraryRoute.AlbumDetail,
                    backStack = backStack + route,
                    selectedAlbum = album,
                    albumTracks = localTracks,
                    isAlbumLoading = false,
                    error = null,
                )
            }
            return
        }
        updateLibrary {
            copy(
                route = LibraryRoute.AlbumDetail,
                backStack = backStack + route,
                selectedAlbum = album,
                albumTracks = emptyList(),
                isAlbumLoading = true,
                error = null,
            )
        }
        viewModelScope.launch(dispatchers.io) {
            when (val tracks = repository.albumTracks(state.session, album.id)) {
                is DistrictResult.Failure -> {
                    if (tracks.error == DistrictError.ExpiredToken) {
                        handleExpiredSession(state.session)
                    } else {
                        updateLibrary {
                            copy(isAlbumLoading = false, error = tracks.error)
                        }
                    }
                }
                is DistrictResult.Success -> updateLibrary {
                    copy(isAlbumLoading = false, albumTracks = tracks.value)
                }
            }
        }
    }

    fun openArtist(artist: Artist) {
        if (artist.id.isBlank()) return
        val state = libraryState() ?: return
        updateLibrary {
            copy(
                route = LibraryRoute.ArtistDetail,
                backStack = backStack + route,
                selectedArtist = artist,
                artistAlbums = emptyList(),
                isArtistLoading = true,
                error = null,
            )
        }
        viewModelScope.launch(dispatchers.io) {
            when (val albums = repository.artistAlbums(state.session, artist.id)) {
                is DistrictResult.Failure -> {
                    if (albums.error == DistrictError.ExpiredToken) {
                        handleExpiredSession(state.session)
                    } else {
                        updateLibrary {
                            copy(isArtistLoading = false, error = albums.error)
                        }
                    }
                }
                is DistrictResult.Success -> updateLibrary {
                    copy(isArtistLoading = false, artistAlbums = albums.value)
                }
            }
        }
    }

    fun backToLibrary() {
        searchJob?.cancel()
        updateLibrary {
            copy(
                route = backStack.lastOrNull() ?: LibraryRoute.Albums,
                backStack = backStack.dropLast(1),
                isAlbumLoading = false,
                isArtistLoading = false,
                isSearching = false,
                error = null,
            )
        }
    }

    fun playAlbumFromStart() {
        val state = libraryState() ?: return
        if (state.albumTracks.isNotEmpty()) playbackController.playQueue(state.albumTracks, 0)
    }

    fun playTrack(track: Track) {
        val state = libraryState() ?: return
        val queue = if (state.albumTracks.isNotEmpty()) state.albumTracks else state.searchResults.tracks
        val index = queue.indexOfFirst { it.id == track.id }
        if (index >= 0) {
            playbackController.playQueue(queue, index)
        } else {
            playbackController.playQueue(listOf(track), 0)
        }
    }

    fun playPause() = playbackController.playPause()
    fun nextTrack() = playbackController.next()
    fun previousTrack() = playbackController.previous()
    fun seekToFraction(fraction: Float) = playbackController.seekToFraction(fraction)
    fun setVolumeFraction(fraction: Float) = playbackController.setVolumeFraction(fraction)

    fun openDownloads() {
        updateLibrary {
            if (route == LibraryRoute.Downloads) this
            else copy(route = LibraryRoute.Downloads, backStack = backStack + route, error = null)
        }
    }

    fun openDownloadedAlbum(album: DownloadedAlbum) {
        val tracks = downloadManager.playableTracks(album.id) ?: return
        updateLibrary {
            copy(
                route = LibraryRoute.AlbumDetail,
                backStack = backStack + route,
                selectedAlbum = album.toAlbum(),
                albumTracks = tracks,
                isAlbumLoading = false,
                error = null,
            )
        }
    }

    fun downloadSelectedAlbum() {
        val state = libraryState() ?: return
        val album = state.selectedAlbum ?: return
        if (state.albumTracks.isEmpty()) return
        if (downloadManager.isDownloaded(album.id)) return
        downloadManager.enqueue(album, state.albumTracks)
    }

    fun deleteDownload(albumId: String) {
        viewModelScope.launch(dispatchers.io) { downloadManager.delete(albumId) }
    }

    private suspend fun handleAuthenticated(session: AuthSession) {
        when (val libraries = repository.libraries(session)) {
            is DistrictResult.Success -> {
                val saveError = runCatching { sessionStore.save(session) }.exceptionOrNull()
                if (saveError != null) {
                    _lastSession = null
                    setOnboarding {
                        copy(
                            step = OnboardingStep.SignIn,
                            isLoading = false,
                            error = DistrictError.Storage(saveError.message ?: "Unable to save session"),
                        )
                    }
                    return
                }
                _lastSession = session
                setOnboarding {
                    copy(
                        step = OnboardingStep.Connected,
                        libraries = libraries.value,
                        isLoading = false,
                        error = null,
                    )
                }
            }
            is DistrictResult.Failure -> setOnboarding {
                copy(isLoading = false, error = libraries.error)
            }
        }
    }

    private suspend fun loadLibrary(session: AuthSession): Boolean {
        return when (val libraries = repository.libraries(session)) {
            is DistrictResult.Failure -> when (val error = libraries.error) {
                DistrictError.ExpiredToken -> {
                    handleExpiredSession(session)
                    false
                }
                is DistrictError.Network -> {
                    enterOfflineMode(session)
                    false
                }
                else -> {
                    _uiState.value = AppUiState.Library(
                        baseLibraryState(session).copy(isLoading = false, error = error),
                    )
                    true
                }
            }
            is DistrictResult.Success -> {
                _uiState.value = AppUiState.Library(
                    baseLibraryState(session, isLoading = true).copy(
                        libraries = libraries.value,
                        selectedLibraryId = libraries.value.musicLibraryId(),
                    ),
                )
                loadAlbums(session, libraries.value)
                _uiState.value !is AppUiState.Onboarding
            }
        }
    }

    private suspend fun loadAlbums(session: AuthSession, libraries: List<MusicLibrary>) {
        val libraryId = libraries.musicLibraryId()
        when (val albums = repository.albums(session, libraryId)) {
            is DistrictResult.Failure -> when (val error = albums.error) {
                DistrictError.ExpiredToken -> handleExpiredSession(session)
                is DistrictError.Network -> enterOfflineMode(session)
                else -> _uiState.value = AppUiState.Library(
                    baseLibraryState(session).copy(
                        libraries = libraries,
                        selectedLibraryId = libraryId,
                        isLoading = false,
                        error = error,
                    ),
                )
            }
            is DistrictResult.Success -> _uiState.value = AppUiState.Library(
                baseLibraryState(session).copy(
                    libraries = libraries,
                    selectedLibraryId = libraryId,
                    albums = albums.value,
                    isLoading = false,
                ),
            )
        }
    }

    private fun enterOfflineMode(session: AuthSession) {
        _uiState.value = AppUiState.Library(
            baseLibraryState(session).copy(
                isLoading = false,
                isOffline = true,
                route = LibraryRoute.Downloads,
            ),
        )
    }

    private suspend fun restorePlayback(session: AuthSession) {
        val snapshot = playbackStore.load() ?: return
        if (snapshot.queueIds.isEmpty() || snapshot.currentTrackId.isNullOrBlank()) {
            clearPlaybackSnapshot()
            return
        }
        when (val tracks = repository.tracksByIds(session, snapshot.queueIds)) {
            is DistrictResult.Failure -> {
                if (tracks.error == DistrictError.ExpiredToken) handleExpiredSession(session)
            }
            is DistrictResult.Success -> {
                val byId = tracks.value.associateBy { it.id }
                val queue = snapshot.queueIds.mapNotNull { byId[it] }
                val start = queue.indexOfFirst { it.id == snapshot.currentTrackId }
                if (queue.isNotEmpty() && start >= 0) {
                    lastPersistedPlayback = snapshot
                    withContext(dispatchers.main) {
                        playbackController.playQueue(
                            queue = queue,
                            startIndex = start,
                            positionMs = snapshot.positionMs,
                            playWhenReady = false,
                        )
                    }
                } else {
                    clearPlaybackSnapshot()
                }
            }
        }
    }

    private suspend fun persistPlayback(playerState: PlayerState) {
        val current = playerState.currentTrack ?: return
        if (playerState.queue.isEmpty()) return
        val snapshot = PlaybackSnapshot(
            queueIds = playerState.queue.map { it.id },
            currentTrackId = current.id,
            positionMs = playerState.positionMs.coerceAtLeast(0L),
            updatedAtEpochMs = clockMs(),
        )
        if (!shouldPersist(snapshot, lastPersistedPlayback)) return
        try {
            withContext(dispatchers.io) {
                playbackStore.save(snapshot)
            }
            lastPersistedPlayback = snapshot
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
        }
    }

    private suspend fun clearPlaybackSnapshot() {
        runCatching {
            withContext(dispatchers.io) {
                playbackStore.clear()
            }
        }
    }

    private fun shouldPersist(next: PlaybackSnapshot, previous: PlaybackSnapshot?): Boolean {
        if (previous == null) return true
        if (next.queueIds != previous.queueIds) return true
        if (next.currentTrackId != previous.currentTrackId) return true
        return abs(next.positionMs - previous.positionMs) >= PLAYBACK_PERSIST_INTERVAL_MS
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            updateLibrary {
                copy(
                    isSearching = false,
                    searchResults = com.district.domain.SearchResults(emptyList(), emptyList(), emptyList()),
                )
            }
            return
        }
        val session = libraryState()?.session ?: return
        searchJob = viewModelScope.launch(dispatchers.io) {
            delay(150)
            when (val results = repository.search(session, query)) {
                is DistrictResult.Failure -> {
                    if (results.error == DistrictError.ExpiredToken) {
                        handleExpiredSession(session)
                    } else {
                        updateLibrary {
                            copy(
                                isSearching = false,
                                error = results.error,
                                searchResults = SearchResults(emptyList(), emptyList(), emptyList()),
                            )
                        }
                    }
                }
                is DistrictResult.Success -> updateLibrary {
                    copy(
                        isSearching = false,
                        searchResults = results.value,
                        recentSearches = listOf(query) + recentSearches.filterNot { it.equals(query, ignoreCase = true) }.take(4),
                        error = null,
                    )
                }
            }
        }
    }

    private suspend fun handleExpiredSession(session: AuthSession) {
        searchJob?.cancel()
        _lastSession = null
        runCatching { sessionStore.clear() }
        _uiState.value = AppUiState.Onboarding(
            OnboardingUiState(
                step = OnboardingStep.SignIn,
                serverAddress = session.serverUrl,
                port = "",
                protocol = if (session.serverUrl.startsWith("https://", ignoreCase = true)) "https" else "http",
                username = session.username,
                error = DistrictError.ExpiredToken,
            ),
        )
    }

    private fun onboardingState(): OnboardingUiState? =
        (_uiState.value as? AppUiState.Onboarding)?.state

    private fun libraryState(): LibraryUiState? =
        (_uiState.value as? AppUiState.Library)?.state

    private fun setOnboarding(reducer: OnboardingUiState.() -> OnboardingUiState) {
        _uiState.update { state ->
            val onboarding = state as? AppUiState.Onboarding ?: return@update state
            AppUiState.Onboarding(onboarding.state.reducer())
        }
    }

    private fun updateLibrary(reducer: LibraryUiState.() -> LibraryUiState) {
        _uiState.update { state ->
            val library = state as? AppUiState.Library ?: return@update state
            AppUiState.Library(library.state.reducer())
        }
    }
}

private fun List<MusicLibrary>.musicLibraryId(): String? =
    firstOrNull { it.collectionType.equals("music", ignoreCase = true) }?.id ?: firstOrNull()?.id

private const val PLAYBACK_PERSIST_INTERVAL_MS = 2_500L
