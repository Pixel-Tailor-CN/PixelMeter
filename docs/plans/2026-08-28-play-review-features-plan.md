# Play Review Features Implementation Plan

Date: 2026-08-28  
Depends on: `2026-08-28-play-review-features-design.md`

## Order

Implement A → B → C. A establishes the shared Rate unit, formatter behavior, and threshold presentation used by later surfaces. B applies the new shared formatting to Overlay modes. C applies it to notification and status-bar rendering.

## Step 1 — DataStore and Repository

Files:

- `app/src/main/kotlin/vip/mystery0/pixel/meter/data/repository/DataStoreRepository.kt`
- `app/src/main/kotlin/vip/mystery0/pixel/meter/data/repository/NetworkRepository.kt`
- `app/src/main/kotlin/vip/mystery0/pixel/meter/ui/settings/SettingsViewModel.kt`
- `docs/architecture/preferences.md`

Add keys, defaults, read/write, and StateFlow:

- `key_speed_rate_unit` Int default 0
- `key_overlay_display_mode` Int default 0
- `key_notification_icon_mode` Int default 0

Normalize unsupported values when exposing/consuming them, without immediately rewriting DataStore:

- invalid `speedRateUnit` → 0 (Bytes)
- invalid `overlayDisplayMode` → 0 (Both)
- invalid `notificationIconMode` → 0 (Total)
- use matching default branches in Settings labels, Overlay rendering, and notification rendering

Expected: existing users with no keys keep current UI.

Runtime propagation:

- `speedRateUnit`: expose through `NetworkRepository` and `SettingsViewModel`; collect in the main UI, General Settings, Overlay, Overlay Settings, and Notification Settings as needed for rendering and summaries
- `overlayDisplayMode`: expose through `NetworkRepository` and `SettingsViewModel`; collect in Overlay and Overlay Settings for immediate recomposition
- `notificationIconMode`: expose through `NetworkRepository` and `SettingsViewModel`; collect in Notification Settings, while `NetworkMonitorService` reads the current StateFlow value during each speed sample
- Notification-related changes intentionally take effect on the next speed sample, within one configured sampling interval (1–3 seconds); do not add separate collectors solely for immediate notification refresh

## Step 2 — SpeedFormatter

File: `format/SpeedFormatter.kt`

- Add `rateUnit: Int = 0` to `formatSpeedText` and `formatSpeedLine`
- Change `formatSpeedTextForLiveUpdate` to use automatic scaling with `rateUnit`; do not pass or apply `speedUnit` / `minSpeedUnit`
- Clamp formatter input to zero or above
- Correct byte Auto unit switching to use strict 1024 boundaries at every level; remove the current mixed 1024-divisor / 1000-switch behavior
- When `rateUnit == 1`, convert the normalized byte value to `Double`, multiply by `8.0`, and format it with decimal SI divisors (1000^n) and bit labels; do not use `Long * 8`
- Interpret `minSpeedUnit` through `rateUnit`: byte mode uses the existing 1024-based KB/MB/GB floors; bit mode uses 1000/1,000,000/1,000,000,000 b/s floors for kb/Mb/Gb
- Preserve existing precision policies separately for locked units, Auto full text, and Live Update; apply the same policy after bit conversion rather than introducing a new unified precision rule

Update every formatting and unit-label call site with the appropriate `speedRateUnit` StateFlow value:

- `MainActivity.kt`: Total, Download, and Upload speed text
- `ui/overlay/OverlayWindow.kt`: Upload, Download, and the new Total mode
- `service/NotificationHelper.kt`: notification drawer Upload/Download text, Bitmap icon value/unit, and Live Update short text
- `ui/settings/OverlaySettingsSection.kt`: threshold summary, slider value text, and custom threshold input display/conversion
- `ui/settings/NotificationSettingsSection.kt`: threshold summary, slider value text, and custom threshold input display/conversion
- `ui/settings/GeneralSettingsSection.kt`: dynamic Speed Unit and Minimum Display Unit labels

After updating, search every `SpeedFormatter.` call and every hard-coded speed-unit option list. No display surface or threshold control may rely on the default byte Rate unit unintentionally.

Expected: locked byte units and ordinary byte outputs remain unchanged, while Auto output intentionally changes at the existing mixed-boundary ranges (1000–1023 KB/s and 1000–1023 MB/s). Negative inputs display as zero and large inputs do not overflow. A final repository search finds no stale Formatter signatures, hard-coded byte-only unit lists, or omitted Rate unit arguments.

## Step 3 — General settings UI (Feature A)

Files:

- `ui/settings/GeneralSettingsSection.kt`
- `ui/settings/OverlaySettingsSection.kt`
- `ui/settings/NotificationSettingsSection.kt`
- all `values*/strings.xml` resources

Add Rate unit `ListPreference` above Speed Unit. When bits are selected, Speed Unit / Min Unit option labels switch to `b/s`, `kb/s`, `Mb/s`, `Gb/s`. Preserve the current `speedUnit` and `minSpeedUnit` indices when switching Rate unit; do not reset or rewrite them.

Strings to add (English source):

- `settings_speed_rate_unit_title` = Rate unit
- `settings_speed_rate_unit_desc` = Byte units keep the existing 1024 scale. Bit units use decimal SI (1 Mb/s = 1,000,000 bits/s).
- `settings_speed_rate_unit_bytes` = Bytes (B/s)
- `settings_speed_rate_unit_bits` = Bits (b/s)

Also update zh-rCN, pt, pt-rBR, ru. Do not add a partial `values-pl`; Polish localization remains a separate full-translation task or community contribution.

Update both Overlay and Notification threshold controls:

- keep persisted thresholds in bytes/s
- format slider summaries and value text with `speedRateUnit`
- byte mode custom input: dot-decimal `KB/s`, converted with `value * 1024`
- bit mode custom input: dot-decimal `kb/s`, converted with `value * 125`
- derive the displayed input value from the stored bytes/s value, so changing Rate unit does not rewrite the threshold
- use `.` as the only decimal separator for formatting and parsing in every Locale; display at most 3 fractional digits and strip trailing zeros, while accepting dot-decimal input with more precision
- convert with nearest-integer rounding: `roundToLong(value * 1024)` in byte mode and `roundToLong(value * 125)` in bit mode
- reject invalid, negative, non-finite, or overflowing input without changing the stored value; keep 0 as disabled

## Step 4 — Overlay display mode (Feature B)

Files:

- `ui/overlay/OverlayWindow.kt` (`OverlayContent`)
- `ui/settings/OverlaySettingsSection.kt`
- `docs/ui/overlay.md`

Collect `overlayDisplayMode` in `OverlayWindow` and pass it into `OverlayContent`.

Branch:

- mode 0: current two-text layout
- mode 1/2/3: single `Text`

Select the speed used by low-traffic auto-hide from the display mode:

- modes 0 and 3: `totalSpeed`
- mode 1: `uploadSpeed`
- mode 2: `downloadSpeed`

Keep inapplicable settings visible but disabled according to the selected mode:

| Setting | Both | Upload only | Download only | Total |
|---|---:|---:|---:|---:|
| Upload prefix | Enabled | Enabled | Disabled | Disabled |
| Download prefix | Enabled | Disabled | Enabled | Disabled |
| Upload first | Enabled | Disabled | Disabled | Disabled |
| Layout direction | Enabled | Disabled | Disabled | Disabled |
| Meter spacing | Enabled only for horizontal layout | Disabled | Disabled | Disabled |
| Vertical alignment | Enabled only for vertical layout | Disabled | Disabled | Disabled |

Single-direction modes render their corresponding configured prefix. Total mode renders no prefix. Disabled preferences retain their stored values for use when the user switches back to a compatible mode.

Strings:

- `settings_overlay_display_mode`
- `settings_display_mode_both` = Upload and download (new shared label for Overlay mode 0 and notification drawer mode 0)
- `settings_display_mode_total` (reuse)
- `settings_display_mode_upload` / `settings_display_mode_download` (reuse)

Do not add an Overlay-specific `settings_overlay_display_mode_both`.

## Step 5 — Status-bar icon mode (Feature C)

Files:

- `service/NotificationHelper.kt`
- `service/NetworkMonitorService.kt` (pass new setting)
- `ui/settings/NotificationSettingsSection.kt`
- `docs/ui/notification.md`

`createRenderState`:

- Choose the single icon speed from icon mode: Total, Upload, or Download.
- Use that same selected speed for notification low-traffic threshold evaluation: mode 0 `totalSpeed`, mode 1 `uploadSpeed`, mode 2 `downloadSpeed`. Apply it to both static low-traffic behavior and Live Update promotion/demotion; do not use drawer display mode for this decision.
- Format every Bitmap icon and Live Update short text with automatic scaling; do not pass `speedUnit` or `minSpeedUnit` into icon formatting.
- Bitmap uses the existing single-value value/unit layout with the selected global Rate unit; Total, Upload, and Download do not add direction markers.
- Live Update passes compact automatic text for the selected single speed to `setShortCriticalText()` without a direction marker.
- Include `rateUnit`, `notificationIconMode`, and all rendered icon/status text in the notification fingerprint so configuration changes are applied on the next sampled update.
- Do not render configurable notification drawer prefixes in the Bitmap icon or Live Update short text.

Before adding the icon list, correct the existing notification drawer setting label without changing behavior:

- `key_notification_display_mode = 0` continues to render upload and download together
- rename its visible option from **Total** to **Upload and download**
- values 1/2 remain Upload only / Download only
- do not migrate the stored key and do not change mode 0 to combined total speed

Add a separate **Status bar icon** list under the drawer Display Content setting with Total / Upload / Download. Use the same three options whether Live Update is enabled or disabled. Default to Total and show no direction marker for any option.

Strings:

- `settings_notification_icon_mode` (new title)
- reuse `settings_display_mode_total`, `settings_display_mode_upload`, and `settings_display_mode_download`
- use the new shared `settings_display_mode_both` for notification drawer mode 0

Copy note: for a larger two-speed display near the status bar, enable Overlay and “Show Over Status Bar”. Update `settings_overlay_show_on_status_bar_desc` in all supported locales to state that this is a floating Overlay requiring Display over other apps permission, not a native SystemUI status-bar indicator, and that available space may be affected by the camera cutout and system icons.

## Step 6 — Docs and changelog

- `docs/architecture/preferences.md`
- `docs/ui/overlay.md`
- `docs/ui/notification.md`
- `README.md` / `README_CN.md` feature bullets
- `CHANGELOG.md` Added section

These documents live in `docs/plans/` and should be followed during implementation.

## Step 7 — Build and focused validation

Do not add a unit-test dependency for this change. Keep validation dependency-free and cover the formatter boundaries and mode-selection cases through the focused checks below.

```bash
./gradlew :app:assembleDebug
./gradlew lint
```

Device checks on a Pixel:

1. Rate unit Bytes vs Bits on home, Overlay, notification icon, Live Update, threshold sliders, and custom threshold inputs; verify switching Rate unit preserves the stored threshold and all status-bar icon modes remain Auto-scaled regardless of Speed Unit / Minimum Unit
2. Overlay modes 0–3 while dragging / locked, including direction-specific auto-hide in upload-only and download-only modes
3. Notification drawer modes (Upload and download / Upload / Download) vs icon modes (Total / Upload / Download), including Rate unit and icon-mode changes appearing by the next sampling tick and direction-specific low-traffic threshold behavior
4. Total / Upload / Download icon contrast and readability on light and dark wallpaper
5. Floating Overlay in the status-bar area remains usable around the punch-hole/camera cutout, privacy indicators, and system icons, with wording that does not imply native SystemUI integration
6. New strings in all five locales

## Out of scope unless requested

- Binary (1024) bit scale / IEC bit units
- Polish localization (`values-pl`)
- Separate Overlay rate unit
- Two notification icons
- Status-bar NotificationListener / SystemUI hooks
