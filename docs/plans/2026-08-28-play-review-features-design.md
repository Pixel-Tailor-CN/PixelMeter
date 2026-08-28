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
4. Preserve existing settings and defaults. Existing users keep their configured behavior except for the explicitly approved decimal-separator and Byte Auto boundary corrections.
5. Keep `SpeedFormatter` as the single formatting source.

## 4. Non-goals

- Root / Shizuku / privileged status-bar injection.
- Changing how `TrafficStats` is sampled.
- Per-surface unit systems (notification bytes + Overlay bits).
- Changing the existing byte-unit scale. Byte units keep the current 1024-based behavior; bit units use decimal SI scaling.
- Redesigning Live Update promotion eligibility or platform integration beyond selecting the threshold speed from the configured icon mode.

## 5. Feature A — Bits per second

### 5.1 User flow

Settings → General → new list **Rate unit**:

- Bytes (`B/s`, `KB/s`, `MB/s`, `GB/s`) — default
- Bits (`b/s`, `kb/s`, `Mb/s`, `Gb/s`)

Existing **Speed Unit** and **Minimum Display Unit** keep their Auto / lock / floor behavior. Labels follow the selected rate base.

### 5.2 Data

New DataStore key:

| Key | Type | Default | Meaning |
|---|---|---|---|
| `key_speed_rate_unit` | Int | 0 | 0 = bytes, 1 = bits |

Do not overload or migrate `key_speed_unit`. Its existing 0–4 values remain scale-level indices whose labels and divisors are interpreted through the selected Rate unit:

| Value | Byte mode | Bit mode |
|---|---|---|
| 0 | Auto | Auto |
| 1 | B/s | b/s |
| 2 | KB/s | kb/s |
| 3 | MB/s | Mb/s |
| 4 | GB/s | Gb/s |

Switching Rate unit preserves the selected index. For example, locked MB/s becomes locked Mb/s in bit mode and returns to MB/s when switching back.

### 5.3 Formatting

`SpeedFormatter` gains an explicit `rateUnit` argument (0/1).

- Normalize formatter input with `bytes.coerceAtLeast(0)`
- Bits path: convert the normalized value to `Double`, then calculate `bits = bytes.toDouble() * 8.0`; do not multiply a `Long` by 8
- Byte Auto uses strict binary boundaries: 1024 B/s, 1024 KB/s, and 1024 MB/s. This intentionally fixes the current mixed behavior that divides by 1024 but switches from KB/MB at 1000.
- Bit Auto uses strict decimal SI boundaries and divisors: 1000, 1,000,000, and 1,000,000,000
- Locked bit units divide by the corresponding decimal SI divisor after the `* 8`
- Minimum Display Unit keeps the existing stored indices (1/2/3) but interprets them through the selected Rate unit
  - byte mode floors at 1024 B/s, 1,048,576 B/s, and 1,073,741,824 B/s for KB/MB/GB
  - bit mode floors at 1,000 b/s, 1,000,000 b/s, and 1,000,000,000 b/s for kb/Mb/Gb
  - for example, with minimum `kb/s`, 100 B/s (800 b/s) displays `0kb/s`, while 125 B/s (1000 b/s) displays `1kb/s`
- Live Update compact text uses `b/s`, `k/s`, `M/s`, `G/s` in bits mode to stay short
- Full text uses `b/s`, `kb/s`, `Mb/s`, `Gb/s`
- Preserve the existing per-context precision policy after conversion: locked units use `formatFixedValue()` (2 decimals below 10, 1 below 100, otherwise integer); Auto full text and Live Update retain their current, more compact precision rules. Bit mode reuses the same precision policy as byte mode.

Threshold values remain stored as **bytes/s**, but every user-facing threshold value follows the selected Rate unit:

- Slider summaries and value text go through `SpeedFormatter`.
- In byte mode, custom threshold fields display and accept `KB/s`; convert input with `value * 1024`.
- In bit mode, custom threshold fields display and accept `kb/s`; convert input with `value * 1000 / 8` (`value * 125`).
- Switching Rate unit converts only the displayed field value and label; it does not change the stored threshold or its effective traffic level.
- Custom fields support decimal values so existing thresholds can round-trip across Rate unit changes (for example, `1 KB/s = 8.192 kb/s`).
- Use `.` as the only decimal separator for all speed text, compact status text, and custom threshold fields, independent of system Locale. Custom input accepts only `.` for decimals.
- Format custom-field values with at most 3 fractional digits and remove trailing zeros. This display rounding never rewrites DataStore by itself. Parsing may accept more than 3 fractional digits.
- Convert accepted input to stored bytes/s with nearest-integer rounding (`roundToLong()`): byte mode rounds `value * 1024`; bit mode rounds `value * 125`.
- Reject negative, non-finite, unparsable, or overflowing input without changing the stored value. Zero remains the disabled threshold.

### 5.4 Unit scale

Byte mode keeps the app's existing binary scale (`1 KB/s = 1024 B/s`) for compatibility. Bit mode follows common network-rate and ISP conventions and uses decimal SI scaling (`1 Mb/s = 1,000,000 b/s`). The Settings summary must make this distinction explicit: “Byte units keep the existing 1024 scale. Bit units use decimal SI (1 Mb/s = 1,000,000 bits/s).”

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

- Mode 0 shows both configured direction prefixes. Upload/download order, horizontal spacing, layout direction, and vertical alignment keep their current behavior.
- Mode 1 shows the configured upload prefix and upload speed. Disable the download prefix, upload-first order, meter spacing, layout direction, and vertical alignment settings.
- Mode 2 shows the configured download prefix and download speed. Disable the upload prefix, upload-first order, meter spacing, layout direction, and vertical alignment settings.
- Mode 3 shows total speed without a direction prefix. Disable both prefix settings, upload-first order, meter spacing, layout direction, and vertical alignment settings.
- Keep inapplicable preferences visible but disabled so users can understand that their saved values still apply when switching back to a compatible mode.
- Single-meter modes use one `Text` inside the existing Surface.
- Low-traffic auto-hide follows the displayed content: modes 0 (both) and 3 (total) use `totalSpeed`; mode 1 (upload only) uses `uploadSpeed`; mode 2 (download only) uses `downloadSpeed`.

Settings placement: Overlay section, after enable / lock, before prefixes.

## 7. Feature C — Status-bar up/down

### 7.1 Constraint

The notification small icon is ~24 dp. Two full speeds with units are unreadable if copied from Overlay text.

Android 16 Live Update `setShortCriticalText()` is also a short single chip.

### 7.2 Status-bar icon content

Keep notification `displayMode` for **notification drawer text**. Its existing value 0 renders upload and download together, even though the current setting label says Total. Rename that option to **Upload and download** without changing its stored value or rendering behavior:

| `key_notification_display_mode` | Drawer content |
|---|---|
| 0 | Upload and download (existing default behavior) |
| 1 | Upload only |
| 2 | Download only |

Do not migrate this key and do not change mode 0 to combined total speed.

Add a separate icon content key so drawer text and the tiny icon can differ:

| Key | Type | Default | Icon |
|---|---|---|---|
| `key_notification_icon_mode` | Int | 0 | 0 Total, 1 Upload, 2 Download |

- Status-bar Bitmap icons and Live Update short text support the same three single-speed modes: Total, Upload, and Download. Neither presentation displays upload and download simultaneously.
- Notification low-traffic threshold evaluation follows the selected icon content: Total uses `totalSpeed`, Upload uses `uploadSpeed`, and Download uses `downloadSpeed`. Notification drawer display mode does not affect this threshold decision. The same selected speed controls static low-traffic behavior and Live Update promotion/demotion.
- All status-bar Bitmap icons and Live Update short text use automatic scaling. They ignore the global Speed Unit and Minimum Display Unit settings; those settings continue to affect the main screen, Overlay, notification drawer text, and threshold summaries.
- Bitmap icons keep the current single-value layout and choose the value and full unit automatically from the selected global Rate unit (`MB/s` for bytes or `Mb/s` for bits, for example). Total, Upload, and Download use the same value/unit layout with no direction marker.
- Live Update formats the selected single speed as compact automatic text through `setShortCriticalText()`, also without a direction marker.
- User-configured notification prefixes remain limited to notification drawer text and are not rendered in the status-bar Bitmap icon or Live Update short text.
- Users who need simultaneous upload and download near the status bar should use the floating Overlay alternative described below.

### 7.3 Existing floating Overlay alternative (document, do not change behavior)

`key_overlay_show_on_status_bar` lets the `TYPE_APPLICATION_OVERLAY` floating window be placed in the status-bar area. It is not a SystemUI status-bar indicator and requires the Display over other apps permission. Settings copy must state that camera cutouts, privacy indicators, and system icons may affect available space.

Use this as an optional larger two-speed display near the status bar, without describing it as native status-bar integration.

## 8. Compatibility

- Missing new keys → defaults above. In particular, `key_notification_icon_mode` defaults to Total (0), preserving the current total-speed status-bar icon for existing and new installations.
- Normalize unsupported stored values at runtime without immediately rewriting DataStore: invalid Rate unit → Bytes (0), invalid Overlay display mode → Both (0), invalid Notification icon mode → Total (0). UI labels and rendering `when` branches must use the same fallbacks.
- Main-screen and Overlay setting changes update immediately through their Compose/StateFlow collectors.
- Notification drawer, status-bar Bitmap, and Live Update changes apply on the next network-speed sample. The accepted delay is at most one configured sampling interval (currently 1–3 seconds); do not add separate notification-setting collectors for immediate refresh.
- Notification render fingerprints must include the Rate unit, icon mode, and resulting compact/icon text so the next sampled update is not skipped.
- No migration or reset of `key_speed_unit` / `key_min_speed_unit`; their saved scale-level indices are reinterpreted through the selected Rate unit.
- Byte Auto output intentionally changes in the narrow 1000–1023 KB/s and 1000–1023 MB/s ranges: values remain in KB/s or MB/s until reaching the true 1024 boundary.
- All currently supported locales need new and updated strings: en, zh-rCN, pt, pt-rBR, ru. Use a shared “Upload and download” resource for Overlay mode 0 and notification drawer mode 0; do not create separate surface-specific labels when the wording is identical.
- Polish localization is out of scope. A Polish Play review motivated the bit-rate feature, but adding `values-pl` requires a complete app translation and should be handled separately, including through a community pull request.

## 9. Risks

- Byte and bit modes use different scales. Mitigate confusion with a Settings summary that explicitly states bytes use the existing 1024 scale while bits use decimal SI units.
- Bitmap icon readability with byte and bit units must be device-tested on light and dark status bars.
- Overlay file size: `OverlayWindow.kt` is already large; keep content branching inside `OverlayContent`.
- `SpeedFormatter` signature changes must update every call site (main, notification, Overlay, threshold summaries).
- Formatter inputs may be negative or near `Long.MAX_VALUE` in tests or abnormal states. Clamp negative values to zero and perform bit conversion as `Double` to prevent signed `Long` overflow.

## 10. Validation

- Bytes mode keeps existing labels and locked-unit behavior. Auto mode uses corrected 1024 boundaries: 1023 KB/s remains KB/s and 1024 KB/s becomes MB/s; 1023 MB/s remains MB/s and 1024 MB/s becomes GB/s.
- Bits: 1,000,000 B/s → `8.00 Mb/s` when locked to Mb/s and `8.0 Mb/s` in Auto; 1,048,576 B/s → approximately `8.39 Mb/s` when locked.
- Minimum `kb/s`: 100 B/s displays `0kb/s`; 125 B/s reaches the 1000 b/s floor and displays `1kb/s`.
- A stored threshold of 1024 B/s displays as `1 KB/s` in byte mode and `8.192 kb/s` in bit-mode custom input without changing the stored value.
- All formatted speeds and threshold fields use `.` as the decimal separator in every Locale. Custom threshold fields display at most 3 fractional digits with trailing zeros removed; decimal input rounds to the nearest bytes/s, while comma-decimal, invalid, negative, non-finite, and overflowing input leaves the previous threshold unchanged.
- Focused formatter validation covers negative input, zero, and `Long.MAX_VALUE`; none may overflow or produce a negative displayed speed. No new unit-test dependency is required for this iteration.
- Invalid persisted values for all three new Int settings fall back to Bytes / Both / Total without crashing or displaying an inconsistent selection.
- Overlay modes 0–3 update live while the service runs.
- Overlay auto-hide uses total speed in modes 0/3, upload speed in mode 1, and download speed in mode 2.
- Notification drawer text follows `key_notification_display_mode`: mode 0 remains the existing upload-and-download content and is relabeled accordingly; modes 1/2 remain upload/download only.
- Notification low-traffic behavior uses total/upload/download speed according to `key_notification_icon_mode`, independently of drawer content.
- Bitmap and Live Update icon modes 0–2 (Total, Upload, Download), including Android 16 Live Update; all use the same no-direction-marker layout and default to Total.
- While monitoring, changing Rate unit or notification icon mode updates the notification no later than the next sampling tick.
- Notification threshold behavior uses total speed in icon mode 0, upload speed in mode 1, and download speed in mode 2 for both Bitmap and Live Update paths.
- In mode 0, the floating Overlay can be placed in the status-bar area without being described as a native SystemUI indicator; verify behavior around the camera cutout, privacy indicators, and system icons.
