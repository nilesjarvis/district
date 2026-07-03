# District

District is a native Android music client for Jellyfin, built with Kotlin, Jetpack Compose, and Media3. It follows a two-half phone UI: browsing/search/album detail in the upper region, with a large one-handed playback control surface in the lower region.

## Features

- Jellyfin server reachability check and username/password authentication.
- Secure session storage with Android Keystore-backed encryption.
- Music library loading, two-column album grid, authenticated cover art, album detail, and live search.
- Media3/ExoPlayer playback using authenticated Jellyfin stream URLs, hosted in a foreground `MediaSessionService` with audio focus, headphone-unplug pausing, and lock-screen/notification controls.
- Persistent playback queue, current track, and position restore.
- Ticked scrub ruler, previous/play-next tap zones, volume bar, haptic detents, and now-playing error/tint state.
- Error/loading/empty states for onboarding, library, search, album detail, playback, session expiry, and persistence failures.

## Project Layout

```text
app/src/main/java/com/district/
  app/              App graph, state, ViewModel, Compose screens
  core/design/      Theme, typography, shell, reusable mono components
  core/media/       Media3 playback controller and control math
  core/network/     Dispatchers and device identity
  core/persistence/ Session and playback stores
  data/jellyfin/    OkHttp Jellyfin API client and repository
  domain/           Models, errors, and repository contract
  feature/          Feature state models
```

## Requirements

- JDK 17
- Android SDK with API 35 installed
- Gradle wrapper from this repository

The app package is `com.district`, with `compileSdk` and `targetSdk` set to 35.

## Build And Test

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

To compile the instrumentation test APK:

```bash
./gradlew assembleDebugAndroidTest
```

To run connected tests, attach a device or start an emulator, then run:

```bash
./gradlew connectedDebugAndroidTest
```

## Optional Real Jellyfin Smoke Test

The JVM suite includes an opt-in real-server smoke test. It is skipped unless all three environment variables are set:

```bash
export JELLYFIN_TEST_URL="http://your-jellyfin-host:8096"
export JELLYFIN_TEST_USERNAME="your-username"
export JELLYFIN_TEST_PASSWORD="your-password"
./gradlew testDebugUnitTest
```

Do not commit real credentials. Passwords and access tokens should only be provided through environment variables or Android secure storage.

## Development Notes

- Generated build outputs, IDE metadata, local prompts/design exports, audit logs, screenshots, signing keys, and environment files are ignored by git.
- Cleartext HTTP should only be allowed for trusted local Jellyfin hosts during development. Review `app/src/main/res/xml/network_security_config.xml` before shipping to a different environment.
- The app is currently portrait-only and optimized for a Pixel 9-like phone viewport.
