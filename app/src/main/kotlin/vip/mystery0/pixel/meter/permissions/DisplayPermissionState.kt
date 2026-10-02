package vip.mystery0.pixel.meter.permissions

/** Permission requirements for the selected displays, independent of Foreground Service startup. */
data class DisplayPermissionState(
    val notificationEnabled: Boolean,
    val overlayEnabled: Boolean,
    val notificationGranted: Boolean,
    val overlayGranted: Boolean
) {
    val hasSelectedDisplay: Boolean
        get() = notificationEnabled || overlayEnabled

    val missingOverlayPermission: Boolean
        get() = overlayEnabled && !overlayGranted

    val missingNotificationPermission: Boolean
        get() = notificationEnabled && !notificationGranted

    // An authorized Overlay works even when notification speed or Live Update is also selected.
    val canStartMonitoring: Boolean
        get() = !missingOverlayPermission &&
            (!missingNotificationPermission || overlayEnabled)
}
