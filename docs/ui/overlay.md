# Compose Overlay

## 1. Window Model

The Overlay uses `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` and requires `SYSTEM_ALERT_WINDOW` permission.

Default flags are `FLAG_NOT_FOCUSABLE`, `FLAG_LAYOUT_IN_SCREEN`, and `FLAG_NOT_TOUCH_MODAL`. Allowing the Overlay over the status bar adds `FLAG_LAYOUT_NO_LIMITS` and configures cutout mode.

## 2. Compose Host

`OverlayWindow` creates a `ComposeView` outside an Activity and implements `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner`.

Showing initializes lifecycle and saved state. Hiding removes the View, dispatches the destroy event, and clears the `ViewModelStore`.

## 3. Content

The Overlay formats speed through `SpeedFormatter` and supports four content modes: Upload and download, Upload only, Download only, and Total. Single-direction modes retain their matching configured prefix; Total has no direction prefix. Two-meter order, direction, spacing, and alignment controls remain visible but are disabled when the selected content mode does not use them.

The global Rate unit selects Bytes (existing binary scale) or Bits (decimal SI scale).

## 4. Dragging and Position

When unlocked, `detectDragGestures` updates WindowManager X/Y coordinates. Drag completion writes the coordinates to DataStore for the next display session.

Coordinates use px and may be negative. The status-bar option places the floating `TYPE_APPLICATION_OVERLAY` in that area; it is not native SystemUI integration. It requires the Display over other apps permission and must be validated around cutouts, privacy indicators, and system icons.

## 5. Conditional Hiding

### Landscape

Portrait Only hides the Overlay in landscape orientation.

### Immersive Mode

WindowInsets provide status-bar and navigation-bar visibility. If the foreground app hides either system bar, the Overlay can become transparent and non-touchable.

The root `ComposeView` remains attached so it can continue receiving Insets and restore the Overlay when system bars return.

### Sustained Low Traffic

The Overlay hides after the displayed speed remains below `key_overlay_auto_hide_threshold` for three sampling cycles and reappears when speed recovers. Upload-only and Download-only modes evaluate their respective direction; Upload-and-download and Total evaluate total speed. A threshold of zero disables this behavior.

## 6. Theme and Colors

The Overlay uses the app's `PixelPulseTheme`. Default color mode uses Material 3 Surface and OnSurface colors; custom mode uses saved ARGB values.

## 7. Device Validation

Validate permission grant and revocation, dragging and position persistence, orientation changes, status-bar and cutout areas, immersive video or games, navigation-mode changes, and low-traffic hiding and restoration.
