# Notifications and Live Update

## 1. Basic Foreground Service Notification

Android requires an ongoing notification while the monitoring Service runs. When dynamic speed is disabled, `NotificationHelper` publishes a minimal basic notification instead of removing it.

The notification channel uses a stable `CHANNEL_ID`; its name and description come from localized resources. Channel availability, sound, and importance remain controlled by system settings.

## 2. Dynamic Bitmap Icon

Standard notification mode uses `IconCompat.createWithBitmap()`:

1. `SpeedFormatter` separates total speed into a value and unit.
2. Canvas draws them with two Paint objects.
3. Users can adjust value and unit text-size ratios.
4. The small icon always represents total speed.

Notification content supports total bidirectional speed, upload only, download only, and custom prefixes and order.

## 3. Low-Traffic Mode

When total speed is below `key_notification_threshold`:

- Static mode uses the application icon and monitoring text.
- Dynamic mode continues to show a low-speed value without Live Update.

A threshold of zero disables low-traffic handling.

## 4. Live Update

Android 16+ can enable Live Update:

- Use the static `ic_speed` small icon.
- Show compact speed text through `setShortCriticalText()`.
- Request Promoted Ongoing presentation with `setRequestPromotedOngoing(true)`.
- Require ordinary notification permission and system support; the manifest declares `POST_PROMOTED_NOTIFICATIONS`.

Android decides whether the notification is ultimately promoted.

## 5. Formatting

`SpeedFormatter` is the single source for speed formatting across the main screen, notifications, Live Update, Overlay, and threshold summaries.

Fixed units show zero, one, or two decimal places based on value range. Auto mode selects B/s, KB/s, MB/s, or GB/s. A minimum display unit can render smaller traffic as zero.

## 6. Update Deduplication

`NotificationHelper` creates a fingerprint containing only visible state. The Service calls `NotificationManager.notify()` only when the fingerprint changes, preventing redundant refreshes and status-bar icon reordering.

## 7. Device Validation

Validate Android 12/12L startup, Android 13+ permission grant and denial, Android 16+ Live Update, light and dark status-bar readability, custom colors across ROMs, and refresh stability with multiple ongoing notifications.
