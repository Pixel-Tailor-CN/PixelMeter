# Foreground Service Lifecycle

## 1. Configuration

`NetworkMonitorService` declares `android:foregroundServiceType="specialUse|dataSync"` in the manifest and declares the `network_monitor` Special Use subtype.

At runtime:

- Android 14+: `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`
- Earlier supported versions: `FOREGROUND_SERVICE_TYPE_DATA_SYNC`

## 2. Start Sources

The Service may start from the main screen, "Finish and start" in Onboarding, a Quick Settings Tile, or `BootReceiver` when auto-start is enabled.

Check `POST_NOTIFICATIONS` before starting on Android 13+. Android 12 and 12L do not have this runtime permission and must not be blocked by that check. If the Overlay is enabled, also check `Settings.canDrawOverlays()`.

## 3. Initial Notification

`onStartCommand()` must call `startForeground()` immediately. The initial notification reads the current Repository configuration, including dynamic notification state, prefixes and order, display mode, text sizes, low-traffic behavior, custom color, speed unit, and minimum display unit.

A basic ongoing notification remains required even when dynamic notification speed is disabled.

## 4. Active Operation

After startup, the Service:

1. Calls `NetworkRepository.startMonitoring()`.
2. Collects the `netSpeed` StateFlow.
3. Updates the Overlay on the main thread.
4. Builds notifications on a background thread.
5. Uses a visible-state fingerprint to avoid reposting equivalent notifications.

It supports a basic static notification, a dynamic Bitmap small icon, and Android 16+ Live Update.

## 5. Screen-Off Policy

- `ACTION_SCREEN_OFF` starts a two-minute timer.
- If the screen remains off when the timer expires, Repository sampling stops while the Service stays alive.
- `ACTION_SCREEN_ON` cancels the timer and resumes sampling if it was paused.

This reduces continuous computation while the screen is off and restores speed display when the screen turns on.

## 6. Boot Startup

`BootReceiver` listens for `BOOT_COMPLETED` and `QUICKBOOT_POWERON`. It calls `startForegroundService()` only when `key_auto_start_service` is true. Startup exceptions are caught and logged.

## 7. Shutdown and Cleanup

When destroyed, the Service cancels the speed-collection and screen-off Jobs, hides and releases the Overlay, stops Repository sampling, removes the Foreground Notification, and unregisters the screen broadcast Receiver.

## 8. Android System Constraints

- Android 14+ strictly limits background Foreground Service starts.
- Permission requests and ordinary starts should originate from visible UI or another system-approved entry point.
- `POST_PROMOTED_NOTIFICATIONS` is only for optional Live Update and does not replace ordinary notification permission.
- Changes to start sources, service types, or survival strategies must be checked against target-SDK behavior and Google Play policy.
