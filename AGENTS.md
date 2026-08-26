# AGENTS.md

This file defines the shared collaboration rules for Pixel Meter. Developers, automation, and coding agents must follow it before making changes.

## 1. Collaboration

- Respond in the user's language unless requested otherwise.
- Use English for code comments, KDoc, project documentation, runtime logs, and new Git commit messages.
- Prefer English for new Issues and Pull Requests, but allow other languages when useful.
- Keep official terms such as Interface, Transport, Foreground Service, Overlay, StateFlow, Repository, and Live Update in English.
- Clarify ambiguous requirements, especially around network filtering, permissions, Foreground Services, Overlays, and background execution.
- Consider Android background limits, notification and Overlay permissions, Foreground Service types, and Google Play policy.
- Core features must not require Root, Shizuku, or persistent ADB privileges.
- Keep business logic out of Activities; state and persistence belong in ViewModels, repositories, or data sources.
- Keep source files below 1,000 lines where practical.

## 2. Product

Pixel Meter is a real-time network speed monitor for Google Pixel and stock or near-stock Android devices. It identifies physical Wi-Fi, cellular, and Ethernet networks with `ConnectivityManager.NetworkCallback`, excludes `TRANSPORT_VPN`, and reads per-interface counters with `TrafficStats.getRxBytes/getTxBytes` to avoid duplicate VPN traffic counting.

## 3. Toolchain

- Module: `app/`
- Min SDK: 31; Compile SDK / Target SDK: 37; JVM Target: 21
- `gradle/libs.versions.toml` is the source of truth for Kotlin, AGP, Compose BOM, dependencies, and `app-version`.
- Current major versions: Kotlin 2.4.10, AGP 9.3.2, Compose BOM 2026.08.00
- Global opt-in: `ExperimentalMaterial3Api`
- `versionCode`: Git commit count
- `versionName`: `app-version` + build-type suffix + Git information
- Locales: English, Simplified Chinese, Portuguese, Brazilian Portuguese, and Russian
- Default resources: English; see `app/src/main/res/resources.properties`

## 4. Build and Validation

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew lint
```

Use `gradlew.bat` on Windows. Release builds require signing configuration.

Unless explicitly requested, the project does not require unit or Android tests. After code changes, run `:app:assembleDebug` and `lint`. Validate network statistics, notifications, Overlay, or Foreground Service changes on a Pixel device. For data-source changes, verify VPN traffic is not counted twice. For resource changes, verify all `values-*` translations. Documentation-only tasks may use an explicitly approved narrower scope.

## 5. Architecture

Package root: `app/src/main/kotlin/vip/mystery0/pixel/meter/`

```text
SpeedDataSource
  → caches physical Networks and interface names; reads TrafficStats
NetworkRepository
  → calculates speed and exposes DataStore settings as StateFlow
NetworkMonitorService
  ├→ NotificationHelper: base notification, Bitmap icon, Live Update
  └→ OverlayWindow: Compose + WindowManager Overlay
MainViewModel / SettingsViewModel
  → expose Repository state to Compose UI
```

- `SpeedDataSource`: physical-interface filtering and counters; excludes `TRANSPORT_VPN` without name blacklists.
- `DataStoreRepository`: Preferences DataStore named `pixel_pulse_preferences`.
- `NetworkRepository`: sampling, speed calculation, and shared application state.
- `SpeedFormatter`: common speed formatting for UI, notifications, Live Update, and Overlay.
- `NetworkMonitorService`: Foreground Service lifecycle, notifications, and Overlay updates.
- `NotificationHelper`: base notification, dynamic Bitmap icon, and Android 16+ Live Update.
- `OverlayWindow`: draggable Compose Overlay with persistence, layout, immersive, and low-traffic behavior.
- `OnboardingScreen`: three-step setup wizard.
- `service/tile/`: Quick Settings Tiles.
- `BootReceiver`: optional boot startup.
- `AppModule`: Koin registrations.

## 6. UI and Android Requirements

- Use Jetpack Compose, Material 3, Material You, and edge-to-edge layouts.
- Dynamic Color is the default; fixed colors and AMOLED Black are supported.
- New installations enable no display method until the user selects one in Onboarding.
- A base Foreground Service notification is always required while monitoring.
- Live Update is Android 16+ only.
- Overlay uses `TYPE_APPLICATION_OVERLAY` and requires `SYSTEM_ALERT_WINDOW`.
- Check `POST_NOTIFICATIONS` only on Android 13+; do not block Android 12/12L.
- Android 14+ Foreground Service starts must respect background restrictions.
- `BootReceiver` must catch startup exceptions.
- Validate Overlay, system-bar, cutout, and immersive-mode changes on a real device.
- Explain Android and Google Play constraints when changing background-survival behavior.

## 7. Localization

- `values/strings.xml` must remain complete English fallback resources.
- `translatable="false"` excludes translation; it does not restrict Locale visibility.
- Locale Config is generated from `resources.properties` and `values-*` directories.
- Weblate manages translations. Add new translatable strings to every current Locale.
- Preserve format arguments and XML escaping across locales.

See `docs/architecture/localization.md`.

## 8. Documentation

Documentation index: `docs/README.md`

- Architecture: `docs/architecture/`
- UI and interaction: `docs/ui/`
- Product images: `docs/*.png`
- Designs and plans: `docs/plans/`
- Naming: `YYYY-MM-DD-topic-design.md` and `YYYY-MM-DD-topic-plan.md`

Update relevant documentation when changing architecture, DataStore, permissions, service lifecycle, or user interaction.

## 9. Completion Checklist

Before reporting completion, check documentation, permissions and background behavior, DataStore keys and defaults, String Resource translations, required builds and Lint, device-validation needs, and that `gradle/libs.versions.toml` remains the version source of truth.

## 10. Release Signing

Release signing reads `local.properties`, then falls back to same-named environment variables:

- `SIGN_KEY_STORE_FILE`
- `SIGN_KEY_STORE_PASSWORD`
- `SIGN_KEY_ALIAS`
- `SIGN_KEY_PASSWORD`
