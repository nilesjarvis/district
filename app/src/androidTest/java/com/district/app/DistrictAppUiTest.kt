package com.district.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.district.core.media.PlayerState
import com.district.domain.Album
import com.district.domain.AuthSession
import com.district.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DistrictAppUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun onboardingServerStepShowsFieldsAndButtons() {
        compose.setContent {
            DistrictAppContent(
                AppUiState.Onboarding(
                    com.district.feature.onboarding.OnboardingUiState(
                        step = com.district.feature.onboarding.OnboardingStep.Server,
                    ),
                ),
            )
        }

        compose.onNodeWithText("SERVER ADDRESS").assertIsDisplayed()
        compose.onNodeWithText("PORT").assertIsDisplayed()
        compose.onNodeWithText("CONTINUE").assertIsDisplayed()
    }

    @Test
    fun libraryGridPlacesFirstTwoAlbumsInTwoColumnsAtPixel9Size() {
        compose.setContent {
            Box(Modifier.size(width = 411.dp, height = 923.dp)) {
                DistrictAppContent(
                    AppUiState.Library(
                        LibraryUiState(
                            session = session(),
                            albums = listOf(
                                Album("album-1", "Slow Structures", "A. Molyneux", 2024, 10, null),
                                Album("album-2", "Ghost Notes", "Molyneux", 2024, 9, null),
                            ),
                        ),
                    ),
                )
            }
        }

        val first = boundsForText("Slow Structures")
        val second = boundsForText("Ghost Notes")
        assertEquals(first.top, second.top, 2f)
        assertNotEquals(first.left, second.left)
    }

    @Test
    fun libraryScrollPositionSurvivesAlbumRoundTrip() {
        val albums = (0 until 40).map { index ->
            Album("album-$index", "Album $index", "District Artist", 2024, 10, null)
        }
        var route by mutableStateOf(LibraryRoute.Albums)
        var selectedAlbum by mutableStateOf<Album?>(null)
        compose.setContent {
            Box(Modifier.size(width = 411.dp, height = 923.dp)) {
                DistrictAppContent(
                    AppUiState.Library(
                        LibraryUiState(
                            session = session(),
                            route = route,
                            albums = albums,
                            selectedAlbum = selectedAlbum,
                        ),
                    ),
                    actions = AppActions(
                        openAlbum = { album ->
                            selectedAlbum = album
                            route = LibraryRoute.AlbumDetail
                        },
                        backToLibrary = {
                            selectedAlbum = null
                            route = LibraryRoute.Albums
                        },
                    ),
                )
            }
        }

        compose.onNodeWithTag("library-album-grid").performScrollToIndex(24)
        compose.onNodeWithText("Album 24").assertIsDisplayed()

        compose.onNodeWithTag("album-tile-album-24").performClick()
        compose.onNodeWithText("BACK").performClick()

        compose.onNodeWithText("Album 24").assertIsDisplayed()
    }

    @Test
    fun albumScrollPositionSurvivesStartingPlayback() {
        val album = Album("album-1", "Long Album", "District Artist", 2024, 40, null)
        val tracks = (0 until 40).map { index ->
            Track(
                id = "track-$index",
                title = "Track $index",
                artist = "District Artist",
                albumId = album.id,
                indexNumber = index + 1,
                durationMs = 180_000L,
                stream = null,
            )
        }
        var playerState by mutableStateOf(PlayerState())
        compose.setContent {
            Box(Modifier.size(width = 411.dp, height = 923.dp)) {
                DistrictAppContent(
                    AppUiState.Library(
                        LibraryUiState(
                            session = session(),
                            route = LibraryRoute.AlbumDetail,
                            selectedAlbum = album,
                            albumTracks = tracks,
                            playerState = playerState,
                        ),
                    ),
                    actions = AppActions(
                        playTrack = { track ->
                            playerState = PlayerState(
                                queue = tracks,
                                currentTrack = track,
                                isPlaying = true,
                                playWhenReady = true,
                                durationMs = track.durationMs,
                            )
                        },
                    ),
                )
            }
        }

        compose.onNodeWithTag("album-detail-grid").performScrollToIndex(24)
        compose.onNodeWithText("Track 24").assertIsDisplayed()

        compose.onNodeWithTag("album-track-track-24").performClick()

        compose.onNodeWithText("Track 24").assertIsDisplayed()
    }

    @Test
    fun searchKeepsNowPlayingVisibleAndRemovesControlZoneForKeyboardSpace() {
        var route by mutableStateOf(LibraryRoute.Search)
        compose.setContent {
            DistrictAppContent(
                AppUiState.Library(
                    LibraryUiState(
                        session = session(),
                        route = route,
                    ),
                ),
                actions = AppActions(backToLibrary = { route = LibraryRoute.Albums }),
            )
        }

        compose.onNodeWithText("BACK").assertIsDisplayed()
        compose.onNodeWithText("IDLE - NOW").assertIsDisplayed()
        compose.onNodeWithText("RECENT").assertIsDisplayed()
        compose.onNodeWithText("CONTROL / SCRUB").assertDoesNotExist()

        compose.onNodeWithText("BACK").performClick()

        compose.onNodeWithContentDescription("Search library").assertIsDisplayed()
    }

    @Test
    fun librarySearchIconOpensSearch() {
        var route by mutableStateOf(LibraryRoute.Albums)
        compose.setContent {
            DistrictAppContent(
                AppUiState.Library(
                    LibraryUiState(
                        session = session(),
                        route = route,
                    ),
                ),
                actions = AppActions(activateSearch = { route = LibraryRoute.Search }),
            )
        }

        compose.onNodeWithContentDescription("Search library").performClick()

        compose.onNodeWithText("BACK").assertIsDisplayed()
    }

    @Test
    fun idleLibraryKeepsNowPlayingVisibleAndCollapsesControlZone() {
        compose.setContent {
            DistrictAppContent(
                AppUiState.Library(
                    LibraryUiState(session = session()),
                ),
            )
        }

        compose.onNodeWithText("IDLE - NOW").assertIsDisplayed()
        compose.onNodeWithText("CONTROL / SCRUB").assertDoesNotExist()
    }

    @Test
    fun pausedTrackCollapsesControlZoneAndRightSideCanResume() {
        val track = track()
        var playPauseCalls = 0
        compose.setContent {
            DistrictAppContent(
                AppUiState.Library(
                    LibraryUiState(
                        session = session(),
                        playerState = PlayerState(
                            queue = listOf(track),
                            currentTrack = track,
                            isPlaying = false,
                            durationMs = track.durationMs,
                        ),
                    ),
                ),
                actions = AppActions(playPause = { playPauseCalls += 1 }),
            )
        }

        compose.onNodeWithText("PAUSE - NOW").assertIsDisplayed()
        compose.onNodeWithText("CONTROL / SCRUB").assertDoesNotExist()

        compose.onNodeWithTag("now-playing-toggle").performClick()

        assertEquals(1, playPauseCalls)
    }

    @Test
    fun bufferingDuringSeekKeepsControlZoneExpanded() {
        // Regression: scrubbing re-buffers, so ExoPlayer reports isPlaying=false while the user
        // has not paused (playWhenReady stays true). The scrub zone must remain open.
        val track = track()
        compose.setContent {
            DistrictAppContent(
                AppUiState.Library(
                    LibraryUiState(
                        session = session(),
                        playerState = PlayerState(
                            queue = listOf(track),
                            currentTrack = track,
                            isPlaying = false,
                            playWhenReady = true,
                            durationMs = track.durationMs,
                        ),
                    ),
                ),
            )
        }

        compose.onNodeWithText("CONTROL / SCRUB").assertDoesNotExist()
        compose.onNodeWithText("0:00").assertIsDisplayed()
        compose.onNodeWithText("3:00").assertIsDisplayed()
        compose.onNodeWithTag("playback-scrub-ruler").assertIsDisplayed()
    }

    @Test
    fun nowPlayingTitleOpensCurrentAlbum() {
        val album = Album("album-1", "Linked Album", "District", 2024, 8, null)
        val track = track()
        var openedAlbum: Album? = null
        var playPauseCalls = 0
        compose.setContent {
            DistrictAppContent(
                AppUiState.Library(
                    LibraryUiState(
                        session = session(),
                        albums = listOf(album),
                        playerState = PlayerState(
                            queue = listOf(track),
                            currentTrack = track,
                            isPlaying = true,
                            durationMs = track.durationMs,
                        ),
                    ),
                ),
                actions = AppActions(
                    openAlbum = { openedAlbum = it },
                    playPause = { playPauseCalls += 1 },
                ),
            )
        }

        compose.onNodeWithTag("now-playing-album-link").performClick()

        assertEquals(album, openedAlbum)
        assertEquals(0, playPauseCalls)
    }

    @Test
    fun scrubRulerSeeksWithoutTogglingPlayback() {
        val track = track()
        var seekCalls = 0
        var playPauseCalls = 0
        compose.setContent {
            DistrictAppContent(
                AppUiState.Library(
                    LibraryUiState(
                        session = session(),
                        playerState = PlayerState(
                            queue = listOf(track),
                            currentTrack = track,
                            isPlaying = true,
                            playWhenReady = true,
                            durationMs = track.durationMs,
                        ),
                    ),
                ),
                actions = AppActions(
                    seekToFraction = { seekCalls += 1 },
                    playPause = { playPauseCalls += 1 },
                ),
            )
        }

        compose.onNodeWithTag("playback-scrub-ruler").performTouchInput {
            swipeRight()
        }

        assertTrue("expected scrub gesture to seek", seekCalls > 0)
        assertEquals(0, playPauseCalls)
    }

    @Test
    fun searchSwipeRightGestureReturnsToLibrary() {
        var route by mutableStateOf(LibraryRoute.Search)
        compose.setContent {
            DistrictAppContent(
                AppUiState.Library(
                    LibraryUiState(
                        session = session(),
                        route = route,
                    ),
                ),
                actions = AppActions(backToLibrary = { route = LibraryRoute.Albums }),
            )
        }

        compose.onNodeWithTag("search-results-region").performTouchInput {
            swipeRight()
        }

        compose.onNodeWithContentDescription("Search library").assertIsDisplayed()
    }

    @Test
    fun bottomControlZonesMeetMinimumTouchTarget() {
        val track = track()
        compose.setContent {
            DistrictAppContent(
                AppUiState.Library(
                    LibraryUiState(
                        session = session(),
                        playerState = PlayerState(
                            queue = listOf(track),
                            currentTrack = track,
                            isPlaying = true,
                            playWhenReady = true,
                            durationMs = track.durationMs,
                        ),
                    ),
                ),
            )
        }

        listOf("control-prev-zone", "control-play-zone", "control-next-zone").forEach { tag ->
            val height = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.height
            assertTrue("$tag height was $height", height >= 44f)
        }
    }

    private fun boundsForText(text: String): Rect =
        compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot

    private fun session() =
        AuthSession(
            serverUrl = "http://preview",
            accessToken = "token",
            userId = "user",
            username = "demo",
            deviceId = "device",
        )

    private fun track() =
        Track(
            id = "track-1",
            title = "First Track",
            artist = "District",
            albumId = "album-1",
            indexNumber = 1,
            durationMs = 180_000L,
            stream = null,
        )
}
