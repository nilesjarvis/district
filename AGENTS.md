# Repository Guidelines

## Project Overview

District is a native Android music client for Jellyfin. It provides server authentication, music library browsing, live search, offline downloads, and foreground playback with lock-screen controls. Built entirely in Kotlin with Jetpack Compose and Media3. Single-module Gradle project under package `com.district`.

## Architecture & Data Flow

### Pattern: MVVM with Unidirectional StateFlow

Single `AppViewModel` drives all app screen state. No MVI intent/effect channels — actions are direct function calls on the ViewModel.

```
User Action → AppViewModel method → MutableStateFlow<AppUiState> → Compose collectAsState → Re-compose
```

Branch-local state updates use reducer-style helpers scoped to the active sealed-state branch:

```kotlin
setOnboarding { copy(step = OnboardingStep.SignIn) }
updateLibrary { copy(route = LibraryRoute.Albums) }
```

Whole-screen transitions may assign `_uiState.value` directly, such as moving from onboarding to library or recovering from an expired session.

### Layer Structure

```
app/              Presentation — ViewModel, state, Compose UI
feature/          Feature state models (e.g. OnboardingUiState)
domain/           Models, repository interfaces, errors
data/jellyfin/    OkHttp API client, repository implementation
core/             Cross-cutting: design, media, network, persistence, download
```

### Data Flow Between Layers

| Direction | Mechanism |
|---|---|
| Compose → ViewModel | `AppActions` data class carrying lambda refs to ViewModel methods |
| ViewModel → Compose | Single `StateFlow<AppUiState>` collected via `collectAsState()` |
| ViewModel → Repository | `suspend` calls returning `DistrictResult<T>` |
| Repository → API | `suspend` calls throwing `JellyfinHttpException` / `JellyfinParseException` / `IOException` |
| PlaybackController → ViewModel | `StateFlow<PlayerState>` collected in `init {}` |
| DownloadManager → ViewModel | `StateFlow<downloads>` and `StateFlow<activeDownloads>` |

### Error Handling

Sealed-result type across all async boundaries:

```kotlin
sealed interface DistrictResult<out T> {
    data class Success<T>(val value: T) : DistrictResult<T>
    data class Failure(val error: DistrictError) : DistrictResult<Nothing>
}
```

`DistrictError` subtypes: `Network`, `InvalidServerUrl`, `Http(code, message)`, `Parse`, `AuthRejected`, `ExpiredToken`, `Empty`, `Playback`, `Storage`.

Repository maps exceptions to errors (`runCatchingDistrict`). UI renders via `DistrictError.label()` extension.

### Navigation

Manual state-based — no Compose Navigation library. `AppUiState` sealed interface (`Onboarding` | `Library`). Within Library, `LibraryRoute` enum + `backStack: List<LibraryRoute>` for back navigation.

### Dependency Injection

Hand-rolled service locator (`AppGraph`). No Hilt/Dagger/Koin. Constructor injection everywhere.

```
DistrictApplication.onCreate() → AppGraph(context)
  → OkHttpClient, stores, controller, download manager, repository
  → appViewModel() factory
```

Test doubles use the same constructor injection with swapped implementations (`InMemorySessionStore`, `FakeRepository`).

## Key Directories

| Path | Purpose |
|---|---|
| `app/src/main/java/com/district/app/` | AppGraph, AppState, AppViewModel, DistrictApp (Compose root) |
| `app/src/main/java/com/district/core/design/` | Theme, tokens, shell layout, UI components, typography |
| `app/src/main/java/com/district/core/media/` | PlaybackController, Media3 controller, PlaybackService, control math |
| `app/src/main/java/com/district/core/network/` | DispatcherProvider, DeviceIdProvider |
| `app/src/main/java/com/district/core/persistence/` | SessionStore, PlaybackStore, DownloadStore + implementations |
| `app/src/main/java/com/district/core/download/` | DownloadManager and Android implementation |
| `app/src/main/java/com/district/data/jellyfin/` | JellyfinApi interface, OkHttp implementation, repository |
| `app/src/main/java/com/district/domain/` | Models, errors, repository contract |
| `app/src/main/java/com/district/feature/` | Feature state models |
| `app/src/test/java/` | Unit tests (mirror source packages) |
| `app/src/androidTest/java/` | Instrumentation tests (Compose UI, playback smoke) |

## Development Commands

```bash
# Build
./gradlew assembleDebug

# Unit tests (JVM, includes Robolectric)
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug

# Instrumentation tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Instrumentation test APK only
./gradlew assembleDebugAndroidTest

# Specific test class
./gradlew testDebugUnitTest --tests "com.district.app.AppViewModelTest"

# Real Jellyfin smoke test (requires env vars)
JELLYFIN_TEST_URL=http://host:8096 \
JELLYFIN_TEST_USERNAME=user \
JELLYFIN_TEST_PASSWORD=pass \
./gradlew testDebugUnitTest
```

## Code Conventions & Common Patterns

### Naming

| Element | Convention | Example |
|---|---|---|
| Files | PascalCase | `AppViewModel.kt` |
| UI components | `Mono` prefix | `MonoAlbumTile`, `MonoNowPlayingBar` |
| Store implementations | Platform prefix | `AndroidSecureSessionStore`, `SharedPreferencesPlaybackStore` |
| Test doubles | `Fake` or `InMemory` prefix | `FakeRepository`, `InMemorySessionStore` |
| Functions | camelCase | `playQueue`, `checkServer` |
| Constants | SCREAMING_SNAKE_CASE | `KEY_DEVICE_ID`, `PLAYBACK_PERSIST_INTERVAL_MS` |
| Design tokens | `MonoTokens` object, PascalCase props | `MonoTokens.Bg`, `MonoTokens.Accent` |

### Async

- All I/O is `suspend` functions via coroutines.
- `StateFlow` for reactive UI state; `SharedFlow` is used for the playback auth-error bridge.
- `DispatcherProvider` abstracts dispatchers (`io`, `default`, `main`) — injectable for testing.
- Search debouncing: `searchJob?.cancel()` + `delay(150)`.
- Playback persistence throttled every 2.5 seconds.

### No Mocking Framework

Tests use hand-rolled fakes and stubs exclusively (no Mockito). Reusable in-memory stores live in `core/persistence/FakeStores.kt`; feature-specific fakes and recording stubs are often private classes inside the relevant test file.

### JSON Parsing

Raw `org.json` (`JSONObject`/`JSONArray`). No Retrofit, Gson, or Moshi.

### Compose UI

- All UI is Compose — XML resources are minimal (theme, backup rules, network config, launcher icon).
- Edge-to-edge with dark system bars matching `MonoTokens.BgInt`.
- Library UI uses a 5-zone vertical layout: `MonoShell` (header, contextual bar, scroll region, now-playing, control zone).
- Semantic test tags on interactive elements (`now-playing-bar`, `album-tile-{id}`, etc.).
- Single string resource (`app_name`); other text is hardcoded in Compose.

### Design Tokens

Dark terminal aesthetic with JetBrains Mono font. Accent color `#d17a4a`. Defined in `MonoTokens`: `Bg`, `Panel`, `Ink`, `Mut`, `Mut2`, `Line`, `Line2`, `Accent`, `Ok`, `Tint`, `CoverBrown/Blue/Green/Violet`.

### Key Architectural Patterns

1. **PlaybackSessionBridge** — singleton bridging auth between UI-side MediaController and system-instantiated MediaSessionService.
2. **Pending command queue** — `Media3PlaybackController` buffers commands until bound.
3. **Offline fallback** — startup and library album-loading `DistrictError.Network` failures switch to the Downloads route.
4. **Session expiry recovery** — `ExpiredToken` anywhere clears session and navigates to onboarding.

## Important Files

| File | Role |
|---|---|
| `DistrictApplication.kt` | Entry point; creates `AppGraph` |
| `MainActivity.kt` | Single activity; wires ViewModel, Compose content |
| `app/AppGraph.kt` | Service locator — all dependency wiring |
| `app/AppViewModel.kt` | Single ViewModel for the entire app |
| `app/DistrictApp.kt` | Root Compose content (~1700 lines) |
| `domain/DistrictError.kt` | Error types and `DistrictResult` |
| `domain/Models.kt` | Domain data classes |
| `domain/JellyfinRepository.kt` | Repository interface |
| `core/media/PlaybackService.kt` | Foreground MediaSessionService |
| `core/persistence/AndroidSecureSessionStore.kt` | Keystore-backed encrypted session storage |
| `data/jellyfin/OkHttpJellyfinApi.kt` | HTTP API implementation |
| `AndroidManifest.xml` | Permissions, MainActivity, PlaybackService |
| `app/build.gradle.kts` | All dependencies, build config |

## Runtime & Tooling

- **JDK 17** minimum.
- **Android SDK 35** (compile and target), minSdk 26.
- **Gradle 9.6.1** via wrapper. AGP 9.2.0, Kotlin 2.2.10 (built-in).
- **Compose BOM 2025.05.01** manages all Compose versions.
- **Media3 1.8.1** for ExoPlayer + MediaSession.
- **OkHttp 4.12.0**, **Coil 2.7.0**, **DataStore Preferences 1.2.1** declared but currently unused.
- **Portrait-only**, optimized for Pixel 9 viewport (411x923dp).
- Cleartext HTTP allowed for local Jellyfin servers (see `network_security_config.xml`).

## Testing & QA

### Frameworks

| Framework | Usage |
|---|---|
| JUnit 4 | All tests |
| kotlinx-coroutines-test | Deterministic scheduling (`runTest`, `StandardTestDispatcher`, `advanceTimeBy`) |
| MockWebServer | API and download tests |
| Robolectric 4.16 | SharedPreferences, file I/O tests |
| Compose UI Test JUnit4 | UI layout, gesture, and interaction tests |

### Conventions

- **Class naming**: `${ClassName}Test`
- **Method naming**: Descriptive camelCase sentences — `signInRejectedStaysOnSignInWithAuthError`
- **File location**: Mirror source packages under `src/test/java/com/district/` or `src/androidTest/java/com/district/`
- **Test doubles**: Hand-rolled `Fake*` and `InMemory*` implementations — no mocking framework
- **Compose UI tests**: `createComposeRule()`, fixed Pixel 9 size, semantic tags for locators

### Coverage

Well-covered: ViewModel state machine, API parsing, download lifecycle, persistence round-trips, Compose layout, playback smoke paths.

Not covered: Coil image loading, Palette extraction, PlaybackService binding lifecycle beyond smoke tests, volume/haptics, MainActivity lifecycle. DataStore is declared but currently unused.
