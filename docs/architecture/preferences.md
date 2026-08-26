# Preferences DataStore

## 1. Storage

Pixel Meter uses Preferences DataStore named `pixel_pulse_preferences`.

`DataStoreRepository` owns keys and raw reads or writes. `NetworkRepository` exposes settings as `StateFlow` and provides application-level update methods.

## 2. New Installations and Existing Users

When `key_onboarding_shown` is absent:

- Empty Preferences indicate a new installation: show Onboarding and disable notification speed by default.
- Non-empty Preferences indicate an upgrade: do not show Onboarding automatically, and preserve the historical notification default.

When a new user skips Onboarding, write `key_onboarding_shown=true`. If DataStore is still empty, also write `key_notification_enabled=false`.

## 3. Keys

### Onboarding, Service, and Global Display

| Key | Type | Default or compatibility value | Description |
|---|---|---|---|
| `key_onboarding_shown` | Boolean | false for new users; true for existing data | Whether automatic Onboarding has been shown |
| `key_auto_start_service` | Boolean | false | Start monitoring after boot |
| `key_hide_from_recents` | Boolean | false | Hide the app from Recents |
| `key_sampling_interval` | Long | 1500 | Sampling interval in milliseconds |
| `key_speed_unit` | Int | 0 | 0 Auto; 1 B/s; 2 KB/s; 3 MB/s; 4 GB/s |
| `key_min_speed_unit` | Int | 0 | Minimum Auto unit: 0 None; 1 KB/s; 2 MB/s; 3 GB/s |

### App Theme

| Key | Type | Default | Description |
|---|---|---|---|
| `key_app_theme_mode` | Int | Dynamic | Dynamic Color or fixed theme color |
| `key_app_theme_color` | Int | `DEFAULT_THEME_COLOR` | Fixed theme color |
| `key_app_theme_use_amoled_black` | Boolean | false | Use black surfaces in fixed-color dark mode |

### Notifications and Live Update

| Key | Type | Default or compatibility value | Description |
|---|---|---|---|
| `key_live_update` | Boolean | false | Android 16+ Live Update |
| `key_notification_enabled` | Boolean | false for new users; true for existing data | Dynamic notification speed |
| `key_notification_text_up` | String | `▲ ` | Upload prefix |
| `key_notification_text_down` | String | `▼ ` | Download prefix |
| `key_notification_order_up_first` | Boolean | true | Show upload first |
| `key_notification_display_mode` | Int | 0 | 0 Total; 1 Upload; 2 Download |
| `key_notification_text_size` | Float | 0.65 | Bitmap value text-size ratio |
| `key_notification_unit_size` | Float | 0.35 | Bitmap unit text-size ratio |
| `key_notification_threshold` | Long | 0 | Low-traffic threshold in bytes per second; 0 disables it |
| `key_notification_low_traffic_mode` | Int | 0 | 0 static icon; 1 dynamic low-speed value |
| `key_notification_use_custom_color` | Boolean | false | Apply a notification accent color |
| `key_notification_color` | Int | 0 | Notification accent color |

### Overlay

| Key | Type | Default | Description |
|---|---|---|---|
| `key_overlay_enabled` | Boolean | false | Enable the Overlay |
| `key_overlay_locked` | Boolean | false | Lock its position |
| `key_overlay_show_on_status_bar` | Boolean | false | Allow it in status-bar and cutout areas |
| `key_overlay_x` | Int | 100 | X coordinate in px |
| `key_overlay_y` | Int | 200 | Y coordinate in px |
| `key_overlay_bg_color` | Int | `0xCC000000` | Background color |
| `key_overlay_text_color` | Int | `0xFFFFFFFF` | Text color |
| `key_overlay_corner_radius` | Int | 8 | Corner radius in dp |
| `key_overlay_padding` | Int | 8 | Background padding in dp |
| `key_overlay_text_size` | Float | 10 | Text size in sp |
| `key_overlay_text_up` | String | `▲ ` | Upload prefix |
| `key_overlay_text_down` | String | `▼ ` | Download prefix |
| `key_overlay_order_up_first` | Boolean | true | Show upload first |
| `key_overlay_hide_background` | Boolean | false | Use a fully transparent background |
| `key_overlay_use_default_colors` | Boolean | false | Use themed Surface colors |
| `key_overlay_direction` | Int | 0 | 0 horizontal; 1 vertical |
| `key_overlay_alignment` | Int | 0 | 0 Start; 1 Center; 2 End |
| `key_overlay_meter_spacing` | Int | 8 | Horizontal upload/download spacing in dp |
| `key_overlay_portrait_only` | Boolean | false | Hide in landscape |
| `key_overlay_hide_in_immersive_mode` | Boolean | false | Hide when system bars are hidden |
| `key_overlay_auto_hide_threshold` | Long | 0 | Sustained low-traffic hide threshold in bytes per second |

## 4. Persistence and Application

- Theme and most UI settings apply immediately through StateFlow.
- Overlay coordinates are saved when dragging ends.
- While the Service is running, notifications and the Overlay consume current Repository settings.
- Completing Onboarding writes notification, Live Update, Overlay, and completion state in one operation.
- Auto-start only affects `BootReceiver`; changing it does not immediately start the Service.

## 5. Change Requirements

When adding or modifying a key:

1. Define its default and read/write methods in `DataStoreRepository`.
2. Synchronize its StateFlow in `NetworkRepository`.
3. Evaluate compatibility when the key is absent for existing users.
4. Update this document.
5. If the key is user-facing, update the relevant UI documentation and translations.
