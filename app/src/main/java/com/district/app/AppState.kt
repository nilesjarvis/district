package com.district.app

import com.district.domain.Artist
import com.district.domain.AuthSession
import com.district.domain.Album
import com.district.domain.DistrictError
import com.district.domain.DownloadState
import com.district.domain.DownloadedAlbum
import com.district.domain.MusicLibrary
import com.district.domain.SearchResults
import com.district.domain.Track
import com.district.core.media.PlayerState
import com.district.feature.onboarding.OnboardingUiState

sealed interface AppUiState {
    data class Onboarding(val state: OnboardingUiState = OnboardingUiState()) : AppUiState
    data class Library(val state: LibraryUiState) : AppUiState
}

data class LibraryUiState(
    val session: AuthSession,
    val libraries: List<MusicLibrary> = emptyList(),
    val selectedLibraryId: String? = null,
    val albums: List<Album> = emptyList(),
    val route: LibraryRoute = LibraryRoute.Albums,
    val backStack: List<LibraryRoute> = emptyList(),
    val selectedAlbum: Album? = null,
    val albumTracks: List<Track> = emptyList(),
    val isAlbumLoading: Boolean = false,
    val selectedArtist: Artist? = null,
    val artistAlbums: List<Album> = emptyList(),
    val isArtistLoading: Boolean = false,
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val searchResults: SearchResults = SearchResults(emptyList(), emptyList(), emptyList()),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val error: DistrictError? = null,
    val playerState: PlayerState = PlayerState(),
    val downloads: List<DownloadedAlbum> = emptyList(),
    val downloadStates: Map<String, DownloadState> = emptyMap(),
    val isOffline: Boolean = false,
)

enum class LibraryRoute {
    Albums,
    Search,
    AlbumDetail,
    ArtistDetail,
    Downloads,
}
