package vip.mystery0.pixel.meter.service

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import vip.mystery0.pixel.meter.data.repository.NetworkRepository
import vip.mystery0.pixel.meter.data.source.NetSpeedData
import vip.mystery0.pixel.meter.ui.overlay.OverlayWindow
import kotlin.time.Duration.Companion.milliseconds

class NetworkMonitorService : Service() {
    companion object {
        private const val TAG = "NetworkMonitorService"
    }

    private val repository: NetworkRepository by inject()
    private val notificationHelper: NotificationHelper by inject()
    private val notificationManager: NotificationManager by inject()
    private val overlayWindow: OverlayWindow by inject()

    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var notificationPostedAtMillis = 0L
    private var lastNotificationFingerprint: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (notificationPostedAtMillis == 0L) {
            notificationPostedAtMillis = System.currentTimeMillis()
        }
        val initialNotif = notificationHelper.buildNotification(
            speed = NetSpeedData(0, 0),
            isLiveUpdate = false,
            isNotificationEnabled = repository.isNotificationEnabled.value,
            textUp = repository.notificationTextUp.value,
            textDown = repository.notificationTextDown.value,
            upFirst = repository.notificationOrderUpFirst.value,
            displayMode = repository.notificationDisplayMode.value,
            iconMode = repository.notificationIconMode.value,
            textSize = repository.notificationTextSize.value,
            unitSize = repository.notificationUnitSize.value,
            threshold = repository.notificationThreshold.value,
            lowTrafficMode = repository.notificationLowTrafficMode.value,
            useCustomColor = repository.notificationUseCustomColor.value,
            color = repository.notificationColor.value,
            speedUnit = repository.speedUnit.value,
            minSpeedUnit = repository.minSpeedUnit.value,
            rateUnit = repository.speedRateUnit.value,
            postedAtMillis = notificationPostedAtMillis
        )
        lastNotificationFingerprint = initialNotif.fingerprint

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID,
                    initialNotif.notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID,
                    initialNotif.notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand: start foreground error", e)
            stopSelf()
            return START_NOT_STICKY
        }

        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceJob?.cancel()

        // Start Repository Monitoring
        repository.startMonitoring()

        serviceJob = scope.launch {
            repository.speedSamples.collect { speed ->
                // Overlay logic
                withContext(Dispatchers.Main) {
                    try {
                        if (repository.isOverlayEnabled.value) {
                            if (Settings.canDrawOverlays(this@NetworkMonitorService)) {
                                overlayWindow.show()
                                overlayWindow.update(speed)
                            } else {
                                Log.w(
                                    TAG,
                                    "overlay enabled but permission not granted, hiding overlay."
                                )
                                overlayWindow.hide()
                            }
                        } else {
                            overlayWindow.hide()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "startMonitoring: overlay window error", e)
                    }
                }

                // Notification logic
                val notificationResult = withContext(Dispatchers.Default) {
                    val isLiveUpdate = repository.isLiveUpdateEnabled.value
                    val isNotificationEnabled = repository.isNotificationEnabled.value
                    val textUp = repository.notificationTextUp.value
                    val textDown = repository.notificationTextDown.value
                    val upFirst = repository.notificationOrderUpFirst.value
                    val displayMode = repository.notificationDisplayMode.value
                    val iconMode = repository.notificationIconMode.value
                    val textSize = repository.notificationTextSize.value
                    val unitSize = repository.notificationUnitSize.value
                    val threshold = repository.notificationThreshold.value
                    val lowTrafficMode = repository.notificationLowTrafficMode.value
                    val useCustomColor = repository.notificationUseCustomColor.value
                    val color = repository.notificationColor.value
                    val speedUnit = repository.speedUnit.value
                    val minSpeedUnit = repository.minSpeedUnit.value
                    val rateUnit = repository.speedRateUnit.value

                    notificationHelper.buildNotification(
                        speed = speed,
                        isLiveUpdate = isLiveUpdate,
                        isNotificationEnabled = isNotificationEnabled,
                        textUp = textUp,
                        textDown = textDown,
                        upFirst = upFirst,
                        displayMode = displayMode,
                        iconMode = iconMode,
                        textSize = textSize,
                        unitSize = unitSize,
                        threshold = threshold,
                        lowTrafficMode = lowTrafficMode,
                        useCustomColor = useCustomColor,
                        color = color,
                        speedUnit = speedUnit,
                        minSpeedUnit = minSpeedUnit,
                        rateUnit = rateUnit,
                        postedAtMillis = notificationPostedAtMillis
                    )
                }
                if (notificationResult.fingerprint != lastNotificationFingerprint) {
                    lastNotificationFingerprint = notificationResult.fingerprint
                    notificationManager.notify(
                        NotificationHelper.NOTIFICATION_ID,
                        notificationResult.notification
                    )
                }
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen OFF: Scheduling sleep in 2 minutes")
                    stopMonitoringJob?.cancel()
                    stopMonitoringJob = scope.launch {
                        delay((2 * 60 * 1000L).milliseconds) // 2 minutes
                        Log.d(TAG, "Screen OFF Timeout: Stopping monitoring to save power")
                        repository.stopMonitoring()
                    }
                }

                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen ON: Cancelling sleep timer")
                    stopMonitoringJob?.cancel()
                    if (!repository.isMonitoring.value) {
                        Log.d(TAG, "Screen ON: Resuming monitoring")
                        startMonitoring()
                    }
                }
            }
        }
    }

    private var stopMonitoringJob: Job? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        serviceJob?.cancel()
        stopMonitoringJob?.cancel()
        notificationPostedAtMillis = 0L
        lastNotificationFingerprint = null
        overlayWindow.hide()
        repository.stopMonitoring()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
