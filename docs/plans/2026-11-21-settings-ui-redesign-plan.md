# Settings UI & Navigation Transition Redesign Implementation Plan

> **For agentic workers:** Follow task-by-task execution and verify with `:app:assembleDebug` and `lint`. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign Pixel Meter's Settings directory, five subpages, and single-pane navigation animations to match `PixelText` and `PixelTelo`'s Material 3 Expressive segmented-card style and Activity-like page transitions with Predictive Back.

**Architecture:** Keep `SettingsActivity` with zero new navigation dependencies by introducing `SettingsNavigationTransitions` and `SettingsNavigationContent` (`SeekableTransitionState` + `PredictiveBackHandler`). Replace `me.zhanghai.compose.preference` with custom Material 3 segmented-card components (`SettingsSection`, `SettingsGroup`, `SettingsItemRow`, `SettingsSwitchRow`, `SettingsChoiceRow`, `SettingsSliderRow`, `SettingsTextFieldRow`, `ColorPreference`). Preserve 840dp two-pane layout and all existing DataStore keys and ViewModel logic.

**Tech Stack:** Kotlin 2.4.10, Jetpack Compose Material 3, AndroidX Activity Compose (`PredictiveBackHandler`), `skydoves-colorpicker`.

**Spec:** `docs/plans/2026-11-21-settings-ui-redesign-design.md`

## Global Constraints
- Min SDK 31, Compile/Target SDK 37, JVM Target 21; `ExperimentalMaterial3Api` globally opted in.
- Use English for code comments, KDoc, documentation, and commit messages.
- Keep all DataStore keys, defaults, ViewModel state/actions, and permissions unchanged.
- Add all new translatable strings to every locale (`values`, `values-zh-rCN`, `values-pt`, `values-pt-rBR`, `values-ru`).
- Validate with `.\gradlew.bat :app:assembleDebug` and `.\gradlew.bat lint`.

## Review Focus
- Dynamic conditional rows (e.g., Fixed theme color/AMOLED, Notification enabled/Live Update/API 36+, Overlay enabled) must recompute first/last `24.dp`/`4.dp` corners without visual gaps or broken radii.
- Fixed Light, Fixed Dark, and AMOLED Black themes must keep clear contrast between `background` and `surfaceContainerLow` cards.
- Exiting pages during forward/back transitions and predictive back cancellation must block touches, hide duplicate dialogs (`LocalNavigationActive`), and smoothly rewind on cancel.
- RTL layout direction must invert horizontal slide directions cleanly.
- All 5 locale `strings.xml` files must have matching format placeholders and valid XML escaping.

---

### Task 1: Dependencies, Manifest, Theme Surfaces & Drawable Asset
**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/vip/mystery0/pixel/meter/ui/theme/Theme.kt`
- Create: `app/src/main/res/drawable/ic_pixel_tailor.xml`

- [ ] Remove `zhanghai-preference` from `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- [ ] Add `android:enableOnBackInvokedCallback="true"` to `<application>` in `app/src/main/AndroidManifest.xml`.
- [ ] Set explicit `surfaceContainerLow` in `FixedLightColorScheme` (`#F0F5F4`), `FixedDarkColorScheme` (`#191E1E`), and `ColorScheme.toAmoled()` (`#121212`) in `Theme.kt`.
- [ ] Add `ic_pixel_tailor.xml` vector drawable to `app/src/main/res/drawable/`.

### Task 2: Localization Strings (All 5 Locales)
**Files:**
- Modify: `app/src/main/res/values/strings.xml`, `values-zh-rCN/strings.xml`, `values-pt/strings.xml`, `values-pt-rBR/strings.xml`, `values-ru/strings.xml`

- [ ] Add subsection headers, dynamic directory status summaries, and external-link accessibility text across all 5 locales with matching format placeholders.

### Task 3: Navigation Transitions & Predictive Back Host
**Files:**
- Create: `app/src/main/kotlin/vip/mystery0/pixel/meter/ui/settings/SettingsNavigationTransitions.kt`
- Create: `app/src/main/kotlin/vip/mystery0/pixel/meter/ui/settings/SettingsNavigationContent.kt`

- [ ] Implement RTL-aware Activity-like enter/exit/popEnter/popExit transitions matching `PixelText` / `PixelTelo` timing and scale curves.
- [ ] Implement `LocalNavigationActive` and `SettingsNavigationContent` using `SeekableTransitionState` + `PredictiveBackHandler`, z-index ordering, cancel rewind, and inactive pointer/semantics blocking.

### Task 4: Custom Segmented-Card Settings Components
**Files:**
- Create: `app/src/main/kotlin/vip/mystery0/pixel/meter/ui/settings/SettingsComponents.kt`
- Modify: `app/src/main/kotlin/vip/mystery0/pixel/meter/ui/settings/ColorPreference.kt`

- [ ] Implement `SettingsRowItem`, `SettingsRowTrailing`, `SettingsSection`, `SettingsGroup`, `SettingsRowContainer`, `SettingsItemRow`, `SettingsSwitchRow`, `SettingsChoiceRow`, `SettingsSliderRow`, and `SettingsTextFieldRow`.
- [ ] Refactor `ColorPreference` to build on `SettingsItemRow` with a bordered circular swatch trailing indicator and `LocalNavigationActive` dialog guard.

### Task 5: Settings Directory, Subpages & Activity Host
**Files:**
- Modify: `SettingsPage.kt`, `MainSettingsPage.kt`, `GeneralSettingsSection.kt`, `NotificationSettingsSection.kt`, `OverlaySettingsSection.kt`, `BackgroundSettingsSection.kt`, `AboutSettingsSection.kt`, `SettingsActivity.kt`

- [ ] Update `SettingsPage` enum with rounded icons and directory group indices.
- [ ] Rebuild `MainSettingsPage` with grouped segmented cards, tonal circular category icons, dynamic status summaries, and two-pane active state (`secondaryContainer` + `Check`).
- [ ] Refactor all five subpages into logical `SettingsSection` + `SettingsGroup` cards with icons and custom controls.
- [ ] Update `SettingsActivity` to use `SettingsNavigationContent` in single-pane mode (whole-page `Scaffold` + `TopAppBar` + `SaveableStateHolder`) and `320.dp` sidebar two-pane mode at `>= 840.dp`.

### Task 6: Documentation & Verification
**Files:**
- Modify: `docs/ui/design-system.md`

- [ ] Update `docs/ui/design-system.md` to document the segmented-card settings UI, custom controls, and transition system.
- [ ] Run `.\gradlew.bat :app:assembleDebug` and `.\gradlew.bat lint` and verify zero errors/regressions.
