package vip.mystery0.pixel.meter.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayPermissionStateTest {
    @Test
    fun everyDisplayAndPermissionCombination() {
        for (notificationEnabled in listOf(false, true)) {
            for (overlayEnabled in listOf(false, true)) {
                for (notificationGranted in listOf(false, true)) {
                    for (overlayGranted in listOf(false, true)) {
                        val state = DisplayPermissionState(
                            notificationEnabled, overlayEnabled,
                            notificationGranted, overlayGranted
                        )
                        val expected = when {
                            overlayEnabled -> overlayGranted
                            notificationEnabled -> notificationGranted
                            else -> true // In-app monitoring does not require either display permission.
                        }
                        assertEquals(state.toString(), expected, state.canStartMonitoring)
                        assertEquals(
                            notificationEnabled || overlayEnabled,
                            state.hasSelectedDisplay
                        )
                        assertEquals(
                            notificationEnabled && !notificationGranted,
                            state.missingNotificationPermission
                        )
                        assertEquals(
                            overlayEnabled && !overlayGranted,
                            state.missingOverlayPermission
                        )
                    }
                }
            }
        }
    }

    @Test
    fun overlayOnlyCanFinishAndStartWithoutNotificationPermission() {
        val state = DisplayPermissionState(false, true, false, true)
        assertTrue(state.hasSelectedDisplay)
        assertTrue(state.canStartMonitoring)
        assertFalse(state.missingNotificationPermission)
    }

    @Test
    fun notificationRevocationDoesNotPreventOverlayRestart() {
        val state = DisplayPermissionState(true, true, true, true)
        assertTrue(state.canStartMonitoring)
        val denied = state.copy(notificationGranted = false)
        assertTrue(denied.canStartMonitoring)
        assertTrue(denied.missingNotificationPermission)
        assertTrue(denied.copy(notificationGranted = true).canStartMonitoring)
    }

    @Test
    fun notificationToggleDoesNotMakeAuthorizedOverlayDependentOnNotifications() {
        val state = DisplayPermissionState(false, true, false, true)
        assertTrue(state.canStartMonitoring)
        assertTrue(state.copy(notificationEnabled = true).canStartMonitoring)
        assertFalse(state.copy(notificationEnabled = true, overlayEnabled = false).canStartMonitoring)
    }

    @Test
    fun overlayStillRequiresItsOwnPermission() {
        val state = DisplayPermissionState(true, true, true, true)
        val revoked = state.copy(overlayGranted = false)
        assertFalse(revoked.canStartMonitoring)
        assertTrue(revoked.missingOverlayPermission)
        assertTrue(revoked.copy(overlayEnabled = false).canStartMonitoring)
    }

    @Test
    fun notificationOnlyStillNeedsNotificationPermission() {
        val state = DisplayPermissionState(true, false, false, false)
        assertFalse(state.canStartMonitoring)
        assertTrue(state.missingNotificationPermission)
        assertTrue(state.copy(notificationGranted = true).canStartMonitoring)
    }

    @Test
    fun noSelectedDisplayFinishesWithoutAutomaticallyStarting() {
        val state = DisplayPermissionState(false, false, false, false)
        assertTrue(state.canStartMonitoring)
        assertFalse(state.hasSelectedDisplay)
    }
}
