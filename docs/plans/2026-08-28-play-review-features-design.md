# Play Review Features Design

Date: 2026-08-28  
Repo: Pixel-Tailor-CN/PixelMeter  
Source: Google Play user reviews after listing

## 1. Context

Pixel Meter already measures physical-interface traffic with `TrafficStats` (bytes per second), formats values in `SpeedFormatter`, and renders three surfaces:

- Main screen
- Notification / Live Update / status-bar Bitmap icon
- Compose Overlay (floating window)

Three Play reviews request display flexibility that the current surfaces only partially cover.

## 2. Review Mapping

| Review | User problem | Current behavior | Proposed feature |
|---|---|---|---|
| Polish: does not show bits per second | ISPs and many users think in `Mb/s`, not `MB/s` | Units are byte-only: Auto / B/s / KB/s / MB/s / GB/s (`key_speed_unit`) | Global **rate base**: Bytes or Bits |
| English: Overlay should offer Download / Upload / Total | Overlay is always two meters | Overlay always renders upload + download | Overlay **display mode** |
| English: up/down speed on the status bar | Status-bar chip is a single total | Bitmap / Live Update small icon always uses `totalSpeed` | Status-bar icon **content mode**, plus document Overlay-over-status-bar |

## 3. Goals

1. Let users choose bits/s without changing sampling or counter source.
2. Let the Overlay show one of: both directions, download only, upload only, or combined total.
3. Let the status-bar presentation show directional speed when the user wants it, within Android small-icon limits.
4. Keep existing users unchanged (defaults preserve current UI).
5. Keep `SpeedFormatter` as the single formatting source.

## 4. Non-goals

- Root / Shizuku / privileged status-bar injection.
- Changing how `TrafficStats` is sampled.
- Per-surface unit systems (notification bytes + Overlay bits).
- Decimal SI (1000) vs binary (1024) split in this iteration. Bits reuse the same 1024 scale after `bytes * 8`.
- Redesigning Live Update promotion rules.

## 5. Feature A — Bits per second

### 5.1 User flow

Settings → General → new list **Rate unit**:

- Bytes (`B/s`, `KB/s`, `MB/s`, `GB/s`) — default
- Bits (`b/s`, `Kb/s`, `Mb/s`, `Gb/s`)

Existing **Speed Unit** and **Minimum Display Unit** keep their Auto / lock / floor behavior. Labels follow the selected rate base.

### 5.2 Data

New DataStore key:

| Key | Type | Default | Meaning |
|---|---|---|---|
| `key_speed_rate_unit` | Int | 0 | 0 = bytes, 1 = bits |

Do not overload `key_speed_unit`. Existing 0–4 values stay byte-scale indices.

### 5.3 Formatting

`SpeedFormatter` gains an explicit `rateUnit` argument (0/1).

- Bits path: `bits = bytes * 8`
- Auto thresholds stay 1024 / 1000-style as today, applied to the bit value
- Locked units divide by the same 1024^n after the `* 8`
- Live Update compact text uses `b/s`, `K/s`, `M/s`, `G/s` in bits mode to stay short
- Full text uses `b/s`, `Kb/s`, `Mb/s`, `Gb/s`

Threshold sliders remain stored as **bytes/s**. Their summary strings go through `SpeedFormatter` so they follow the selected rate base.

### 5.4 Why 1024 after `* 8`

The app already treats `KB/s` as 1024 bytes/s. Using 1000 only for bits would make `1 MB/s` ≠ `8 Mb/s` and confuse comparisons. Document this in Settings summary: bits are `bytes × 8` with the same binary steps.

## 6. Feature B — Overlay display mode

### 6.1 Modes

| Value | Label | Overlay content |
|---|---|---|
| 0 | Upload and download | Current two-meter UI (default) |
| 1 | Upload only | One line with upload prefix + upload speed |
| 2 | Download only | One line with download prefix + download speed |
| 3 | Total speed | One line with combined `upload + download` |

The Play request listed three modes. Mode 0 is kept so current Overlay users do not lose the two-meter layout.

### 6.2 Data

| Key | Type | Default | Meaning |
|---|---|---|---|
| `key_overlay_display_mode` | Int | 0 | See table above |

### 6.3 UI rules

- Prefixes and “upload first” apply only when two meters are visible (mode 0).
- Horizontal spacing applies only in mode 0 + horizontal layout.
- Vertical alignment applies only in mode 0 + vertical layout.
- Single-meter modes use one `Text` inside the existing Surface.
- Low-traffic auto-hide still uses `totalSpeed`.

Settings placement: Overlay section, after enable / lock, before prefixes.

## 7. Feature C — Status-bar up/down

### 7.1 Constraint

The notification small icon is ~24 dp. Two full speeds with units are unreadable if copied from Overlay text.

Android 16 Live Update `setShortCriticalText()` is also a short single chip.

### 7.2 Status-bar icon content

Reuse notification `displayMode` for **notification drawer text** (already implemented).

Add a separate icon content key so drawer text and the tiny icon can differ:

| Key | Type | Default | Icon |
|---|---|---|---|
| `key_notification_icon_mode` | Int | 0 | 0 Total, 1 Upload, 2 Download, 3 Dual |

- 0–2: current Bitmap layout (value + unit) of the chosen speed.
- 3 Dual: two-line Bitmap, no unit on the icon:
  - line 1: `▲12K` or configured upload prefix + compact value
  - line 2: `▼3.4M` or configured download prefix + compact value
  - compact value = Auto unit letter only (`B`/`K`/`M`/`G`), bits use the same letters
- Live Update: Dual writes a compact string such as `↑12K ↓3M` into `setShortCriticalText()`. If the string is too long, fall back to total.

### 7.3 Existing alternative (document, do not change)

`key_overlay_show_on_status_bar` already lets the Overlay sit in the status-bar / cutout area with both meters. Settings copy should mention this as the readable two-speed status-bar option.

## 8. Compatibility

- Missing new keys → defaults above. Existing installs look unchanged.
- No migration of `key_speed_unit`.
- All locales need new strings: en, zh-rCN, pt, pt-rBR, ru.

## 9. Risks

- Bits vs decimal Mbps expectations (ISP 1000-based). Mitigate with Settings summary.
- Dual Bitmap readability on light/dark status bars. Must device-test on Pixel.
- Overlay file size: `OverlayWindow.kt` is already large; keep content branching inside `OverlayContent`.
- `SpeedFormatter` signature changes must update every call site (main, notification, Overlay, threshold summaries).

## 10. Validation

- Bytes mode matches current screenshots and unit lists.
- Bits: 1,048,576 B/s → `8.00 Mb/s` in locked Mb/s (or Auto equivalent).
- Overlay modes 0–3 update live while the service runs.
- Notification drawer text still follows `key_notification_display_mode`.
- Icon modes 0–3 and Live Update Dual on Android 16.
- Overlay dragged over the status bar still shows two speeds when mode is 0.
