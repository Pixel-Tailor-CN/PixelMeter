package vip.mystery0.pixel.meter.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import vip.mystery0.pixel.meter.R
import vip.mystery0.pixel.meter.data.repository.NetworkRepository
import vip.mystery0.pixel.meter.service.NetworkMonitorService

class MainViewModel(
    private val application: Application,
) : AndroidViewModel(application), KoinComponent {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val repository: NetworkRepository by inject()

    val currentSpeed = repository.netSpeed

    val isOnboardingShown = repository.isOnboardingShown
    val isLiveUpdateEnabled = repository.isLiveUpdateEnabled
    val isOverlayEnabled = repository.isOverlayEnabled
    val isNotificationEnabled = repository.isNotificationEnabled
    val isHideFromRecents = repository.isHideFromRecents

    val speedUnit = repository.speedUnit
    val speedRateUnit = repository.speedRateUnit
    val minSpeedUnit = repository.minSpeedUnit
    val appThemeMode = repository.appThemeMode
    val appThemeColor = repository.appThemeColor
    val isAppThemeUseAmoledBlack = repository.isAppThemeUseAmoledBlack

    val isServiceRunning = repository.isMonitoring

    private val _serviceStartError = MutableStateFlow<Pair<String, String>?>(null)
    val serviceStartError = _serviceStartError.asStateFlow()

    fun skipOnboarding() {
        viewModelScope.launch {
            repository.markOnboardingShown()
        }
    }

    fun completeOnboarding(
        notificationEnabled: Boolean,
        liveUpdateEnabled: Boolean,
        overlayEnabled: Boolean,
        startServiceAfterSaving: Boolean
    ) {
        viewModelScope.launch {
            repository.completeOnboarding(
                notificationEnabled = notificationEnabled,
                liveUpdateEnabled = liveUpdateEnabled,
                overlayEnabled = overlayEnabled
            )
            if (startServiceAfterSaving) {
                startService()
            }
        }
    }

    fun startService() {
        _serviceStartError.value = null

        // 1. Check notification permission (runtime grant is required only on Android 13+).
        if (!hasNotificationPermission()) {
            _serviceStartError.value =
                application.getString(R.string.error_notification_permission) to Settings.ACTION_APP_NOTIFICATION_SETTINGS
            return
        }

        // 2. Check Overlay Permission if enabled
        if (isOverlayEnabled.value) {
            if (!Settings.canDrawOverlays(application)) {
                _serviceStartError.value =
                    application.getString(R.string.error_overlay_permission) to Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                return
            }
        }

        val intent = Intent(application, NetworkMonitorService::class.java)
        try {
            application.startForegroundService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "startService: start foreground service error", e)
            _serviceStartError.value =
                application.getString(
                    R.string.error_service_start_failed,
                    e.message
                ) to Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        }
    }

    fun stopService(clearError: Boolean = true) {
        val intent = Intent(application, NetworkMonitorService::class.java)
        application.stopService(intent)

        if (clearError) {
            clearError()
        }
    }

    fun clearError() {
        _serviceStartError.value = null
    }

    fun setOverlayEnabled(enable: Boolean) {
        if (enable && isServiceRunning.value) {
            if (!Settings.canDrawOverlays(application)) {
                _serviceStartError.value =
                    application.getString(R.string.error_overlay_permission) to Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                stopService(false)
            }
        }
        repository.setOverlayEnabled(enable)
    }

    fun setNotificationEnabled(enable: Boolean) {
        if (enable && isServiceRunning.value) {
            if (!hasNotificationPermission()) {
                _serviceStartError.value =
                    application.getString(R.string.error_notification_permission) to Settings.ACTION_APP_NOTIFICATION_SETTINGS
                stopService(false)
            }
        }
        repository.setNotificationEnabled(enable)
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                application,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
}
