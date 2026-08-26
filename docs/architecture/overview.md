# Architecture Overview

## 1. Project Structure

Pixel Meter is a single-module Android application. The main module is `app/`, and the root package is `vip.mystery0.pixel.meter`.

- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Architecture: layered MVVM + Repository
- Dependency injection: Koin
- Persistence: Preferences DataStore
- State: StateFlow
- Minimum version: Android 12 / API 31

`gradle/libs.versions.toml` is the source of truth for dependencies and toolchain versions.

## 2. Layers

```text
Android System APIs
  ├─ ConnectivityManager / NetworkCallback
  ├─ TrafficStats
  ├─ NotificationManager
  ├─ WindowManager
  └─ DataStore
          ↓
DataSource / Repository
  ├─ SpeedDataSource
  ├─ DataStoreRepository
  └─ NetworkRepository
          ↓
Service / ViewModel
  ├─ NetworkMonitorService
  ├─ MainViewModel
  └─ SettingsViewModel
          ↓
Compose UI
  ├─ MainActivity
  ├─ SettingsActivity
  ├─ OnboardingScreen
  └─ OverlayWindow
```

## 3. Real-Time Speed Data Flow

1. `SpeedDataSource` uses `NetworkCallback` to cache physical networks and their interface names.
2. `NetworkRepository` calls `getTrafficData()` at the user-selected sampling interval.
3. The Repository calculates upload and download speeds from byte-counter and time deltas.
4. Results are published through `StateFlow<NetSpeedData>`.
5. `NetworkMonitorService` collects the speed state to update notifications, Live Update, and the Overlay.
6. The main screen observes the same StateFlow to display current values.

## 4. Settings Data Flow

1. `DataStoreRepository` defines and reads or writes all Preferences keys.
2. `NetworkRepository` maps settings to long-lived `StateFlow` instances.
3. ViewModels expose state to Compose and delegate updates to the Repository.
4. The Service, `NotificationHelper`, and `OverlayWindow` consume the same state to keep behavior consistent.

## 5. Main Components

### SpeedDataSource

Identifies networks and reads per-interface byte counters. It does not calculate speed deltas or handle UI and persistence.

### NetworkRepository

Owns speed sampling, delta calculation, and application-level settings state. It is the shared state hub for the Service and UI.

### SpeedFormatter

A stateless formatter that keeps value precision and units consistent across the main screen, notification Bitmap, Live Update, Overlay, and threshold summaries.

### NetworkMonitorService

Owns long-running monitoring, sampling lifecycle, notification publication, and Overlay updates. It does not contain settings-screen logic.

### MainViewModel / SettingsViewModel

Handle UI events, permission state, and Repository interaction. Activities retain only system launcher, Intent, and Compose-host responsibilities.

## 6. Dependency Injection

`di/AppModule.kt` is the Koin registration entry point. Koin provides Android system services, DataStore, repositories, notification helpers, and `OverlayWindow`.

## 7. Key Constraints

- Do not require Root, Shizuku, or persistent ADB privileges.
- Do not identify VPN interfaces with an interface-name blacklist.
- A Foreground Service must retain an ongoing notification.
- Permission requests must originate from user-visible actions.
- Source code takes precedence over documentation for versions, keys, and actual system behavior.
