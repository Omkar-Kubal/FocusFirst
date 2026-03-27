# FocusFirst

Android focus timer built with Jetpack Compose. The app is published on-device as **Toki** (`app_name` in `strings.xml`).

## What it does

- **Pomodoro-style sessions** — focus phases with short and long breaks, plus presets (Quick 15m, Classic 25m, Deep 35m, Flow 45m) and custom focus length.
- **Reliable timing** — foreground service keeps the timer running; boot handling helps restore expectations after a restart.
- **Home screen widget** — Glance-based widget for at-a-glance status.
- **Stats** — session history and summaries on the Stats tab.
- **Settings** — themes, sounds, DND-related options, battery optimization hints, and other preferences.
- **Pro upgrade** — Google Play Billing integration for optional paid features.

## Tech stack

- **UI:** Jetpack Compose, Material 3  
- **DI:** Hilt  
- **Persistence:** Room, DataStore  
- **Background:** WorkManager (with Hilt), foreground service  
- **Other:** Navigation Compose, Kotlin Coroutines, Play Billing, Glance (App Widget)

## Requirements

- **Android Studio** (recent stable; project uses AGP 8.7.x)  
- **JDK 11** (matches `compileOptions` / `jvmTarget` in `app/build.gradle.kts`)  
- **Device or emulator** with **API 26+** (minSdk 26), targets SDK 35

## Build and run

1. Open the project root (`FocusFirst`) in Android Studio.  
2. Let Gradle sync finish.  
3. Choose the `app` configuration and run on a device or emulator.

Release builds: R8 minification is currently **disabled** in `app/build.gradle.kts` for easier debugging; enable and tune `proguard-rules.pro` before a Play Store release.

## Project layout

- `app/` — Android application module (`com.focusfirst`)  
- `app/src/main/java/com/focusfirst/` — UI screens, view models, data layer, billing, widget, services  
- `gradle/libs.versions.toml` — dependency versions (version catalog)

## License

Add a license here if you publish the repo publicly.
