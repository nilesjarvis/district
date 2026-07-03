package com.district.app

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.district.core.design.DistrictTheme
import com.district.core.design.JetBrainsMono
import com.district.core.design.MonoAlbumTile
import com.district.core.design.MonoButton
import com.district.core.design.MonoLedgerRow
import com.district.core.design.MonoNowPlayingBar
import com.district.core.design.MonoPrimaryButton
import com.district.core.design.MonoScrubPreview
import com.district.core.design.MonoShell
import com.district.core.design.MonoStatusDot
import com.district.core.design.MonoTokens
import com.district.core.design.MonoTrackRow
import com.district.core.design.MonoVolumeBar
import com.district.core.design.ShellMetrics
import com.district.core.design.UpperLabel
import com.district.core.design.coverTint
import com.district.domain.Album
import com.district.domain.Artist
import com.district.domain.DistrictError
import com.district.domain.DownloadState
import com.district.domain.DownloadedAlbum
import com.district.domain.SearchResults
import com.district.domain.Track
import com.district.core.media.DetentHapticGate
import com.district.core.media.PlayerState
import com.district.core.media.horizontalFraction
import com.district.feature.onboarding.OnboardingStep
import com.district.feature.onboarding.OnboardingUiState
import kotlinx.coroutines.delay

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
            openArtist = viewModel::openArtist,
            openDownloads = viewModel::openDownloads,
            openDownloadedAlbum = viewModel::openDownloadedAlbum,
            downloadSelectedAlbum = viewModel::downloadSelectedAlbum,
            deleteDownload = viewModel::deleteDownload,
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
    val openArtist: (Artist) -> Unit = {},
    val backToLibrary: () -> Unit = {},
    val playAlbumFromStart: () -> Unit = {},
    val playTrack: (Track) -> Unit = {},
    val playPause: () -> Unit = {},
    val nextTrack: () -> Unit = {},
    val previousTrack: () -> Unit = {},
    val seekToFraction: (Float) -> Unit = {},
    val setVolumeFraction: (Float) -> Unit = {},
    val openDownloads: () -> Unit = {},
    val openDownloadedAlbum: (DownloadedAlbum) -> Unit = {},
    val downloadSelectedAlbum: () -> Unit = {},
    val deleteDownload: (String) -> Unit = {},
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
                Text("DT", color = MonoTokens.Ink, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text("DISTRICT", color = MonoTokens.Ink, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            UpperLabel("JELLYFIN CLIENT", color = MonoTokens.Mut)
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
    var focused by remember { mutableStateOf(false) }
    Column(modifier) {
        UpperLabel(label, color = if (focused) MonoTokens.Accent else MonoTokens.Mut2)
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
                .border(1.dp, if (focused) MonoTokens.Accent else MonoTokens.Line2)
                .onFocusChanged { focused = it.isFocused }
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
    val albumGridState = rememberLazyGridState()
    val albumDetailGridState = rememberLazyGridState()
    LaunchedEffect(state.selectedAlbum?.id) {
        albumDetailGridState.scrollToItem(0)
    }
    var keepControlsExpandedForTrackHandoff by remember { mutableStateOf(false) }
    LaunchedEffect(keepControlsExpandedForTrackHandoff, state.playerState.currentTrack?.id) {
        if (keepControlsExpandedForTrackHandoff) {
            delay(CONTROL_ZONE_HANDOFF_GRACE_MS)
            keepControlsExpandedForTrackHandoff = false
        }
    }
    val controlActions = actions.copy(
        nextTrack = {
            keepControlsExpandedForTrackHandoff = true
            actions.nextTrack()
        },
        previousTrack = {
            keepControlsExpandedForTrackHandoff = true
            actions.previousTrack()
        },
    )
    val targetControlZoneHeight = when {
        state.route == LibraryRoute.Search -> 0.dp
        state.playerState.currentTrack == null -> 0.dp
        // Key off playback intent, not isPlaying: a scrub re-buffers and momentarily flips
        // isPlaying false, which would otherwise collapse the zone mid-drag. Stay open while
        // playing or seeking; collapse only when the user pauses.
        state.playerState.playWhenReady || keepControlsExpandedForTrackHandoff -> ShellMetrics.ControlZoneHeight
        else -> 0.dp
    }
    val controlZoneHeight by animateDpAsState(
        targetValue = targetControlZoneHeight,
        animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing),
        label = "controlZoneHeight",
    )
    val playerTint = rememberPlayerTint(state.playerState)
    MonoShell(
        header = {
            LibraryTopBar(state, actions)
        },
        contextualBar = null,
        scrollRegion = {
            when (state.route) {
                LibraryRoute.Albums -> LibraryAlbumGrid(state, actions, albumGridState)
                LibraryRoute.Search -> SearchResultsRegion(state, actions)
                LibraryRoute.AlbumDetail -> AlbumDetailRegion(state, actions, albumDetailGridState)
                LibraryRoute.ArtistDetail -> ArtistDetailRegion(state, actions)
                LibraryRoute.Downloads -> DownloadsRegion(state, actions)
            }
        },
        nowPlaying = { NowPlayingFromState(state, actions, playerTint) },
        controlZone = { PlayerControlZone(state.playerState, controlActions, playerTint) },
        controlZoneHeight = controlZoneHeight,
    )
}

@Composable
private fun LibraryTopBar(state: LibraryUiState, actions: AppActions) {
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
                    .padding(vertical = 13.dp),
            )
            SearchGlyph(color = MonoTokens.Mut2)
        }
        LibraryRoute.AlbumDetail -> Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = actions.backToLibrary)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UpperLabel("< ALBUM", color = MonoTokens.Ink)
            Spacer(Modifier.weight(1f))
            UpperLabel("BACK", color = MonoTokens.Mut2)
        }
        LibraryRoute.ArtistDetail -> Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = actions.backToLibrary)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UpperLabel("< ARTIST", color = MonoTokens.Ink)
            Spacer(Modifier.weight(1f))
            UpperLabel("BACK", color = MonoTokens.Mut2)
        }
        LibraryRoute.Downloads -> Row(
            modifier = Modifier
                .fillMaxSize()
                .then(if (state.backStack.isNotEmpty()) Modifier.clickable(onClick = actions.backToLibrary) else Modifier)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UpperLabel(if (state.backStack.isNotEmpty()) "< DOWNLOADS" else "DOWNLOADS", color = MonoTokens.Ink)
            Spacer(Modifier.weight(1f))
            if (state.isOffline) UpperLabel("OFFLINE", color = MonoTokens.Accent) else UpperLabel("BACK", color = MonoTokens.Mut2)
        }
        LibraryRoute.Albums -> Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UpperLabel("DISTRICT|LIB", color = MonoTokens.Ink)
            Spacer(Modifier.weight(1f))
            DownloadsIconButton(onClick = actions.openDownloads)
            Spacer(Modifier.width(6.dp))
            SearchIconButton(onClick = actions.activateSearch)
        }
    }
}

@Composable
private fun SearchIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(46.dp)
            .semantics { contentDescription = "Search library" }
            .testTag("search-library-action")
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SearchGlyph(color = MonoTokens.Ink)
    }
}

@Composable
private fun SearchGlyph(color: Color) {
    Canvas(
        modifier = Modifier
            .width(18.dp)
            .height(18.dp),
    ) {
        val strokeWidth = 2.dp.toPx()
        drawCircle(
            color = color,
            radius = size.minDimension * 0.32f,
            center = Offset(size.width * 0.43f, size.height * 0.43f),
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.64f, size.height * 0.64f),
            end = Offset(size.width * 0.86f, size.height * 0.86f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square,
        )
    }
}

@Composable
private fun LibraryAlbumGrid(state: LibraryUiState, actions: AppActions, gridState: LazyGridState) {
    if (!state.isLoading && state.error == null && state.albums.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MonoTokens.Bg)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            UpperLabel("NO ALBUMS", color = MonoTokens.Mut, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "This music library is empty.\nAdd albums on your Jellyfin server.",
                color = MonoTokens.Mut2,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("library-album-grid")
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
                    MonoTrackRow(
                        number = "AR",
                        title = artist.name,
                        duration = "›",
                        onClick = { actions.openArtist(artist) },
                    )
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
private fun AlbumDetailRegion(state: LibraryUiState, actions: AppActions, gridState: LazyGridState) {
    val album = state.selectedAlbum
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("album-detail-grid")
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
                val artistId = album.artistId
                val artistName = album.artist.ifBlank { "Unknown artist" }
                val trackCount = album.trackCount ?: state.albumTracks.size
                val totalDuration = state.albumTracks.sumOf { it.durationMs }.formatDuration()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MonoTokens.Panel)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    UpperLabel(album.productionYear?.toString() ?: "ALBUM", color = MonoTokens.Mut2)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = album.title,
                        color = MonoTokens.Ink,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 22.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .then(
                                if (artistId != null) {
                                    Modifier.clickable {
                                        actions.openArtist(Artist(id = artistId, name = album.artist))
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (artistId != null) "$artistName ›" else artistName,
                            color = if (artistId != null) MonoTokens.Ink else MonoTokens.Mut,
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Column {
                        UpperLabel("${trackCount.toString().padStart(2, '0')} TRACKS", color = MonoTokens.Mut2)
                        UpperLabel(totalDuration, color = MonoTokens.Mut2)
                    }
                }
            }
            item(span = { GridItemSpan(2) }) {
                MonoPrimaryButton("PLAY ALL", modifier = Modifier
                    .height(44.dp)
                    .fillMaxWidth(), onClick = actions.playAlbumFromStart)
            }
            if (state.albumTracks.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    AlbumDownloadControl(state, album, actions)
                }
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
                    modifier = Modifier.testTag("album-track-${track.id}"),
                    isPlaying = state.playerState.currentTrack?.id == track.id,
                    onClick = { actions.playTrack(track) },
                )
            }
        }
    }
}

@Composable
private fun ArtistDetailRegion(state: LibraryUiState, actions: AppActions) {
    val artist = state.selectedArtist
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .testTag("artist-detail-region")
            .background(MonoTokens.Line)
            .border(1.dp, MonoTokens.Line),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        if (artist != null) {
            item(span = { GridItemSpan(2) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MonoTokens.Panel)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    UpperLabel("ARTIST", color = MonoTokens.Mut2)
                    Text(
                        text = artist.name,
                        color = MonoTokens.Ink,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    UpperLabel(
                        if (state.isArtistLoading) "LOADING DISCOGRAPHY" else "${state.artistAlbums.size} ALBUMS",
                        color = MonoTokens.Mut,
                    )
                }
            }
        }
        if (state.error != null) {
            item(span = { GridItemSpan(2) }) {
                StateTile(state.error.label(), color = MonoTokens.Accent, height = 72.dp)
            }
        }
        if (state.isArtistLoading) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(MonoTokens.Panel),
                    contentAlignment = Alignment.Center,
                ) {
                    UpperLabel("LOADING DISCOGRAPHY", color = MonoTokens.Mut)
                }
            }
        } else if (artist != null && state.error == null && state.artistAlbums.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                StateTile("NO ALBUMS", color = MonoTokens.Mut, height = 72.dp)
            }
        } else {
            itemsIndexed(state.artistAlbums) { index, album ->
                AlbumTileWithArt(
                    album = album,
                    fallbackColor = previewColor(index),
                    onClick = { actions.openAlbum(album) },
                )
            }
        }
    }
}

@Composable
private fun AlbumDownloadControl(state: LibraryUiState, album: Album, actions: AppActions) {
    val active = state.downloadStates[album.id]
    val isDownloaded = state.downloads.any { it.id == album.id }
    when {
        active is DownloadState.InProgress -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("download-control")
                .background(MonoTokens.Panel)
                .border(1.dp, MonoTokens.Line),
            contentAlignment = Alignment.Center,
        ) {
            UpperLabel("DOWNLOADING ${active.completedTracks}/${active.totalTracks}", color = MonoTokens.Accent)
        }
        isDownloaded -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("download-control"),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MonoTokens.Panel)
                    .border(1.dp, MonoTokens.Line),
                contentAlignment = Alignment.Center,
            ) {
                UpperLabel("DOWNLOADED", color = MonoTokens.Ok)
            }
            MonoButton(
                "DELETE",
                modifier = Modifier.weight(1f).testTag("download-delete"),
                onClick = { actions.deleteDownload(album.id) },
            )
        }
        active is DownloadState.Failed -> MonoButton(
            "RETRY DOWNLOAD",
            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("download-control"),
            onClick = actions.downloadSelectedAlbum,
        )
        else -> MonoButton(
            "DOWNLOAD ALBUM",
            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("download-control"),
            onClick = actions.downloadSelectedAlbum,
        )
    }
}

@Composable
private fun DownloadsRegion(state: LibraryUiState, actions: AppActions) {
    val downloads = state.downloads.sortedByDescending { it.downloadedAtEpochMs }
    val totalBytes = downloads.sumOf { it.sizeBytes }
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier
            .fillMaxSize()
            .testTag("downloads-region")
            .background(MonoTokens.Line)
            .border(1.dp, MonoTokens.Line),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MonoTokens.Panel)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                UpperLabel("DOWNLOADS", color = MonoTokens.Mut2)
                Text(
                    text = "${downloads.size} ${if (downloads.size == 1) "ALBUM" else "ALBUMS"} - ${formatBytes(totalBytes)}",
                    color = MonoTokens.Ink,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                if (state.isOffline) UpperLabel("PLAYING FROM DEVICE STORAGE", color = MonoTokens.Mut)
            }
        }
        if (downloads.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MonoTokens.Bg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    UpperLabel("NO DOWNLOADS", color = MonoTokens.Mut, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Open an album and tap Download to listen offline.",
                        color = MonoTokens.Mut2,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        items(downloads) { album ->
            DownloadRow(
                album = album,
                onOpen = { actions.openDownloadedAlbum(album) },
                onDelete = { actions.deleteDownload(album.id) },
            )
        }
    }
}

@Composable
private fun DownloadRow(album: DownloadedAlbum, onOpen: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .testTag("download-${album.id}")
            .background(MonoTokens.Panel)
            .clickable(onClick = onOpen)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = album.title,
                color = MonoTokens.Ink,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            UpperLabel("${album.trackCount} ${if (album.trackCount == 1) "TRACK" else "TRACKS"} - ${formatBytes(album.sizeBytes)}", color = MonoTokens.Mut2, fontSize = 8.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .defaultMinSize(minWidth = 68.dp)
                .testTag("download-delete-${album.id}")
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            UpperLabel("DELETE", color = MonoTokens.Accent)
        }
    }
}

@Composable
private fun DownloadsIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(46.dp)
            .semantics { contentDescription = "Downloads" }
            .testTag("downloads-action")
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 2.dp.toPx()
            drawLine(MonoTokens.Ink, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.62f), stroke, cap = StrokeCap.Round)
            drawLine(MonoTokens.Ink, Offset(w * 0.28f, h * 0.42f), Offset(w * 0.5f, h * 0.66f), stroke, cap = StrokeCap.Round)
            drawLine(MonoTokens.Ink, Offset(w * 0.72f, h * 0.42f), Offset(w * 0.5f, h * 0.66f), stroke, cap = StrokeCap.Round)
            drawLine(MonoTokens.Ink, Offset(w * 0.24f, h * 0.86f), Offset(w * 0.76f, h * 0.86f), stroke, cap = StrokeCap.Round)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return when {
        mb >= 1024.0 -> String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
        mb >= 10.0 -> "${mb.toInt()} MB"
        else -> String.format(java.util.Locale.US, "%.1f MB", mb)
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
            .testTag("album-tile-${album.id}")
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
        com.district.core.design.CoverPlaceholder(fallbackColor, modifier)
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
private fun rememberPlayerTint(playerState: PlayerState): Color {
    val track = playerState.currentTrack
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
        val palette = Palette.from(bitmap).generate()
        val sampled = listOf(
            palette.getMutedColor(0),
            palette.getDominantColor(0),
            palette.getVibrantColor(0),
        ).firstOrNull { it != 0 }
        if (sampled != null) {
            sampledTint = Color(sampled.toLong() and 0xFFFFFFFFL)
        }
    }
    return sampledTint
}

@Composable
private fun NowPlayingFromState(state: LibraryUiState, actions: AppActions, tintColor: Color) {
    val playerState = state.playerState
    val track = playerState.currentTrack
    val linkedAlbum = state.albumForTrack(track)
    val error = playerState.errorMessage?.takeIf { it.isNotBlank() }
    val coverResource = track?.coverArt
    MonoNowPlayingBar(
        title = if (error != null && track == null) "Playback error" else track?.title ?: "Ready",
        artist = error ?: track?.artist?.ifBlank { "District" } ?: "District",
        code = when {
            error != null -> "ERR"
            playerState.isPlaying -> "PLAY"
            track != null -> "PAUSE"
            else -> "IDLE"
        },
        elapsed = playerState.positionMs.formatDuration(),
        duration = playerState.durationMs.formatDuration(),
        isPlaying = playerState.isPlaying,
        isError = error != null,
        coverColor = tintColor,
        tintColor = tintColor,
        modifier = Modifier
            .testTag("now-playing-bar"),
        onTitleClick = linkedAlbum?.let { album ->
            { actions.openAlbum(album) }
        },
        onActionClick = if (track != null && error == null) actions.playPause else null,
        cover = coverResource?.let { resource ->
            {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(resource.url)
                        .apply {
                            resource.authHeaders?.asMap()?.forEach { (name, value) -> addHeader(name, value) }
                        }
                        .build(),
                    contentDescription = "${track?.title ?: "Current track"} cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    )
}

private fun LibraryUiState.albumForTrack(track: Track?): Album? {
    val albumId = track?.albumId?.takeIf { it.isNotBlank() } ?: return null
    return sequenceOf(selectedAlbum)
        .filterNotNull()
        .plus(albums.asSequence())
        .plus(searchResults.albums.asSequence())
        .plus(artistAlbums.asSequence())
        .firstOrNull { it.id == albumId }
        ?: Album(
            id = albumId,
            title = "Current album",
            artist = track.artist.ifBlank { "Unknown artist" },
            productionYear = null,
            trackCount = null,
            coverArt = track.coverArt,
            tintArgb = track.tintArgb,
        )
}

@Composable
private fun PlayerControlZone(playerState: PlayerState, actions: AppActions, tintColor: Color) {
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
            .background(coverTint(tintColor, alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InteractiveRuler(
            fraction = progress,
            tintColor = tintColor,
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
            PlaybackControlButton("PREV", tintColor = tintColor, modifier = Modifier.weight(1f).testTag("control-prev-zone"), onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                actions.previousTrack()
            })
            PlaybackControlButton(if (playerState.isPlaying) "PAUSE" else "PLAY", tintColor = tintColor, modifier = Modifier.weight(1f).testTag("control-play-zone"), active = true, onClick = {
                actions.playPause()
            })
            PlaybackControlButton("NEXT", tintColor = tintColor, modifier = Modifier.weight(1f).testTag("control-next-zone"), onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                actions.nextTrack()
            })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            UpperLabel("VOL", color = MonoTokens.Mut2)
            Spacer(Modifier.width(8.dp))
            InteractiveVolume(
                volume = playerState.volume,
                tintColor = tintColor,
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
private fun PlaybackControlButton(
    text: String,
    tintColor: Color,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .defaultMinSize(minHeight = ShellMetrics.MinTouchTarget)
            .background(MonoTokens.Panel)
            .background(coverTint(tintColor, alpha = if (active) 0.58f else 0.26f))
            .border(1.dp, coverTint(tintColor, alpha = if (active) 0.80f else 0.42f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        UpperLabel(
            text = text,
            color = MonoTokens.Ink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun InteractiveRuler(fraction: Float, tintColor: Color, onChange: (Float) -> Unit) {
    val clamped = fraction.coerceIn(0f, 1f)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("playback-scrub-ruler")
            .background(coverTint(tintColor, alpha = 0.18f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    var latestPosition = down.position
                    var totalDrag = Offset.Zero
                    var isDragging = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        latestPosition = change.position

                        if (!change.pressed) break

                        val delta = change.positionChange()
                        totalDrag += delta
                        if (!isDragging && totalDrag.getDistance() > viewConfiguration.touchSlop) {
                            isDragging = true
                        }
                        if (isDragging) {
                            change.consume()
                            onChange(horizontalFraction(change.position.x, size.width.toFloat()))
                        }
                    }

                    if (!isDragging) {
                        onChange(horizontalFraction(latestPosition.x, size.width.toFloat()))
                    }
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(maxWidth * clamped)
                .background(coverTint(tintColor, alpha = 0.48f)),
        )
    }
}

@Composable
private fun InteractiveVolume(volume: Float, tintColor: Color, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
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
                .background(coverTint(tintColor, alpha = 0.16f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(volume.coerceIn(0f, 1f))
                .height(barHeight)
                .align(Alignment.CenterStart)
                .background(coverTint(tintColor, alpha = 0.52f)),
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

private const val CONTROL_ZONE_HANDOFF_GRACE_MS = 360L

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
                session = com.district.domain.AuthSession("http://preview", "token", "user", "demo", "device"),
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
