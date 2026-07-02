package com.district.jellyfinmono.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.district.jellyfinmono.domain.Album
import com.district.jellyfinmono.domain.AuthSession
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
                    com.district.jellyfinmono.feature.onboarding.OnboardingUiState(
                        step = com.district.jellyfinmono.feature.onboarding.OnboardingStep.Server,
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
    fun searchKeepsNowPlayingVisibleAndRemovesControlZoneForKeyboardSpace() {
        compose.setContent {
            DistrictAppContent(
                AppUiState.Library(
                    LibraryUiState(
                        session = session(),
                        route = LibraryRoute.Search,
                    ),
                ),
            )
        }

        compose.onNodeWithText("IDLE - NOW").assertIsDisplayed()
        compose.onNodeWithText("RECENT").assertIsDisplayed()
        compose.onNodeWithText("CONTROL / SCRUB").assertDoesNotExist()
    }

    @Test
    fun bottomControlZonesMeetMinimumTouchTarget() {
        compose.setContent {
            DistrictAppContent(
                AppUiState.Library(
                    LibraryUiState(session = session()),
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
            username = "marcus",
            deviceId = "device",
        )
}
