# Onboarding

## 1. Goal

Onboarding explains VPN-safe traffic counting, lets users choose display methods, and requests only the required permissions. It must not enable a display method without confirmation.

## 2. Display Rules

- A new installation with empty Preferences shows Onboarding once.
- Existing users with Preferences are not shown Onboarding after upgrading.
- Skipping or finishing writes `key_onboarding_shown=true`.
- Settings > General provides an entry to rerun the wizard.

## 3. Three-Step Flow

### Step 1: Product Introduction

Explain the app's purpose and how reading physical interfaces avoids counting VPN traffic twice. Actions are Start setup, Set up later, and the TopAppBar Skip action.

### Step 2: Display Methods

All options are disabled by default: notification speed, Live Update, and Overlay.

Recommended configuration:

- Android 16+: notification speed + Live Update.
- Earlier versions: notification speed.

Disabling notification speed also disables Live Update. Live Update cannot be changed on unsupported versions. The page must explain that Android always requires a basic Foreground Service notification while monitoring.

### Step 3: Permissions and Completion

Selecting notification speed or Overlay requires notification permission because monitoring uses a Foreground Service. Overlay additionally requires permission to draw over other apps.

- All required permissions granted: show "Finish and start".
- No display method selected: show "Finish" and do not start the Service.
- Permissions incomplete: disable the primary action and offer "Grant later"; save selections without starting automatically.

## 4. Persistence

Completion writes these values together:

- `key_onboarding_shown`
- `key_notification_enabled`
- `key_live_update`
- `key_overlay_enabled`

For "Finish and start", the Repository updates persistence and StateFlow before `MainViewModel` starts the Service, preventing the Service from reading stale configuration.

## 5. Lifecycle

`MainActivity` receives rerun requests through an Intent extra. The extra is removed after consumption to prevent Activity recreation from reopening the wizard. Permission state refreshes on Activity `ON_RESUME` to handle returns from system settings.

## 6. Copy and Compatibility

- All UI copy must come from String Resources.
- Permission names and Android-version restrictions must be explicit.
- New Onboarding copy must be added to every supported Locale.
- Completion behavior must account for notification-permission differences between Android 12/12L and Android 13+.
