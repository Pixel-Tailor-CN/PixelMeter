# Settings UI & Navigation Transition Redesign

Date: 2026-11-21
Status: Approved

## 1. Intent & Goals

Redesign Pixel Meter's Settings screen and all five subpages (`General`, `Notification`, `Overlay`, `Background`, `About`) to match the Material 3 Expressive segmented-card visual language and whole-page Activity-like navigation transitions established in `PixelText` and `PixelTelo`.

### Goals
- Replace the flat `me.zhanghai.compose.preference` layout with custom Material 3 segmented-card settings components (`surfaceContainerLow`, dynamic `24.dp` / `4.dp` corners, `2.dp` row gap, `primary` section headers, leading icons, and consistent trailing affordances).
- Upgrade the Settings directory page with grouped categories, tonal circular category icons (matching `PixelText`), and live status summaries.
- Organize each subpage into logical subsections so long pages (especially `General`, `Notification`, and `Overlay`) are easy to scan.
- Replace the basic `96.dp` content-only slide transition with whole-page Activity-like transitions (including `TopAppBar`), RTL direction support, z-index layering, and Android 14+ Predictive Back gesture preview.
- Preserve responsive two-pane support at widths `>= 840.dp` with `SaveableStateHolder` scroll-state persistence.
- Preserve all existing DataStore keys, defaults, ViewModel logic, permissions, and feature behavior.

### Non-Goals
- Merging `SettingsActivity` into `MainActivity` or adding `androidx.navigation:navigation-compose`.
- Changing any monitoring logic, DataStore schema, or adding a user-configurable transition-style preference.
- Redesigning `MainActivity` or `OnboardingScreen`.

## 2. Approaches Considered

1. **Keep `SettingsActivity` + zero-dependency `SettingsNavigationContent` (Chosen)**
   - Uses `SeekableTransitionState` + `PredictiveBackHandler` modeled on `PixelTelo`'s `NavigationContent.kt` and `NavigationTransitions.kt`, combined with `PixelText`'s `840.dp` two-pane layout and segmented-card components.
   - Trade-off: `MainActivity` → `SettingsActivity` remains a standard Android Activity transition, while Settings directory ↔ subpages gets the exact Compose Activity-like transition and predictive back with zero new library dependencies.
2. **Single-Activity consolidation with `navigation-compose` (Not Selected)**
   - Consolidates `SettingsActivity` into `MainActivity` under a `NavHost`, unifying `MainActivity` ↔ `Settings` ↔ subpage animations.
   - Trade-off: Requires adding `navigation-compose`, refactoring `MainActivity` and `AndroidManifest.xml`, and expanding scope beyond the settings module.

## 3. Navigation & Transition Design

### 3.1 Whole-Page Transitions (`SettingsNavigationTransitions.kt`)
Match `PixelText` / `PixelTelo` Activity-like transition specs, adapted for RTL (`direction = +1` LTR, `-1` RTL):
- **Enter (forward)**: `slideInHorizontally(fullWidth * dir -> 0, 300ms, FastOutSlowIn)` + `fadeIn(90ms)` + `scaleIn(0.96f -> 1f, 300ms, FastOutSlowIn)`
- **Exit (forward)**: `slideOutHorizontally(0 -> -fullWidth / 4 * dir, 260ms, FastOutSlowIn)` + `fadeOut(90ms, delay 120ms)` + `scaleOut(1f -> 0.92f, 260ms, FastOutSlowIn)`
- **PopEnter (back)**: `slideInHorizontally(-fullWidth / 4 * dir -> 0, 260ms, FastOutSlowIn)` + `fadeIn(90ms)` + `scaleIn(0.92f -> 1f, 260ms, FastOutSlowIn)`
- **PopExit (back)**: `slideOutHorizontally(0 -> fullWidth * dir, 300ms, FastOutSlowIn)` + `fadeOut(90ms, delay 210ms)` + `scaleOut(1f -> 0.90f, 300ms, FastOutSlowIn)`
- Layering: `targetContentZIndex = if (forward) 1f else -1f`.

### 3.2 `SettingsNavigationContent` & Predictive Back
- Uses `SeekableTransitionState` + `rememberTransition` + `PredictiveBackHandler` (modeled on `PixelTelo`'s `NavigationContent.kt`):
  - Gestural back from a subpage (`SettingsPage`) previews the directory (`null`) in real time; cancelling smoothly rewinds back to the subpage without losing state.
  - Inactive/exiting pages have pointer input consumed and semantics cleared via `LocalNavigationActive` to prevent double taps or duplicate dialogs during transitions.
- Each displayed page renders its own `Scaffold` + `TopAppBar(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background)` with `enterAlwaysScrollBehavior`, so the app bar transitions seamlessly with the page.
- Add `android:enableOnBackInvokedCallback="true"` in `AndroidManifest.xml` to enable system predictive back animations.
- Set window background to `MaterialTheme.colorScheme.background` in `SettingsActivity` via `SideEffect` to avoid flash artifacts during transitions.

### 3.3 Responsive Two-Pane (`>= 840.dp`)
- Modeled on `PixelText`'s `SettingsPage.kt`:
  - `BoxWithConstraints` checks `maxWidth >= 840.dp`.
  - Left pane (`320.dp`): grouped category list with `secondaryContainer` active highlight and trailing `Icons.Rounded.Check`.
  - `VerticalDivider` separator.
  - Right pane (`weight(1f)`): `headlineSmall` category heading followed by grouped setting sections.
  - `rememberSaveableStateHolder` preserves independent scroll state for the directory and each subpage.

## 4. Visual Style & Components

### 4.1 Theme Surface Alignment (`Theme.kt`)
- Explicitly define `surfaceContainerLow` in `FixedLightColorScheme` (`#F0F5F4`), `FixedDarkColorScheme` (`#191E1E`), and `ColorScheme.toAmoled()` (`#121212`) so grouped setting cards have crisp, hue-consistent contrast in Dynamic, Fixed, and AMOLED Black themes.

### 4.2 Custom Settings Components (`SettingsComponents.kt`)
Remove `me.zhanghai.compose.preference` dependency and implement dedicated components:
- `SettingsSection`: `16.dp` horizontal padding, `titleSmall` `primary` header with `semantics { heading() }`, `8.dp` gap to content.
- `SettingsGroup`: `2.dp` vertical spacing between dynamically filtered `SettingsRowItem`s; `24.dp` outer corners on first/last item, `4.dp` inner corners; `surfaceContainerLow` background (or `secondaryContainer` when active).
- `SettingsItemRow`: `32.dp` leading icon slot (`24.dp` icon or custom composable), `bodyLarge` title, optional `bodyMedium` (`onSurfaceVariant`) summary or `summaryContent`, trailing chevron (`KeyboardArrowRight`), external link (`OpenInNew` with localized content description), custom trailing content, or none. Min height `72.dp` with summary, `56.dp` without.
- `SettingsSwitchRow`: Row-level `toggleable(role = Role.Switch)` with `Switch(onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {})`.
- `SettingsChoiceRow`: Opens Material 3 `AlertDialog` with `selectableGroup` `RadioButton` rows.
- `SettingsSliderRow` *(Proposal)*: Inline slider inside the rounded card row displaying leading icon, title, formatted current value badge, optional summary, and Material 3 `Slider`. Keeping sliders inline allows real-time visual feedback while tuning the floating overlay or status bar icon without an AlertDialog obscuring the screen.
- `SettingsTextFieldRow`: Opens Material 3 `AlertDialog` with `OutlinedTextField` (appropriately typed keyboard for integers/decimals/text) and Confirm/Cancel actions.
- `SettingsColorRow` (`ColorPreference.kt`): Uses `SettingsItemRow` with a `28.dp` circular color preview (with subtle `1.dp` outline border for light/transparent colors) as `trailingContent`, opening the existing `skydoves` HSV color picker dialog.

## 5. Information Architecture

### 5.1 Settings Directory Groups
1. **Group 1 (`primaryContainer` / `onPrimaryContainer` circular icon badge)**
   - **General** (`Icons.Rounded.Tune`): dynamic summary of sampling interval & rate/unit (e.g., `1500ms · B/s (Auto)`)
   - **Notification** (`Icons.Rounded.Notifications`): dynamic summary (`Disabled`, `Enabled`, or `Enabled · Live Update`)
   - **Overlay** (`Icons.Rounded.PictureInPictureAlt`): dynamic summary (`Disabled`, `Enabled`, or `Enabled · Locked`)
2. **Group 2 (`secondaryContainer` / `onSecondaryContainer` circular icon badge)**
   - **Background** (`Icons.Rounded.BatterySaver`): dynamic summary of battery optimization status
   - **About** (`Icons.Rounded.Info`): dynamic summary showing `BuildConfig.VERSION_NAME`

### 5.2 Subpage Subsections
- **General**:
  1. *Speed & Units*: Sampling Interval (slider), Rate Unit (choice), Speed Unit (choice), Minimum Display Unit (choice), Hide `/s` Suffix (switch)
  2. *Appearance*: App Theme (choice), [Fixed] Theme Color (color), [Fixed] AMOLED Black (switch)
  3. *Startup & Permissions*: Run Setup Wizard Again (item), Auto-start Monitoring Service (switch), Floating Window Permission (external), Notification Permission (external)
- **Notification**:
  1. *Basic*: Show on Notification (switch), [enabled] Enable Live Update (switch), [enabled & Live Update & API 36+] Hide `/s` in Live Update (switch)
  2. *Content & Order* (when enabled): Display Content (choice), Upload Prefix (text), Download Prefix (text), Prioritize Upload Speed (switch)
  3. *Status Bar Icon* (when enabled): Status Bar Icon (choice), Icon Number Size (slider), Icon Unit Size (slider), Custom Notification Color (switch), Notification Color (color)
  4. *Low Traffic* (when enabled): Notification Threshold (slider), Custom Threshold (text), Action Below Threshold (choice)
- **Overlay**:
  1. *Basic*: Enable Floating Window (switch)
  2. *Behavior & Position* (when enabled): Lock Floating Window (switch), Show Over Status Bar (switch), X Position (text), Y Position (text), Hide in Landscape (switch), Hide in Immersive Mode (switch)
  3. *Auto-Hide* (when enabled): Auto-hide Threshold (slider), Custom Auto-hide Threshold (text)
  4. *Appearance* (when enabled): Use Default Colors (switch), Hide Background (switch), Background Color (color), Text Color (color), Corner Radius (slider), Background Padding (slider), Text Size (slider)
  5. *Content & Layout* (when enabled): Overlay Display Content (choice), Upload Prefix (text), Download Prefix (text), Prioritize Upload Speed (switch), Layout Direction (choice), Meter Spacing (slider), Text Alignment (choice)
- **Background**:
  1. *Background Execution*: Ignore Battery Optimizations (external), Hide from Recents (switch), Background Survival Guide (external)
- **About**:
  1. *About*: App Version (info, no trailing chevron), GitHub (external), Pixel Tailor (`ic_pixel_tailor` vector icon + external), Telegram Channel (external)

## 6. Resources, Dependencies & Documentation

- **Drawable**: Copy `ic_pixel_tailor.xml` vector resource for the About page.
- **Strings**: Add new section titles, dynamic category summaries, and accessibility labels across all 5 locales (`values`, `values-zh-rCN`, `values-pt`, `values-pt-rBR`, `values-ru`).
- **Dependencies**: Remove unused `zhanghai-preference` from `app/build.gradle.kts` and `gradle/libs.versions.toml`.
- **Docs**: Update `docs/ui/design-system.md` to reflect the redesigned settings UI and navigation transitions.
- **Validation**: Run `.\gradlew.bat :app:assembleDebug` and `.\gradlew.bat lint`, plus note real-device checks for Predictive Back, AMOLED contrast, and live overlay slider tuning.
