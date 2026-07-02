package com.district.jellyfinmono.app

import com.district.jellyfinmono.domain.AuthSession
import com.district.jellyfinmono.domain.Album
import com.district.jellyfinmono.domain.DistrictError
import com.district.jellyfinmono.domain.MusicLibrary
import com.district.jellyfinmono.domain.SearchResults
import com.district.jellyfinmono.domain.Track
import com.district.jellyfinmono.core.media.PlayerState
import com.district.jellyfinmono.feature.onboarding.OnboardingUiState

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
    val previousRoute: LibraryRoute = LibraryRoute.Albums,
    val selectedAlbum: Album? = null,
    val albumTracks: List<Track> = emptyList(),
    val isAlbumLoading: Boolean = false,
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val searchResults: SearchResults = SearchResults(emptyList(), emptyList(), emptyList()),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val error: DistrictError? = null,
    val playerState: PlayerState = PlayerState(),
)

enum class LibraryRoute {
    Albums,
    Search,
    AlbumDetail,
}
