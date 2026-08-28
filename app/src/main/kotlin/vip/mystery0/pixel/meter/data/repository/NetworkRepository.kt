package vip.mystery0.pixel.meter.data.repository

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import vip.mystery0.pixel.meter.data.model.AppThemeMode
import vip.mystery0.pixel.meter.data.source.NetSpeedData
import vip.mystery0.pixel.meter.data.source.impl.SpeedDataSource

class NetworkRepository(
    private val dataSource: SpeedDataSource,
    private val dataStoreRepository: DataStoreRepository,
) : KoinComponent {
    private val _speedSamples = MutableSharedFlow<NetSpeedData>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply {
        tryEmit(NetSpeedData(0, 0))
    }
    val speedSamples: SharedFlow<NetSpeedData> = _speedSamples.asSharedFlow()

    private val _isOnboardingShown = MutableStateFlow(false)
    val isOnboardingShown: StateFlow<Boolean> = _isOnboardingShown.asStateFlow()

    private val _isOverlayEnabled = MutableStateFlow(false)
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

    private val _isLiveUpdateEnabled = MutableStateFlow(false)
    val isLiveUpdateEnabled: StateFlow<Boolean> = _isLiveUpdateEnabled.asStateFlow()

    private val _isNotificationEnabled = MutableStateFlow(false)
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled.asStateFlow()

    private val _isOverlayLocked = MutableStateFlow(false)
    val isOverlayLocked: StateFlow<Boolean> = _isOverlayLocked.asStateFlow()

    private val _isOverlayShowOnStatusBar = MutableStateFlow(false)
    val isOverlayShowOnStatusBar: StateFlow<Boolean> = _isOverlayShowOnStatusBar.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _netSpeed = MutableStateFlow(NetSpeedData(0, 0))
    val netSpeed: StateFlow<NetSpeedData> = _netSpeed.asStateFlow()

    private val _samplingInterval = MutableStateFlow(1500L)
    val samplingInterval: StateFlow<Long> = _samplingInterval.asStateFlow()

    private val _overlayBgColor = MutableStateFlow(0xCC000000.toInt())
    val overlayBgColor: StateFlow<Int> = _overlayBgColor.asStateFlow()

    private val _overlayTextColor = MutableStateFlow(0xFFFFFFFF.toInt())
    val overlayTextColor: StateFlow<Int> = _overlayTextColor.asStateFlow()

    private val _overlayCornerRadius = MutableStateFlow(8)
    val overlayCornerRadius: StateFlow<Int> = _overlayCornerRadius.asStateFlow()

    private val _overlayPadding = MutableStateFlow(8)
    val overlayPadding: StateFlow<Int> = _overlayPadding.asStateFlow()

    private val _overlayTextSize = MutableStateFlow(10f)
    val overlayTextSize: StateFlow<Float> = _overlayTextSize.asStateFlow()

    private val _overlayTextUp = MutableStateFlow("▲ ")
    val overlayTextUp: StateFlow<String> = _overlayTextUp.asStateFlow()

    private val _overlayTextDown = MutableStateFlow("▼ ")
    val overlayTextDown: StateFlow<String> = _overlayTextDown.asStateFlow()

    private val _overlayOrderUpFirst = MutableStateFlow(true)
    val overlayOrderUpFirst: StateFlow<Boolean> = _overlayOrderUpFirst.asStateFlow()

    private val _isOverlayHideBackground = MutableStateFlow(false)
    val isOverlayHideBackground: StateFlow<Boolean> = _isOverlayHideBackground.asStateFlow()

    private val _overlayX = MutableStateFlow(100)
    val overlayX: StateFlow<Int> = _overlayX.asStateFlow()

    private val _overlayY = MutableStateFlow(200)
    val overlayY: StateFlow<Int> = _overlayY.asStateFlow()

    private val _overlayDirection = MutableStateFlow(0)
    val overlayDirection: StateFlow<Int> = _overlayDirection.asStateFlow()

    private val _overlayDisplayMode = MutableStateFlow(0)
    val overlayDisplayMode: StateFlow<Int> = _overlayDisplayMode.asStateFlow()

    private val _overlayAlignment = MutableStateFlow(0)
    val overlayAlignment: StateFlow<Int> = _overlayAlignment.asStateFlow()

    private val _overlayMeterSpacing = MutableStateFlow(8)
    val overlayMeterSpacing: StateFlow<Int> = _overlayMeterSpacing.asStateFlow()

    private val _isOverlayPortraitOnly = MutableStateFlow(false)
    val isOverlayPortraitOnly: StateFlow<Boolean> = _isOverlayPortraitOnly.asStateFlow()

    private val _isOverlayHideInImmersiveMode = MutableStateFlow(false)
    val isOverlayHideInImmersiveMode: StateFlow<Boolean> =
        _isOverlayHideInImmersiveMode.asStateFlow()

    private val _overlayAutoHideThreshold = MutableStateFlow(0L)
    val overlayAutoHideThreshold: StateFlow<Long> = _overlayAutoHideThreshold.asStateFlow()

    private val _notificationTextUp = MutableStateFlow("▲ ")
    val notificationTextUp: StateFlow<String> = _notificationTextUp.asStateFlow()

    private val _notificationTextDown = MutableStateFlow("▼ ")
    val notificationTextDown: StateFlow<String> = _notificationTextDown.asStateFlow()

    private val _notificationOrderUpFirst = MutableStateFlow(true)
    val notificationOrderUpFirst: StateFlow<Boolean> = _notificationOrderUpFirst.asStateFlow()

    private val _notificationDisplayMode = MutableStateFlow(0)
    val notificationDisplayMode: StateFlow<Int> = _notificationDisplayMode.asStateFlow()

    private val _notificationIconMode = MutableStateFlow(0)
    val notificationIconMode: StateFlow<Int> = _notificationIconMode.asStateFlow()

    private val _notificationTextSize = MutableStateFlow(0.65f)
    val notificationTextSize: StateFlow<Float> = _notificationTextSize.asStateFlow()

    private val _notificationUnitSize = MutableStateFlow(0.35f)
    val notificationUnitSize: StateFlow<Float> = _notificationUnitSize.asStateFlow()

    private val _notificationThreshold = MutableStateFlow(0L)
    val notificationThreshold: StateFlow<Long> = _notificationThreshold.asStateFlow()

    private val _notificationLowTrafficMode = MutableStateFlow(0)
    val notificationLowTrafficMode: StateFlow<Int> = _notificationLowTrafficMode.asStateFlow()

    private val _notificationUseCustomColor = MutableStateFlow(false)
    val notificationUseCustomColor: StateFlow<Boolean> = _notificationUseCustomColor.asStateFlow()

    private val _notificationColor = MutableStateFlow(0)
    val notificationColor: StateFlow<Int> = _notificationColor.asStateFlow()

    private val _isHideFromRecents = MutableStateFlow(false)
    val isHideFromRecents: StateFlow<Boolean> = _isHideFromRecents.asStateFlow()

    private val _isOverlayUseDefaultColors = MutableStateFlow(false)
    val isOverlayUseDefaultColors: StateFlow<Boolean> = _isOverlayUseDefaultColors.asStateFlow()

    private val _isAutoStartServiceEnabled = MutableStateFlow(false)
    val isAutoStartServiceEnabled: StateFlow<Boolean> = _isAutoStartServiceEnabled.asStateFlow()

    private val _speedUnit = MutableStateFlow(0)
    val speedUnit: StateFlow<Int> = _speedUnit.asStateFlow()

    private val _speedRateUnit = MutableStateFlow(0)
    val speedRateUnit: StateFlow<Int> = _speedRateUnit.asStateFlow()

    private val _minSpeedUnit = MutableStateFlow(0)
    val minSpeedUnit: StateFlow<Int> = _minSpeedUnit.asStateFlow()

    private val _appThemeMode = MutableStateFlow(AppThemeMode.Dynamic.value)
    val appThemeMode: StateFlow<Int> = _appThemeMode.asStateFlow()

    private val _appThemeColor = MutableStateFlow(AppThemeMode.DEFAULT_THEME_COLOR)
    val appThemeColor: StateFlow<Int> = _appThemeColor.asStateFlow()

    private val _isAppThemeUseAmoledBlack = MutableStateFlow(false)
    val isAppThemeUseAmoledBlack: StateFlow<Boolean> =
        _isAppThemeUseAmoledBlack.asStateFlow()

    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val monitoringStateLock = Any()
    private var monitoringGeneration = 0L

    private var lastTotalRxBytes = 0L
    private var lastTotalTxBytes = 0L
    private var lastTime = 0L

    init {
        // Read all preferences in one file I/O operation instead of triggering repeated DataStore reads with first().
        runBlocking {
            dataStoreRepository.allPreferences.first().let { prefs ->
                val hasExistingPreferences = prefs.asMap().isNotEmpty()
                _isOnboardingShown.value =
                    prefs[DataStoreRepository.KEY_ONBOARDING_SHOWN] ?: hasExistingPreferences
                _isLiveUpdateEnabled.value = prefs[DataStoreRepository.KEY_LIVE_UPDATE] ?: false
                _isNotificationEnabled.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_ENABLED] ?: hasExistingPreferences
                _isOverlayLocked.value = prefs[DataStoreRepository.KEY_OVERLAY_LOCKED] ?: false
                _isOverlayShowOnStatusBar.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_SHOW_ON_STATUS_BAR] ?: false
                _isOverlayEnabled.value = prefs[DataStoreRepository.KEY_OVERLAY_ENABLED] ?: false
                _samplingInterval.value = prefs[DataStoreRepository.KEY_SAMPLING_INTERVAL] ?: 1500L
                _overlayBgColor.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_BG_COLOR] ?: 0xCC000000.toInt()
                _overlayTextColor.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_TEXT_COLOR] ?: 0xFFFFFFFF.toInt()
                _overlayCornerRadius.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_CORNER_RADIUS] ?: 8
                _overlayPadding.value = prefs[DataStoreRepository.KEY_OVERLAY_PADDING] ?: 8
                _overlayTextSize.value = prefs[DataStoreRepository.KEY_OVERLAY_TEXT_SIZE] ?: 10f
                _overlayTextUp.value = prefs[DataStoreRepository.KEY_OVERLAY_TEXT_UP] ?: "▲ "
                _overlayTextDown.value = prefs[DataStoreRepository.KEY_OVERLAY_TEXT_DOWN] ?: "▼ "
                _overlayOrderUpFirst.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_ORDER_UP_FIRST] ?: true
                _isOverlayHideBackground.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_HIDE_BACKGROUND] ?: false
                _overlayX.value = prefs[DataStoreRepository.KEY_OVERLAY_X] ?: 100
                _overlayY.value = prefs[DataStoreRepository.KEY_OVERLAY_Y] ?: 200
                _overlayDirection.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_DIRECTION] ?: 0
                _overlayDisplayMode.value = DataStoreRepository.normalizeOverlayDisplayMode(
                    prefs[DataStoreRepository.KEY_OVERLAY_DISPLAY_MODE] ?: 0
                )
                _overlayAlignment.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_ALIGNMENT] ?: 0
                _overlayMeterSpacing.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_METER_SPACING] ?: 8
                _isOverlayPortraitOnly.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_PORTRAIT_ONLY] ?: false
                _isOverlayHideInImmersiveMode.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_HIDE_IN_IMMERSIVE_MODE] ?: false
                _overlayAutoHideThreshold.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_AUTO_HIDE_THRESHOLD] ?: 0L
                _notificationTextUp.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_TEXT_UP] ?: "▲ "
                _notificationTextDown.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_TEXT_DOWN] ?: "▼ "
                _notificationOrderUpFirst.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_ORDER_UP_FIRST] ?: true
                _notificationDisplayMode.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_DISPLAY_MODE] ?: 0
                _notificationIconMode.value = DataStoreRepository.normalizeNotificationIconMode(
                    prefs[DataStoreRepository.KEY_NOTIFICATION_ICON_MODE] ?: 0
                )
                _notificationTextSize.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_TEXT_SIZE] ?: 0.65f
                _notificationUnitSize.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_UNIT_SIZE] ?: 0.35f
                _isHideFromRecents.value = prefs[DataStoreRepository.KEY_HIDE_FROM_RECENTS] ?: false
                _isOverlayUseDefaultColors.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_USE_DEFAULT_COLORS] ?: false
                _isAutoStartServiceEnabled.value =
                    prefs[DataStoreRepository.KEY_AUTO_START_SERVICE] ?: false
                _notificationThreshold.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_THRESHOLD] ?: 0L
                _notificationLowTrafficMode.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_LOW_TRAFFIC_MODE] ?: 0
                _notificationUseCustomColor.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_USE_CUSTOM_COLOR] ?: false
                _notificationColor.value = prefs[DataStoreRepository.KEY_NOTIFICATION_COLOR] ?: 0
                _speedUnit.value = prefs[DataStoreRepository.KEY_SPEED_UNIT] ?: 0
                _speedRateUnit.value = DataStoreRepository.normalizeSpeedRateUnit(
                    prefs[DataStoreRepository.KEY_SPEED_RATE_UNIT] ?: 0
                )
                _minSpeedUnit.value = prefs[DataStoreRepository.KEY_MIN_SPEED_UNIT] ?: 0
                _appThemeMode.value =
                    prefs[DataStoreRepository.KEY_APP_THEME_MODE] ?: AppThemeMode.Dynamic.value
                _appThemeColor.value =
                    prefs[DataStoreRepository.KEY_APP_THEME_COLOR]
                        ?: AppThemeMode.DEFAULT_THEME_COLOR
                _isAppThemeUseAmoledBlack.value =
                    prefs[DataStoreRepository.KEY_APP_THEME_USE_AMOLED_BLACK] ?: false
            }
        }
        scope.launch {
            dataStoreRepository.isOnboardingShown.collect { _isOnboardingShown.value = it }
        }
        scope.launch {
            dataStoreRepository.isLiveUpdateEnabled.collect { _isLiveUpdateEnabled.value = it }
        }
        scope.launch {
            dataStoreRepository.isNotificationEnabled.collect { _isNotificationEnabled.value = it }
        }
        scope.launch {
            dataStoreRepository.isOverlayLocked.collect { _isOverlayLocked.value = it }
        }
        scope.launch {
            dataStoreRepository.isOverlayShowOnStatusBar.collect {
                _isOverlayShowOnStatusBar.value = it
            }
        }
        scope.launch {
            dataStoreRepository.isOverlayEnabled.collect { _isOverlayEnabled.value = it }
        }
        scope.launch {
            dataStoreRepository.samplingInterval.collect { _samplingInterval.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayBgColor.collect { _overlayBgColor.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayTextColor.collect { _overlayTextColor.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayCornerRadius.collect { _overlayCornerRadius.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayPadding.collect { _overlayPadding.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayTextSize.collect { _overlayTextSize.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayTextUp.collect { _overlayTextUp.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayTextDown.collect { _overlayTextDown.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayOrderUpFirst.collect { _overlayOrderUpFirst.value = it }
        }
        scope.launch {
            dataStoreRepository.isOverlayHideBackground.collect {
                _isOverlayHideBackground.value = it
            }
        }
        scope.launch {
            dataStoreRepository.overlayX.collect { _overlayX.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayY.collect { _overlayY.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayDirection.collect { _overlayDirection.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayDisplayMode.collect { _overlayDisplayMode.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayAlignment.collect { _overlayAlignment.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayMeterSpacing.collect { _overlayMeterSpacing.value = it }
        }
        scope.launch {
            dataStoreRepository.isOverlayPortraitOnly.collect { _isOverlayPortraitOnly.value = it }
        }
        scope.launch {
            dataStoreRepository.isOverlayHideInImmersiveMode.collect {
                _isOverlayHideInImmersiveMode.value = it
            }
        }
        scope.launch {
            dataStoreRepository.overlayAutoHideThreshold.collect {
                _overlayAutoHideThreshold.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationTextUp.collect { _notificationTextUp.value = it }
        }
        scope.launch {
            dataStoreRepository.notificationTextDown.collect { _notificationTextDown.value = it }
        }
        scope.launch {
            dataStoreRepository.notificationOrderUpFirst.collect {
                _notificationOrderUpFirst.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationDisplayMode.collect {
                _notificationDisplayMode.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationIconMode.collect {
                _notificationIconMode.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationTextSize.collect {
                _notificationTextSize.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationUnitSize.collect {
                _notificationUnitSize.value = it
            }
        }
        scope.launch {
            dataStoreRepository.isHideFromRecents.collect {
                _isHideFromRecents.value = it
            }
        }
        scope.launch {
            dataStoreRepository.isOverlayUseDefaultColors.collect {
                _isOverlayUseDefaultColors.value = it
            }
        }
        scope.launch {
            dataStoreRepository.isAutoStartServiceEnabled.collect {
                _isAutoStartServiceEnabled.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationThreshold.collect {
                _notificationThreshold.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationLowTrafficMode.collect {
                _notificationLowTrafficMode.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationUseCustomColor.collect {
                _notificationUseCustomColor.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationColor.collect {
                _notificationColor.value = it
            }
        }
        scope.launch {
            dataStoreRepository.speedUnit.collect {
                _speedUnit.value = it
            }
        }
        scope.launch {
            dataStoreRepository.speedRateUnit.collect {
                _speedRateUnit.value = it
            }
        }
        scope.launch {
            dataStoreRepository.minSpeedUnit.collect {
                _minSpeedUnit.value = it
            }
        }
        scope.launch {
            dataStoreRepository.appThemeMode.collect {
                _appThemeMode.value = it
            }
        }
        scope.launch {
            dataStoreRepository.appThemeColor.collect {
                _appThemeColor.value = it
            }
        }
        scope.launch {
            dataStoreRepository.isAppThemeUseAmoledBlack.collect {
                _isAppThemeUseAmoledBlack.value = it
            }
        }
    }

    suspend fun markOnboardingShown() {
        dataStoreRepository.markOnboardingShown()
        _isOnboardingShown.value = true
    }

    suspend fun completeOnboarding(
        notificationEnabled: Boolean,
        liveUpdateEnabled: Boolean,
        overlayEnabled: Boolean
    ) {
        dataStoreRepository.completeOnboarding(
            notificationEnabled = notificationEnabled,
            liveUpdateEnabled = liveUpdateEnabled,
            overlayEnabled = overlayEnabled
        )
        _isOnboardingShown.value = true
        _isNotificationEnabled.value = notificationEnabled
        _isLiveUpdateEnabled.value = liveUpdateEnabled
        _isOverlayEnabled.value = overlayEnabled
    }

    fun setOverlayEnabled(enable: Boolean) {
        scope.launch { dataStoreRepository.setOverlayEnabled(enable) }
    }

    fun setLiveUpdateEnabled(enable: Boolean) {
        scope.launch { dataStoreRepository.setLiveUpdateEnabled(enable) }
    }

    fun setNotificationEnabled(enable: Boolean) {
        scope.launch { dataStoreRepository.setNotificationEnabled(enable) }
    }

    fun setOverlayLocked(locked: Boolean) {
        scope.launch { dataStoreRepository.setOverlayLocked(locked) }
    }

    fun setOverlayShowOnStatusBar(show: Boolean) {
        scope.launch { dataStoreRepository.setOverlayShowOnStatusBar(show) }
    }

    fun setSamplingInterval(interval: Long) {
        scope.launch { dataStoreRepository.setSamplingInterval(interval) }
    }

    fun setOverlayBgColor(color: Int) {
        scope.launch { dataStoreRepository.setOverlayBgColor(color) }
    }

    fun setOverlayTextColor(color: Int) {
        scope.launch { dataStoreRepository.setOverlayTextColor(color) }
    }

    fun setOverlayCornerRadius(radius: Int) {
        scope.launch { dataStoreRepository.setOverlayCornerRadius(radius) }
    }

    fun setOverlayPadding(padding: Int) {
        scope.launch { dataStoreRepository.setOverlayPadding(padding) }
    }

    fun setOverlayTextSize(size: Float) {
        scope.launch { dataStoreRepository.setOverlayTextSize(size) }
    }

    fun setOverlayTextUp(text: String) {
        scope.launch { dataStoreRepository.setOverlayTextUp(text) }
    }

    fun setOverlayTextDown(text: String) {
        scope.launch { dataStoreRepository.setOverlayTextDown(text) }
    }

    fun setOverlayOrderUpFirst(upFirst: Boolean) {
        scope.launch { dataStoreRepository.setOverlayOrderUpFirst(upFirst) }
    }

    fun setOverlayHideBackground(hideBackground: Boolean) {
        scope.launch { dataStoreRepository.setOverlayHideBackground(hideBackground) }
    }

    fun setOverlayDirection(direction: Int) {
        scope.launch { dataStoreRepository.setOverlayDirection(direction) }
    }

    fun setOverlayDisplayMode(mode: Int) {
        scope.launch { dataStoreRepository.setOverlayDisplayMode(mode) }
    }

    fun setOverlayAlignment(alignment: Int) {
        scope.launch { dataStoreRepository.setOverlayAlignment(alignment) }
    }

    fun setOverlayMeterSpacing(spacing: Int) {
        scope.launch { dataStoreRepository.setOverlayMeterSpacing(spacing) }
    }

    fun setOverlayPortraitOnly(portraitOnly: Boolean) {
        scope.launch { dataStoreRepository.setOverlayPortraitOnly(portraitOnly) }
    }

    fun setOverlayHideInImmersiveMode(hideInImmersiveMode: Boolean) {
        scope.launch {
            dataStoreRepository.setOverlayHideInImmersiveMode(hideInImmersiveMode)
        }
    }

    fun setOverlayAutoHideThreshold(threshold: Long) {
        scope.launch { dataStoreRepository.setOverlayAutoHideThreshold(threshold) }
    }

    fun setNotificationTextUp(text: String) {
        scope.launch { dataStoreRepository.setNotificationTextUp(text) }
    }

    fun setNotificationTextDown(text: String) {
        scope.launch { dataStoreRepository.setNotificationTextDown(text) }
    }

    fun setNotificationOrderUpFirst(upFirst: Boolean) {
        scope.launch { dataStoreRepository.setNotificationOrderUpFirst(upFirst) }
    }

    fun setNotificationDisplayMode(mode: Int) {
        scope.launch { dataStoreRepository.setNotificationDisplayMode(mode) }
    }

    fun setNotificationIconMode(mode: Int) {
        scope.launch { dataStoreRepository.setNotificationIconMode(mode) }
    }

    fun setNotificationTextSize(size: Float) {
        scope.launch { dataStoreRepository.setNotificationTextSize(size) }
    }

    fun setNotificationUnitSize(size: Float) {
        scope.launch { dataStoreRepository.setNotificationUnitSize(size) }
    }

    fun setHideFromRecents(hide: Boolean) {
        scope.launch { dataStoreRepository.setHideFromRecents(hide) }
    }

    fun setOverlayUseDefaultColors(useDefault: Boolean) {
        scope.launch { dataStoreRepository.setOverlayUseDefaultColors(useDefault) }
    }

    fun setAutoStartServiceEnabled(enabled: Boolean) {
        scope.launch { dataStoreRepository.setAutoStartServiceEnabled(enabled) }
    }

    fun setNotificationThreshold(threshold: Long) {
        scope.launch { dataStoreRepository.setNotificationThreshold(threshold) }
    }

    fun setNotificationLowTrafficMode(mode: Int) {
        scope.launch { dataStoreRepository.setNotificationLowTrafficMode(mode) }
    }

    fun setNotificationUseCustomColor(useCustom: Boolean) {
        scope.launch { dataStoreRepository.setNotificationUseCustomColor(useCustom) }
    }

    fun setNotificationColor(color: Int) {
        scope.launch { dataStoreRepository.setNotificationColor(color) }
    }

    fun setSpeedUnit(unit: Int) {
        scope.launch { dataStoreRepository.setSpeedUnit(unit) }
    }

    fun setSpeedRateUnit(rateUnit: Int) {
        scope.launch { dataStoreRepository.setSpeedRateUnit(rateUnit) }
    }

    fun setMinSpeedUnit(unit: Int) {
        scope.launch { dataStoreRepository.setMinSpeedUnit(unit) }
    }

    fun setAppThemeMode(mode: Int) {
        scope.launch { dataStoreRepository.setAppThemeMode(mode) }
    }

    fun setAppThemeColor(color: Int) {
        scope.launch { dataStoreRepository.setAppThemeColor(color) }
    }

    fun setAppThemeUseAmoledBlack(useAmoledBlack: Boolean) {
        scope.launch { dataStoreRepository.setAppThemeUseAmoledBlack(useAmoledBlack) }
    }

    suspend fun getOverlayPosition(): Pair<Int, Int> {
        val x = dataStoreRepository.overlayX.first()
        val y = dataStoreRepository.overlayY.first()
        return x to y
    }

    fun saveOverlayPosition(x: Int, y: Int) {
        scope.launch {
            dataStoreRepository.saveOverlayPosition(x, y)
        }
    }

    fun startMonitoring() {
        Log.i(TAG, "request start monitoring")
        if (monitoringJob?.isActive == true) return

        val generation = synchronized(monitoringStateLock) {
            monitoringGeneration += 1
            lastTotalRxBytes = 0L
            lastTotalTxBytes = 0L
            lastTime = 0L
            monitoringGeneration
        }

        _isMonitoring.value = true

        monitoringJob = scope.launch {
            Log.i(TAG, "startMonitoring")

            while (isActive) {
                val interval = _samplingInterval.value
                val startTime = SystemClock.elapsedRealtime()

                // Get Traffic Data
                val trafficData = dataSource.getTrafficData()

                val currentTime = SystemClock.elapsedRealtime()
                val totalRxBytes = trafficData.rxBytes
                val totalTxBytes = trafficData.txBytes

                withContext(Dispatchers.Default) {
                    synchronized(monitoringStateLock) {
                        if (generation != monitoringGeneration) return@synchronized

                        if (lastTime != 0L) {
                            val timeDelta = currentTime - lastTime
                            val rxDelta = totalRxBytes - lastTotalRxBytes
                            val txDelta = totalTxBytes - lastTotalTxBytes

                            if (timeDelta > 0) {
                                // Calculate speed
                                val downloadSpeed = ((rxDelta * 1000) / timeDelta).coerceAtLeast(0)
                                val uploadSpeed = ((txDelta * 1000) / timeDelta).coerceAtLeast(0)

                                val speed = NetSpeedData(
                                    downloadSpeed.coerceAtLeast(0),
                                    uploadSpeed.coerceAtLeast(0)
                                )
                                _netSpeed.value = speed
                                _speedSamples.tryEmit(speed)
                            }
                        }

                        lastTotalRxBytes = totalRxBytes
                        lastTotalTxBytes = totalTxBytes
                        lastTime = currentTime
                    }
                }

                // Delay to achieve the desired interval
                val delayMills = interval - (SystemClock.elapsedRealtime() - startTime)
                delay(delayMills.coerceAtLeast(0))
            }
        }
    }

    fun stopMonitoring() {
        Log.i(TAG, "request stop monitoring")
        monitoringJob?.cancel()
        monitoringJob = null
        _isMonitoring.value = false
        synchronized(monitoringStateLock) {
            monitoringGeneration += 1
            val stoppedSpeed = NetSpeedData(0, 0)
            _netSpeed.value = stoppedSpeed
            _speedSamples.tryEmit(stoppedSpeed)
        }
    }

    companion object {
        private const val TAG = "NetworkRepository"
    }
}
