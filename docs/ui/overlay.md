# Compose Overlay

## 1. Window Model

The Overlay uses `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` and requires `SYSTEM_ALERT_WINDOW` permission.

Default flags are `FLAG_NOT_FOCUSABLE`, `FLAG_LAYOUT_IN_SCREEN`, and `FLAG_NOT_TOUCH_MODAL`. Allowing the Overlay over the status bar adds `FLAG_LAYOUT_NO_LIMITS` and configures cutout mode.

## 2. Compose Host

`OverlayWindow` creates a `ComposeView` outside an Activity and implements `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner`.

Showing initializes lifecycle and saved state. Hiding removes the View, dispatches the destroy event, and clears the `ViewModelStore`.

## 3. Content

The Overlay displays upload and download speeds formatted by `SpeedFormatter`. It supports custom prefixes and order, horizontal or vertical layout, Start/Center/End alignment for vertical values, horizontal spacing, text size, padding, corner radius, custom colors, themed colors, and a transparent background.

## 4. Dragging and Position

When unlocked, `detectDragGestures` updates WindowManager X/Y coordinates. Drag completion writes the coordinates to DataStore for the next display session.

Coordinates use px and may be negative. The status-bar option changes the valid drag area and must be validated on devices with display cutouts.

## 5. Conditional Hiding

### Landscape

Portrait Only hides the Overlay in landscape orientation.

### Immersive Mode

WindowInsets provide status-bar and navigation-bar visibility. If the foreground app hides either system bar, the Overlay can become transparent and non-touchable.

The root `ComposeView` remains attached so it can continue receiving Insets and restore the Overlay when system bars return.

### Sustained Low Traffic

The Overlay hides after total speed remains below `key_overlay_auto_hide_threshold` for three sampling cycles and reappears when speed recovers. A threshold of zero disables this behavior.

## 6. Theme and Colors

The Overlay uses the app's `PixelPulseTheme`. Default color mode uses Material 3 Surface and OnSurface colors; custom mode uses saved ARGB values.

## 7. Device Validation

Validate permission grant and revocation, dragging and position persistence, orientation changes, status-bar and cutout areas, immersive video or games, navigation-mode changes, and low-traffic hiding and restoration.
