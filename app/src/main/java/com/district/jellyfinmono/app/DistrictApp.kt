package com.district.jellyfinmono.app

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.compose.BackHandler
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.district.jellyfinmono.core.design.DistrictTheme
import com.district.jellyfinmono.core.design.JetBrainsMono
import com.district.jellyfinmono.core.design.MonoAlbumTile
import com.district.jellyfinmono.core.design.MonoButton
import com.district.jellyfinmono.core.design.MonoLedgerRow
import com.district.jellyfinmono.core.design.MonoNowPlayingBar
import com.district.jellyfinmono.core.design.MonoPrimaryButton
import com.district.jellyfinmono.core.design.MonoScrubPreview
import com.district.jellyfinmono.core.design.MonoShell
import com.district.jellyfinmono.core.design.MonoStatusDot
import com.district.jellyfinmono.core.design.MonoTokens
import com.district.jellyfinmono.core.design.MonoTrackRow
import com.district.jellyfinmono.core.design.MonoVolumeBar
import com.district.jellyfinmono.core.design.ShellMetrics
import com.district.jellyfinmono.core.design.UpperLabel
import com.district.jellyfinmono.domain.Album
import com.district.jellyfinmono.domain.DistrictError
import com.district.jellyfinmono.domain.SearchResults
import com.district.jellyfinmono.domain.Track
import com.district.jellyfinmono.core.media.DetentHapticGate
import com.district.jellyfinmono.core.media.PlayerState
import com.district.jellyfinmono.core.media.horizontalFraction
import com.district.jellyfinmono.feature.onboarding.OnboardingStep
import com.district.jellyfinmono.feature.onboarding.OnboardingUiState

@Composable
fun DistrictApp(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsState()
    DistrictAppContent(
        state = state,
        actions = AppActions(
            connectServer = viewModel::connectServer,
            checkServer = viewModel::checkServer,
            signIn = viewModel::signIn,
            enterLibrary = viewModel::enterLibrary,
            updateServerAddress = viewModel::updateServerAddress,
            updatePort = viewModel::updatePort,
            updateProtocol = viewModel::updateProtocol,
            updateUsername = viewModel::updateUsername,
            updatePassword = viewModel::updatePassword,
            toggleShowPassword = viewModel::toggleShowPassword,
            toggleRememberDevice = viewModel::toggleRememberDevice,
            back = viewModel::back,
            activateSearch = viewModel::activateSearch,
            updateSearchQuery = viewModel::updateSearchQuery,
            openAlbum = viewModel::openAlbum,
            backToLibrary = viewModel::backToLibrary,
            playAlbumFromStart = viewModel::playAlbumFromStart,
            playTrack = viewModel::playTrack,
            playPause = viewModel::playPause,
            nextTrack = viewModel::nextTrack,
            previousTrack = viewModel::previousTrack,
            seekToFraction = viewModel::seekToFraction,
            setVolumeFraction = viewModel::setVolumeFraction,
        ),
    )
}

@Composable
fun DistrictAppContent(
    state: AppUiState,
    actions: AppActions = AppActions(),
) {
    DistrictTheme {
        when (state) {
            is AppUiState.Onboarding -> OnboardingScreen(state.state, actions)
            is AppUiState.Library -> LibraryScreen(state.state, actions)
        }
    }
}

data class AppActions(
    val connectServer: () -> Unit = {},
    val checkServer: () -> Unit = {},
    val signIn: () -> Unit = {},
    val enterLibrary: () -> Unit = {},
    val updateServerAddress: (String) -> Unit = {},
    val updatePort: (String) -> Unit = {},
    val updateProtocol: (String) -> Unit = {},
    val updateUsername: (String) -> Unit = {},
    val updatePassword: (String) -> Unit = {},
    val toggleShowPassword: () -> Unit = {},
    val toggleRememberDevice: () -> Unit = {},
    val back: () -> Unit = {},
    val activateSearch: () -> Unit = {},
    val updateSearchQuery: (String) -> Unit = {},
    val openAlbum: (Album) -> Unit = {},
    val backToLibrary: () -> Unit = {},
    val playAlbumFromStart: () -> Unit = {},
    val playTrack: (Track) -> Unit = {},
    val playPause: () -> Unit = {},
    val nextTrack: () -> Unit = {},
    val previousTrack: () -> Unit = {},
    val seekToFraction: (Float) -> Unit = {},
    val setVolumeFraction: (Float) -> Unit = {},
)

@Composable
private fun OnboardingScreen(state: OnboardingUiState, actions: AppActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoTokens.Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        HeaderLine(left = state.step.headerLabel(), right = state.step.progressLabel())
        when (state.step) {
            OnboardingStep.Welcome -> WelcomeStep(state, actions)
            OnboardingStep.Server -> ServerStep(state, actions)
            OnboardingStep.SignIn -> SignInStep(state, actions)
            OnboardingStep.Connected -> ConnectedStep(state, actions)
        }
    }
}

@Composable
private fun HeaderLine(left: String, right: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .border(1.dp, MonoTokens.Line)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UpperLabel(left, color = MonoTokens.Ink)
        Spacer(Modifier.weight(1f))
        UpperLabel(right, color = if (right == "DONE") MonoTokens.Ok else MonoTokens.Mut)
    }
}

@Composable
private fun WelcomeStep(state: OnboardingUiState, actions: AppActions) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(74.dp)
                    .height(74.dp)
                    .background(MonoTokens.Panel)
                    .border(1.dp, MonoTokens.Line2),
                contentAlignment = Alignment.Center,
            ) {
                Text("JF", color = MonoTokens.Ink, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text("JELLYFIN", color = MonoTokens.Ink, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            UpperLabel("MONO CLIENT", color = MonoTokens.Mut)
            Spacer(Modifier.height(28.dp))
            Text(
                text = "A quiet, tile-based player for your own Jellyfin library.",
                color = MonoTokens.Mut,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        ErrorLine(state.error)
        MonoPrimaryButton(
            text = "CONNECT SERVER",
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            onClick = actions.connectServer,
        )
        Spacer(Modifier.height(56.dp))
    }
}

@Composable
private fun ServerStep(state: OnboardingUiState, actions: AppActions) {
    StepBody(
        error = state.error,
        footer = {
            StepButtons("BACK", actions.back, if (state.isLoading) "CHECKING" else "CONTINUE", actions.checkServer, enabled = !state.isLoading)
        },
    ) {
        Text("Where is your server?", color = MonoTokens.Ink, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Enter your Jellyfin server. The LAN HTTP target is allowed; use HTTPS for domains.", color = MonoTokens.Mut, fontFamily = JetBrainsMono, fontSize = 11.sp)
        Spacer(Modifier.height(22.dp))
        MonoInput("SERVER ADDRESS", state.serverAddress, actions.updateServerAddress)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MonoInput("PORT", state.port, actions.updatePort, Modifier.weight(1f), KeyboardType.Number)
            ProtocolSwitch(state.protocol, actions.updateProtocol, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        val info = state.serverInfo
        if (info != null) {
            MonoStatusDot("REACHABLE - JELLYFIN ${info.version} DETECTED", color = MonoTokens.Ok)
        } else {
            UpperLabel("SERVER CHECK WILL VERIFY /SYSTEM/INFO/PUBLIC", color = MonoTokens.Mut2)
        }
    }
}

@Composable
private fun SignInStep(state: OnboardingUiState, actions: AppActions) {
    StepBody(
        error = state.error,
        footer = {
            StepButtons("BACK", actions.back, if (state.isLoading) "SIGNING IN" else "SIGN IN", actions.signIn, enabled = !state.isLoading)
        },
    ) {
        ServerChip(state.serverInfo?.serverUrl ?: state.serverUrl, onChange = actions.back)
        Spacer(Modifier.height(16.dp))
        Text("Sign in", color = MonoTokens.Ink, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Use your Jellyfin account for this server.", color = MonoTokens.Mut, fontFamily = JetBrainsMono, fontSize = 11.sp)
        Spacer(Modifier.height(18.dp))
        MonoInput("USERNAME", state.username, actions.updateUsername)
        MonoInput(
            label = "PASSWORD",
            value = state.password,
            onValueChange = actions.updatePassword,
            visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        )
        Row(
            modifier = Modifier
                .height(44.dp)
                .fillMaxWidth()
                .clickable(onClick = actions.toggleRememberDevice),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(14.dp)
                    .background(if (state.rememberDevice) MonoTokens.Accent else MonoTokens.Panel)
                    .border(1.dp, MonoTokens.Line2),
            )
            Spacer(Modifier.width(8.dp))
            UpperLabel("REMEMBER THIS DEVICE", color = MonoTokens.Mut)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clickable(onClick = actions.toggleShowPassword)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                UpperLabel(if (state.showPassword) "HIDE" else "SHOW", color = MonoTokens.Mut)
            }
        }
    }
}

@Composable
private fun ConnectedStep(state: OnboardingUiState, actions: AppActions) {
    StepBody(
        error = state.error,
        footer = {
            MonoPrimaryButton(
                text = "ENTER LIBRARY",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                onClick = actions.enterLibrary,
            )
        },
    ) {
        Box(
            modifier = Modifier
                .width(74.dp)
                .height(74.dp)
                .background(MonoTokens.Panel)
                .border(1.dp, MonoTokens.Ok),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", color = MonoTokens.Ok, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text("CONNECTED", color = MonoTokens.Ink, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        UpperLabel("SIGNED IN AS ${state.username}", color = MonoTokens.Mut)
        Spacer(Modifier.height(20.dp))
        MonoLedgerRow("SERVER", state.serverInfo?.serverUrl ?: state.serverUrl)
        MonoLedgerRow("VERSION", state.serverInfo?.version ?: "-")
        MonoLedgerRow("LIBRARIES", state.libraries.joinToString { it.name }.ifBlank { "Music" })
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp),
        )
    }
}

@Composable
private fun StepBody(
    error: DistrictError?,
    footer: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            content()
        }
        ErrorLine(error)
        footer()
    }
}

@Composable
private fun ErrorLine(error: DistrictError?) {
    val text = error?.label().orEmpty()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (text.isNotBlank()) UpperLabel(text, color = MonoTokens.Accent)
    }
}

private fun DistrictError.label(): String =
    when (this) {
        DistrictError.AuthRejected -> "SIGN IN FAILED"
        DistrictError.ExpiredToken -> "SESSION EXPIRED"
        is DistrictError.InvalidServerUrl -> "INVALID SERVER URL"
        is DistrictError.Network -> "SERVER UNREACHABLE"
        is DistrictError.Http -> "HTTP $code"
        is DistrictError.Parse -> "UNEXPECTED SERVER RESPONSE"
        DistrictError.Empty -> "NO RESULTS"
        is DistrictError.Playback -> "PLAYBACK ERROR"
        is DistrictError.Storage -> "STORAGE ERROR"
    }

@Composable
private fun MonoInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(modifier) {
        UpperLabel(label, color = MonoTokens.Mut2)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(
                color = MonoTokens.Ink,
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(MonoTokens.Panel)
                .border(1.dp, MonoTokens.Accent)
                .padding(horizontal = 10.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun ProtocolSwitch(protocol: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        UpperLabel("PROTOCOL", color = MonoTokens.Mut2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(MonoTokens.Panel)
                .border(1.dp, MonoTokens.Line),
        ) {
            MonoButton("HTTP", modifier = Modifier.weight(1f), active = protocol == "http", onClick = { onChange("http") })
            MonoButton("HTTPS", modifier = Modifier.weight(1f), active = protocol == "https", onClick = { onChange("https") })
        }
    }
}

@Composable
private fun ServerChip(server: String, onChange: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(MonoTokens.Panel)
            .border(1.dp, MonoTokens.Line)
            .clickable(onClick = onChange)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoStatusDot(server, color = MonoTokens.Ok)
        Spacer(Modifier.weight(1f))
        UpperLabel("CHANGE", color = MonoTokens.Mut2)
    }
}

@Composable
private fun StepButtons(left: String, onLeft: () -> Unit, right: String, onRight: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MonoButton(left, modifier = Modifier.weight(0.7f), onClick = onLeft)
        MonoPrimaryButton(right, modifier = Modifier.weight(1.7f), enabled = enabled, onClick = onRight)
    }
}

@Composable
private fun LibraryScreen(state: LibraryUiState, actions: AppActions = AppActions()) {
    BackHandler(enabled = state.route != LibraryRoute.Albums, onBack = actions.backToLibrary)
    val targetControlZoneHeight = when {
        state.route == LibraryRoute.Search -> 0.dp
        state.playerState.currentTrack == null -> 0.dp
        else -> ShellMetrics.ControlZoneHeight
    }
    val controlZoneHeight by animateDpAsState(
        targetValue = targetControlZoneHeight,
        animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing),
        label = "controlZoneHeight",
    )
    MonoShell(
        header = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UpperLabel(if (state.route == LibraryRoute.AlbumDetail) "< ALBUM" else "JELLYFIN|LIB", color = MonoTokens.Ink)
                Spacer(Modifier.weight(1f))
                UpperLabel(if (state.error == null) "SRV:OK" else "SRV:ERR", color = if (state.error == null) MonoTokens.Ok else MonoTokens.Accent)
            }
        },
        contextualBar = {
            LibraryContextBar(state, actions)
        },
        scrollRegion = {
            when (state.route) {
                LibraryRoute.Albums -> LibraryAlbumGrid(state, actions)
                LibraryRoute.Search -> SearchResultsRegion(state, actions)
                LibraryRoute.AlbumDetail -> AlbumDetailRegion(state, actions)
            }
        },
        nowPlaying = { NowPlayingFromState(state.playerState) },
        controlZone = { PlayerControlZone(state.playerState, actions) },
        controlZoneHeight = controlZoneHeight,
    )
}

@Composable
private fun LibraryContextBar(state: LibraryUiState, actions: AppActions) {
    when (state.route) {
        LibraryRoute.Search -> Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MonoTokens.Panel)
                .border(1.dp, MonoTokens.Accent)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(62.dp)
                    .clickable(onClick = actions.backToLibrary),
                contentAlignment = Alignment.CenterStart,
            ) {
                UpperLabel("BACK", color = MonoTokens.Ink)
            }
            BasicTextField(
                value = state.searchQuery,
                onValueChange = actions.updateSearchQuery,
                singleLine = true,
                textStyle = TextStyle(color = MonoTokens.Ink, fontFamily = JetBrainsMono, fontSize = 13.sp),
                modifier = Modifier
                    .weight(1f)
                    .searchAutofocus()
                    .padding(vertical = 16.dp),
            )
            UpperLabel("SEARCH", color = MonoTokens.Mut2)
        }
        LibraryRoute.AlbumDetail -> Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = actions.backToLibrary)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UpperLabel("RECENT - ALBUM", color = MonoTokens.Mut)
            Spacer(Modifier.weight(1f))
            UpperLabel("BACK", color = MonoTokens.Mut2)
        }
        LibraryRoute.Albums -> Row(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MonoTokens.Accent)
                .clickable(onClick = actions.activateSearch)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UpperLabel("SEARCH LIBRARY", color = MonoTokens.Mut)
            Spacer(Modifier.weight(1f))
            UpperLabel("TAP", color = MonoTokens.Mut2)
        }
    }
}

@Composable
private fun LibraryAlbumGrid(state: LibraryUiState, actions: AppActions) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = rememberLazyGridState(),
        modifier = Modifier
            .fillMaxSize()
            .background(MonoTokens.Line)
            .border(1.dp, MonoTokens.Line),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        if (state.isLoading) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(MonoTokens.Panel),
                    contentAlignment = Alignment.Center,
                ) {
                    UpperLabel("LOADING LIBRARY", color = MonoTokens.Mut)
                }
            }
        }
        if (state.error != null) {
            item(span = { GridItemSpan(2) }) {
                StateTile(state.error.label(), color = MonoTokens.Accent)
            }
        }
        if (!state.isLoading && state.error == null && state.albums.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(MonoTokens.Panel),
                    contentAlignment = Alignment.Center,
                ) {
                    UpperLabel("NO ALBUMS", color = MonoTokens.Mut)
                }
            }
        }
        itemsIndexed(state.albums) { index, album ->
            AlbumTileWithArt(
                album = album,
                fallbackColor = previewColor(index),
                onClick = { actions.openAlbum(album) },
            )
        }
    }
}

@Composable
private fun SearchResultsRegion(state: LibraryUiState, actions: AppActions) {
    val results = state.searchResults
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .testTag("search-results-region")
            .pointerInput(actions.backToLibrary) {
                var totalDrag = 0f
                var triggered = false
                val threshold = 72.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        triggered = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (triggered) return@detectHorizontalDragGestures
                        totalDrag += dragAmount
                        if (totalDrag > threshold) {
                            triggered = true
                            change.consume()
                            actions.backToLibrary()
                        }
                    },
                )
            }
            .background(MonoTokens.Line)
            .border(1.dp, MonoTokens.Line),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            SearchTabs(results, state.searchQuery, state.isSearching)
        }
        if (state.error != null) {
            item(span = { GridItemSpan(2) }) {
                StateTile(state.error.label(), color = MonoTokens.Accent)
            }
        }
        if (state.searchQuery.isBlank()) {
            item(span = { GridItemSpan(2) }) {
                RecentSearches(state.recentSearches, actions.updateSearchQuery)
            }
        } else {
            if (state.isSearching) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .background(MonoTokens.Panel),
                        contentAlignment = Alignment.Center,
                    ) {
                        UpperLabel("SEARCHING", color = MonoTokens.Accent)
                    }
                }
            }
            if (!state.isSearching && state.error == null && results.totalCount == 0) {
                item(span = { GridItemSpan(2) }) {
                    StateTile("NO RESULTS", color = MonoTokens.Mut)
                }
            }
            itemsIndexed(results.albums) { index, album ->
                AlbumTileWithArt(
                    album = album,
                    fallbackColor = previewColor(index),
                    onClick = { actions.openAlbum(album) },
                )
            }
            if (results.artists.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    SectionLabel("ARTISTS - ${results.artists.size}")
                }
                items(results.artists, span = { GridItemSpan(2) }) { artist ->
                    MonoTrackRow(number = "AR", title = artist.name, duration = "")
                }
            }
            if (results.tracks.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    SectionLabel("TRACKS - ${results.tracks.size}")
                }
                items(results.tracks, span = { GridItemSpan(2) }) { track ->
                    MonoTrackRow(
                        number = track.indexNumber?.toString()?.padStart(2, '0') ?: "--",
                        title = track.title,
                        duration = track.durationMs.formatDuration(),
                        isPlaying = state.playerState.currentTrack?.id == track.id,
                        onClick = { actions.playTrack(track) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTabs(results: SearchResults, query: String, loading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(MonoTokens.Bg)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UpperLabel("ALL ${results.totalCount}", color = MonoTokens.Ink)
        UpperLabel("ALBUMS ${results.albums.size}", color = MonoTokens.Mut)
        UpperLabel("TRACKS ${results.tracks.size}", color = MonoTokens.Mut)
        Spacer(Modifier.weight(1f))
        UpperLabel(if (loading) "LIVE" else if (query.isBlank()) "RECENT" else "DONE", color = if (loading) MonoTokens.Accent else MonoTokens.Mut2)
    }
}

@Composable
private fun AlbumDetailRegion(state: LibraryUiState, actions: AppActions) {
    val album = state.selectedAlbum
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MonoTokens.Line)
            .border(1.dp, MonoTokens.Line),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        if (album != null) {
            item {
                CoverArt(
                    album = album,
                    fallbackColor = MonoTokens.CoverBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MonoTokens.Panel)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    UpperLabel(album.productionYear?.toString() ?: "ALBUM", color = MonoTokens.Mut2)
                    Text(
                        text = album.title,
                        color = MonoTokens.Ink,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = album.artist.ifBlank { "Unknown artist" },
                        color = MonoTokens.Mut,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    UpperLabel(
                        "${album.trackCount ?: state.albumTracks.size} TRACKS - ${state.albumTracks.sumOf { it.durationMs }.formatDuration()}",
                        color = MonoTokens.Mut2,
                    )
                }
            }
            item(span = { GridItemSpan(2) }) {
                MonoPrimaryButton("PLAY ALL", modifier = Modifier
                    .height(44.dp)
                    .fillMaxWidth(), onClick = actions.playAlbumFromStart)
            }
        }
        if (state.error != null) {
            item(span = { GridItemSpan(2) }) {
                StateTile(state.error.label(), color = MonoTokens.Accent, height = 72.dp)
            }
        }
        if (state.isAlbumLoading) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(MonoTokens.Panel),
                    contentAlignment = Alignment.Center,
                ) {
                    UpperLabel("LOADING TRACKS", color = MonoTokens.Mut)
                }
            }
        } else {
            if (album != null && state.error == null && state.albumTracks.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    StateTile("NO TRACKS", color = MonoTokens.Mut, height = 72.dp)
                }
            }
            items(state.albumTracks, span = { GridItemSpan(2) }) { track ->
                MonoTrackRow(
                    number = track.indexNumber?.toString()?.padStart(2, '0') ?: "--",
                    title = track.title,
                    duration = track.durationMs.formatDuration(),
                    isPlaying = state.playerState.currentTrack?.id == track.id,
                    onClick = { actions.playTrack(track) },
                )
            }
        }
    }
}

@Composable
private fun StateTile(text: String, color: Color, height: androidx.compose.ui.unit.Dp = 96.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(MonoTokens.Panel),
        contentAlignment = Alignment.Center,
    ) {
        UpperLabel(text, color = color)
    }
}

private fun previewColor(index: Int): Color =
    listOf(MonoTokens.CoverBrown, MonoTokens.CoverBlue, MonoTokens.CoverGreen, MonoTokens.CoverViolet)[index % 4]

@Composable
private fun RecentSearches(recentSearches: List<String>, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MonoTokens.Panel)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UpperLabel("RECENT", color = MonoTokens.Mut)
        if (recentSearches.isEmpty()) {
            UpperLabel("TYPE TO SEARCH ALBUMS, ARTISTS & TRACKS", color = MonoTokens.Mut2)
        } else {
            recentSearches.forEach { query ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable { onSelect(query) },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(query, color = MonoTokens.Ink, fontFamily = JetBrainsMono, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(MonoTokens.Bg)
            .padding(top = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        UpperLabel(text, color = MonoTokens.Mut)
    }
}

@Composable
private fun AlbumTileWithArt(album: Album, fallbackColor: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .background(MonoTokens.Panel)
            .clickable(onClick = onClick),
    ) {
        CoverArt(
            album = album,
            fallbackColor = fallbackColor,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            UpperLabel(album.productionYear?.toString() ?: album.id.take(6), color = MonoTokens.Mut2, fontSize = 8.sp)
            Text(
                text = album.title,
                color = MonoTokens.Ink,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = album.artist.ifBlank { "Unknown artist" },
                color = MonoTokens.Mut,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CoverArt(album: Album, fallbackColor: Color, modifier: Modifier) {
    val resource = album.coverArt
    if (resource == null) {
        com.district.jellyfinmono.core.design.CoverPlaceholder(fallbackColor, modifier)
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(resource.url)
                .apply {
                    resource.authHeaders?.asMap()?.forEach { (name, value) -> addHeader(name, value) }
                }
                .build(),
            contentDescription = album.title,
            contentScale = ContentScale.Crop,
            modifier = modifier.background(fallbackColor),
        )
    }
}

private fun Long.formatDuration(): String {
    if (this <= 0L) return "--:--"
    val totalSeconds = this / 1000L
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

@Composable
private fun Modifier.searchAutofocus(): Modifier {
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    return focusRequester(focusRequester)
}

@Composable
private fun NowPlayingFromState(playerState: PlayerState) {
    val track = playerState.currentTrack
    val error = playerState.errorMessage?.takeIf { it.isNotBlank() }
    val context = LocalContext.current
    val coverResource = track?.coverArt
    val fallbackTint = track?.tintArgb?.let { Color(it) } ?: MonoTokens.CoverBlue
    var sampledTint by remember(track?.id, fallbackTint) { mutableStateOf(fallbackTint) }
    LaunchedEffect(coverResource?.url, fallbackTint) {
        sampledTint = fallbackTint
        val resource = coverResource ?: return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(resource.url)
            .allowHardware(false)
            .apply {
                resource.authHeaders?.asMap()?.forEach { (name, value) -> addHeader(name, value) }
            }
            .build()
        val result = runCatching { context.imageLoader.execute(request) }.getOrNull() as? SuccessResult
        val bitmap = (result?.drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
        val dominant = Palette.from(bitmap).generate().getDominantColor(0)
        if (dominant != 0) {
            sampledTint = Color(dominant.toLong() and 0xFFFFFFFFL)
        }
    }
    MonoNowPlayingBar(
        title = if (error != null && track == null) "Playback error" else track?.title ?: "Ready",
        artist = error ?: track?.artist?.ifBlank { "Jellyfin Mono" } ?: "Jellyfin Mono",
        code = when {
            error != null -> "ERR"
            playerState.isPlaying -> "PLAY"
            track != null -> "PAUSE"
            else -> "IDLE"
        },
        elapsed = playerState.positionMs.formatDuration(),
        duration = playerState.durationMs.formatDuration(),
        coverColor = sampledTint,
        tintColor = sampledTint,
    )
}

@Composable
private fun PlayerControlZone(playerState: PlayerState, actions: AppActions) {
    val haptics = LocalHapticFeedback.current
    val progress = if (playerState.durationMs > 0) playerState.positionMs.toFloat() / playerState.durationMs.toFloat() else 0f
    val scrubGate = remember(playerState.currentTrack?.id) { DetentHapticGate(detents = 100, initialFraction = progress) }
    val volumeGate = remember { DetentHapticGate(detents = 20, initialFraction = playerState.volume) }
    LaunchedEffect(playerState.currentTrack?.id) {
        scrubGate.reset(progress)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoTokens.Panel)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UpperLabel(
                if (playerState.errorMessage.isNullOrBlank()) "CONTROL / SCRUB" else "PLAYBACK ERROR",
                color = if (playerState.errorMessage.isNullOrBlank()) MonoTokens.Mut else MonoTokens.Accent,
            )
            Spacer(Modifier.weight(1f))
            UpperLabel("${playerState.positionMs.formatDuration()} - ${playerState.durationMs.formatDuration()}", color = MonoTokens.Mut)
        }
        InteractiveRuler(
            fraction = progress,
            onChange = { fraction ->
                if (scrubGate.shouldFire(fraction)) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                actions.seekToFraction(fraction)
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            MonoButton("PREV", modifier = Modifier.weight(1f).testTag("control-prev-zone"), onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                actions.previousTrack()
            })
            MonoButton(if (playerState.isPlaying) "PAUSE" else "PLAY", modifier = Modifier.weight(1f).testTag("control-play-zone"), active = true, onClick = {
                actions.playPause()
            })
            MonoButton("NEXT", modifier = Modifier.weight(1f).testTag("control-next-zone"), onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                actions.nextTrack()
            })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            UpperLabel("VOL", color = MonoTokens.Mut2)
            Spacer(Modifier.width(8.dp))
            InteractiveVolume(
                volume = playerState.volume,
                onChange = { fraction ->
                    if (volumeGate.shouldFire(fraction)) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    actions.setVolumeFraction(fraction)
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            UpperLabel((playerState.volume * 100).toInt().toString(), color = MonoTokens.Mut2)
        }
    }
}

@Composable
private fun InteractiveRuler(fraction: Float, onChange: (Float) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MonoTokens.Line)
            .pointerInput(Unit) {
                detectTapGestures { offset -> onChange(horizontalFraction(offset.x, size.width.toFloat())) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> onChange(horizontalFraction(change.position.x, size.width.toFloat())) }
            }
            .padding(1.dp),
    ) {
        Row(Modifier.fillMaxSize()) {
            repeat(24) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (index % 4 == 0) MonoTokens.Line2 else MonoTokens.Panel2),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(maxWidth * fraction.coerceIn(0f, 1f))
                .background(MonoTokens.Accent.copy(alpha = 0.22f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = (maxWidth * fraction.coerceIn(0f, 1f) - 1.dp).coerceAtLeast(0.dp))
                .width(2.dp)
                .fillMaxHeight()
                .background(MonoTokens.Ink),
        )
    }
}

@Composable
private fun InteractiveVolume(volume: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    var pressActive by remember { mutableStateOf(false) }
    var dragActive by remember { mutableStateOf(false) }
    val isAdjusting = pressActive || dragActive
    val barHeight by animateDpAsState(
        targetValue = if (isAdjusting) 18.dp else 8.dp,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "volumeBarHeight",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        pressActive = true
                        onChange(horizontalFraction(offset.x, size.width.toFloat()))
                        tryAwaitRelease()
                        pressActive = false
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragActive = true
                        onChange(horizontalFraction(offset.x, size.width.toFloat()))
                    },
                    onDragEnd = { dragActive = false },
                    onDragCancel = { dragActive = false },
                ) { change, _ ->
                    onChange(horizontalFraction(change.position.x, size.width.toFloat()))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(MonoTokens.Line),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(volume.coerceIn(0f, 1f))
                .height(barHeight)
                .align(Alignment.CenterStart)
                .background(MonoTokens.Ink),
        )
    }
}

private fun OnboardingStep.headerLabel(): String =
    when (this) {
        OnboardingStep.Welcome -> "SETUP"
        OnboardingStep.Server -> "< SERVER"
        OnboardingStep.SignIn -> "< SIGN IN"
        OnboardingStep.Connected -> "SETUP"
    }

private fun OnboardingStep.progressLabel(): String =
    when (this) {
        OnboardingStep.Welcome -> "v1.0"
        OnboardingStep.Server -> "01 / 02"
        OnboardingStep.SignIn -> "02 / 02"
        OnboardingStep.Connected -> "DONE"
    }

@Preview(widthDp = 411, heightDp = 923)
@Composable
private fun OnboardingWelcomePixel9Preview() {
    DistrictAppContent(AppUiState.Onboarding())
}

@Preview(widthDp = 411, heightDp = 923)
@Composable
private fun LibraryPixel9Preview() {
    DistrictTheme {
        LibraryScreen(
            LibraryUiState(
                session = com.district.jellyfinmono.domain.AuthSession("http://preview", "token", "user", "marcus", "device"),
                albums = listOf(
                    Album("1", "Slow Structures", "A. Molyneux", 2024, 10, null),
                    Album("2", "Ghost Notes", "Molyneux", 2024, 9, null),
                    Album("3", "Field Recordings", "Halden", 2023, 8, null),
                    Album("4", "Neon Districts", "Saibe", 2022, 12, null),
                ),
            ),
        )
    }
}
