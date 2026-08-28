# Play Review Features Implementation Plan

Date: 2026-08-28  
Depends on: `2026-08-28-play-review-features-design.md`

## Order

Implement A → B → C. A is isolated in the formatter. B is Overlay-only. C touches notification rendering.

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

Expected: existing users with no keys keep current UI.

## Step 2 — SpeedFormatter

File: `format/SpeedFormatter.kt`

- Add `rateUnit: Int = 0` to `formatSpeedText`, `formatSpeedTextForLiveUpdate`, `formatSpeedLine`
- When `rateUnit == 1`, format `bytes * 8` with bit labels
- Add `formatSpeedCompact(bytes, speedUnit, minSpeedUnit, rateUnit): String` for Dual icon / Live Update (`12K`, `3.4M`)

Update every call site to pass `repository.speedRateUnit`:

- `MainActivity.kt` (or whatever renders home speeds)
- `NotificationHelper.kt`
- `OverlayWindow.kt`
- Overlay / notification threshold summaries

Expected: bytes path is bit-identical to current output.

## Step 3 — General settings UI (Feature A)

File: `ui/settings/GeneralSettingsSection.kt`  
Resources: all `values*/strings.xml`

Add Rate unit `ListPreference` above Speed Unit. When bits are selected, Speed Unit / Min Unit option labels switch to `b/s`, `Kb/s`, `Mb/s`, `Gb/s`.

Strings to add (English source):

- `settings_speed_rate_unit_title` = Rate unit
- `settings_speed_rate_unit_desc` = Bits are bytes × 8. Scale stays 1024.
- `settings_speed_rate_unit_bytes` = Bytes (B/s)
- `settings_speed_rate_unit_bits` = Bits (b/s)

Also update zh-rCN, pt, pt-rBR, ru.

## Step 4 — Overlay display mode (Feature B)

Files:

- `ui/overlay/OverlayWindow.kt` (`OverlayContent`)
- `ui/settings/OverlaySettingsSection.kt`
- `docs/ui/overlay.md`

Collect `overlayDisplayMode` in `OverlayWindow` and pass it into `OverlayContent`.

Branch:

- mode 0: current two-text layout
- mode 1/2/3: single `Text`

Disable prefix-order / spacing / alignment preferences when they do not apply, same pattern as direction-gated controls today.

Strings:

- `settings_overlay_display_mode`
- `settings_overlay_display_mode_both`
- `settings_display_mode_total` (reuse if identical)
- `settings_display_mode_upload` / `settings_display_mode_download` (reuse)

## Step 5 — Status-bar icon mode (Feature C)

Files:

- `service/NotificationHelper.kt`
- `service/NetworkMonitorService.kt` (pass new setting)
- `ui/settings/NotificationSettingsSection.kt`
- `docs/ui/notification.md`

`createRenderState`:

- Choose icon bytes from icon mode (total / up / down)
- Dual: two compact lines + include them in fingerprint
- Dual Live Update: compact `↑… ↓…` or total fallback

`buildNotificationFromState` Dual Bitmap:

- Slightly smaller type size
- `cy` at ~0.42 and ~0.82 of icon height
- Prefix characters from notification up/down text, trimmed if needed

Settings: new list **Status bar icon** under existing Display Content.

Copy note: for a readable two-speed status bar, enable Overlay and “Show Over Status Bar”.

## Step 6 — Docs and changelog

- `docs/architecture/preferences.md`
- `docs/ui/overlay.md`
- `docs/ui/notification.md`
- `README.md` / `README_CN.md` feature bullets
- `CHANGELOG.md` Added section

These documents live in `docs/plans/` and should be followed during implementation.

## Step 7 — Build

```bash
./gradlew :app:assembleDebug
./gradlew lint
```

Device checks on a Pixel:

1. Rate unit Bytes vs Bits on home, Overlay, notification icon, Live Update
2. Overlay modes 0–3 while dragging / locked
3. Notification drawer vs icon mode combinations
4. Dual icon contrast on light and dark wallpaper
5. Overlay over status bar / punch-hole still usable
6. New strings in all five locales

## Out of scope unless requested

- Decimal (1000) bit scale
- Separate Overlay rate unit
- Two notification icons
- Status-bar NotificationListener / SystemUI hooks
