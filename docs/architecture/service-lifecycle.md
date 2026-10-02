# Foreground Service Lifecycle

## 1. Configuration

`NetworkMonitorService` declares `android:foregroundServiceType="specialUse|dataSync"` in the manifest and declares the `network_monitor` Special Use subtype.

At runtime:

- Android 14+: `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`
- Earlier supported versions: `FOREGROUND_SERVICE_TYPE_DATA_SYNC`

## 2. Start Sources

The Service may start from the main screen, "Finish and start" in Onboarding, or `BootReceiver` when auto-start is enabled. Quick Settings Tiles toggle display preferences; they do not start or stop the Service.

`POST_NOTIFICATIONS` is not a Foreground Service startup prerequisite. `DisplayPermissionState` shares the display requirements between the main screen and Onboarding:

- An enabled Overlay requires `Settings.canDrawOverlays()`, but never notification permission.
- With an authorized Overlay, monitoring can start even if notification speed / Live Update remains selected and notification permission was denied or revoked. The UI explains that notifications are unavailable.
- Notification-only display still asks for notification permission on Android 13+, so it does not start with an unavailable selected output.
- Android 12 and 12L do not have `POST_NOTIFICATIONS` and are never blocked by its runtime check.
- No selected display can still be monitored from the main screen; Onboarding saves that configuration without automatically starting.

## 3. Initial Notification

`onStartCommand()` must call `startForeground()` immediately. The initial notification reads the current Repository configuration, including dynamic notification state, prefixes and order, display mode, text sizes, low-traffic behavior, custom color, speed unit, and minimum display unit.

A basic notification object remains required even when dynamic notification speed or notification permission is disabled. Do not skip `startForeground()` or replace the Service with an ordinary background Service to hide the notification. On Android 13+, denied notification permission hides the Foreground Service notification from the notification drawer, but Android can still show the app in Active apps / Task Manager.

## 4. Active Operation

After startup, the Service:

1. Calls `NetworkRepository.startMonitoring()`.
2. Collects the `netSpeed` StateFlow.
3. Updates the Overlay on the main thread.
4. Builds notifications on a background thread.
5. Uses a visible-state fingerprint to avoid reposting equivalent notifications.

It supports a basic static notification, a dynamic Bitmap small icon, and Android 16+ Live Update.

The Overlay update runs independently of notification permission. When `POST_NOTIFICATIONS` is denied, optional `notify()` updates are skipped and the notification fingerprint is invalidated so a later grant refreshes even unchanged content. A permission race while posting is caught without cancelling monitoring. Enabling notification speed without permission must not stop a working Overlay.

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

- Android 12+ limits background Foreground Service starts. The current target is API 37.
- Permission requests and ordinary starts should originate from visible UI or another system-approved entry point.
- On Android 15+, the `SYSTEM_ALERT_WINDOW` background-start exemption requires an already visible Overlay. Holding Overlay permission alone does not allow an arbitrary background start; this fix adds no such start path. `BOOT_COMPLETED` has its own exemption and remains subject to service-type restrictions. The Service uses `specialUse` on Android 14+, not `dataSync`.
- `POST_PROMOTED_NOTIFICATIONS` is only for optional Live Update and does not replace ordinary notification permission.
- Changes to start sources, service types, or survival strategies must be checked against target-SDK behavior and Google Play policy.

References: [notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission), [launching a Foreground Service](https://developer.android.com/develop/background-work/services/fgs/launch), [background-start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start).

## 9. Permission Regression Checks

Run `./gradlew :app:testDebugUnitTest :app:assembleDebug lint`. `DisplayPermissionStateTest` covers all 16 combinations of selected notification / Overlay displays and granted / denied display permissions, plus restart, permission changes, toggling, and empty Onboarding selections. These JVM tests validate decision logic, not Android framework behavior.

On a real Pixel, validate Android 12/12L, Android 13+, and Android 15+ / the current target as available:

1. Fresh Onboarding, Overlay only: grant Overlay permission and deny notifications. Only the Overlay permission card is required; "Finish and start" starts monitoring.
2. Both displays selected: with Overlay granted and notifications denied, show the notification limitation and allow start. Repeat after stopping and reopening the app, with notification speed and Live Update preferences retained.
3. Notification only: deny notification permission and verify the notification permission prompt; grant it and verify normal notification speed / Live Update behavior.
4. Deny Overlay permission while Overlay is selected: verify startup asks for Overlay permission regardless of notification permission. Disable Overlay and verify notification-only startup with notifications granted.
5. While Overlay is running, toggle notification speed repeatedly with notifications denied. Sampling and Overlay must continue. Grant notification permission and confirm notification display refreshes even when the speed has not changed.
6. Revoke notifications, stop and restart monitoring, then regrant notifications. Also revoke Overlay permission while running and confirm the window is hidden safely; regrant and confirm it returns on a later sample. Check logs for startup or notification-posting exceptions.
7. Stop/start repeatedly, leave and return to the Activity, toggle both Quick Settings Tiles, and test screen-off sleep / screen-on resume. Confirm there is only one Overlay and one Foreground Service notification when notifications are permitted.
8. Enable auto-start with Overlay permission granted and notifications denied, reboot, and verify the existing boot path on supported devices. Background startup denial must be caught; Overlay permission alone must not be treated as a new API 35+ background-start exemption.

These device checks remain required before claiming runtime validation; successful compilation, Lint, and JVM tests are not a substitute.
