package vip.mystery0.pixel.meter.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vip.mystery0.pixel.meter.data.model.AppThemeMode

const val DATA_STORE_NAME = "pixel_pulse_preferences"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_NAME)

class DataStoreRepository(private val dataStore: DataStore<Preferences>) {

    /** Exposes the raw Preferences Flow for reading initial values in one batch. */
    val allPreferences: Flow<Preferences> = dataStore.data

    // Keys mapped from legacy SharedPreferences in NetworkRepository.kt
    companion object {
        val KEY_LIVE_UPDATE = booleanPreferencesKey("key_live_update")
        val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("key_notification_enabled")
        val KEY_OVERLAY_ENABLED = booleanPreferencesKey("key_overlay_enabled")
        val KEY_OVERLAY_LOCKED = booleanPreferencesKey("key_overlay_locked")
        val KEY_OVERLAY_SHOW_ON_STATUS_BAR = booleanPreferencesKey("key_overlay_show_on_status_bar")
        val KEY_OVERLAY_X = intPreferencesKey("key_overlay_x")
        val KEY_OVERLAY_Y = intPreferencesKey("key_overlay_y")

        val KEY_SAMPLING_INTERVAL = longPreferencesKey("key_sampling_interval")
        val KEY_OVERLAY_BG_COLOR = intPreferencesKey("key_overlay_bg_color")
        val KEY_OVERLAY_TEXT_COLOR = intPreferencesKey("key_overlay_text_color")
        val KEY_OVERLAY_CORNER_RADIUS = intPreferencesKey("key_overlay_corner_radius")
        val KEY_OVERLAY_PADDING = intPreferencesKey("key_overlay_padding")
        val KEY_OVERLAY_TEXT_SIZE = floatPreferencesKey("key_overlay_text_size")
        val KEY_OVERLAY_TEXT_UP = stringPreferencesKey("key_overlay_text_up")
        val KEY_OVERLAY_TEXT_DOWN = stringPreferencesKey("key_overlay_text_down")
        val KEY_OVERLAY_ORDER_UP_FIRST = booleanPreferencesKey("key_overlay_order_up_first")
        val KEY_OVERLAY_HIDE_BACKGROUND = booleanPreferencesKey("key_overlay_hide_background")
        val KEY_NOTIFICATION_TEXT_UP = stringPreferencesKey("key_notification_text_up")
        val KEY_NOTIFICATION_TEXT_DOWN = stringPreferencesKey("key_notification_text_down")
        val KEY_NOTIFICATION_ORDER_UP_FIRST =
            booleanPreferencesKey("key_notification_order_up_first")
        val KEY_NOTIFICATION_DISPLAY_MODE = intPreferencesKey("key_notification_display_mode")
        val KEY_NOTIFICATION_TEXT_SIZE = floatPreferencesKey("key_notification_text_size")
        val KEY_NOTIFICATION_UNIT_SIZE = floatPreferencesKey("key_notification_unit_size")

        val KEY_HIDE_FROM_RECENTS = booleanPreferencesKey("key_hide_from_recents")
        val KEY_OVERLAY_USE_DEFAULT_COLORS = booleanPreferencesKey("key_overlay_use_default_colors")
        val KEY_AUTO_START_SERVICE = booleanPreferencesKey("key_auto_start_service")
        val KEY_NOTIFICATION_THRESHOLD = longPreferencesKey("key_notification_threshold")
        val KEY_NOTIFICATION_LOW_TRAFFIC_MODE =
            intPreferencesKey("key_notification_low_traffic_mode")
        val KEY_NOTIFICATION_USE_CUSTOM_COLOR =
            booleanPreferencesKey("key_notification_use_custom_color")
        val KEY_NOTIFICATION_COLOR = intPreferencesKey("key_notification_color")
        val KEY_SPEED_UNIT = intPreferencesKey("key_speed_unit")
        val KEY_MIN_SPEED_UNIT = intPreferencesKey("key_min_speed_unit")
        val KEY_APP_THEME_MODE = intPreferencesKey("key_app_theme_mode")
        val KEY_APP_THEME_COLOR = intPreferencesKey("key_app_theme_color")
        val KEY_APP_THEME_USE_AMOLED_BLACK =
            booleanPreferencesKey("key_app_theme_use_amoled_black")
        val KEY_OVERLAY_DIRECTION = intPreferencesKey("key_overlay_direction")
        val KEY_OVERLAY_ALIGNMENT = intPreferencesKey("key_overlay_alignment")
        val KEY_OVERLAY_METER_SPACING = intPreferencesKey("key_overlay_meter_spacing")
        val KEY_OVERLAY_PORTRAIT_ONLY = booleanPreferencesKey("key_overlay_portrait_only")
        val KEY_OVERLAY_HIDE_IN_IMMERSIVE_MODE =
            booleanPreferencesKey("key_overlay_hide_in_immersive_mode")
        val KEY_OVERLAY_AUTO_HIDE_THRESHOLD =
            longPreferencesKey("key_overlay_auto_hide_threshold")
        val KEY_ONBOARDING_SHOWN = booleanPreferencesKey("key_onboarding_shown")
    }

    val isOnboardingShown: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_ONBOARDING_SHOWN] ?: preferences.asMap().isNotEmpty()
        }

    val isLiveUpdateEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_LIVE_UPDATE] ?: false
        }

    val isNotificationEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_ENABLED] ?: preferences.asMap().isNotEmpty()
        }

    val isOverlayEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_ENABLED] ?: false
        }

    val isOverlayLocked: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_LOCKED] ?: false
        }

    val isOverlayShowOnStatusBar: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_SHOW_ON_STATUS_BAR] ?: false
        }

    val overlayX: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_X] ?: 100
        }

    val overlayY: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_Y] ?: 200
        }

    val samplingInterval: Flow<Long> = dataStore.data
        .map { preferences ->
            preferences[KEY_SAMPLING_INTERVAL] ?: 1500L
        }

    val overlayBgColor: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_BG_COLOR]
                ?: 0xCC000000.toInt() // Default semi-transparent black
        }

    val overlayTextColor: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_TEXT_COLOR]
                ?: 0xFFFFFFFF.toInt() // Default white
        }

    val overlayCornerRadius: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_CORNER_RADIUS] ?: 8
        }

    val overlayPadding: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_PADDING] ?: 8
        }

    val overlayTextSize: Flow<Float> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_TEXT_SIZE] ?: 10f
        }

    val overlayTextUp: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_TEXT_UP] ?: "▲ "
        }

    val overlayTextDown: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_TEXT_DOWN] ?: "▼ "
        }

    val overlayOrderUpFirst: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_ORDER_UP_FIRST] ?: true // Default TRUE as requested
        }

    val isOverlayHideBackground: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_HIDE_BACKGROUND] ?: false
        }

    val overlayDirection: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_DIRECTION] ?: 0 // 0: Horizontal, 1: Vertical
        }

    val overlayAlignment: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_ALIGNMENT] ?: 0 // 0: Start, 1: Center, 2: End
        }

    val overlayMeterSpacing: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_METER_SPACING] ?: 8
        }

    val notificationTextUp: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_TEXT_UP] ?: "▲ "
        }

    val notificationTextDown: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_TEXT_DOWN] ?: "▼ "
        }

    val notificationOrderUpFirst: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_ORDER_UP_FIRST] ?: true // Default TRUE as requested
        }

    val notificationDisplayMode: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_DISPLAY_MODE] ?: 0 // 0: Total, 1: Up, 2: Down
        }

    val notificationTextSize: Flow<Float> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_TEXT_SIZE] ?: 0.65f
        }

    val notificationUnitSize: Flow<Float> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_UNIT_SIZE] ?: 0.35f
        }

    val notificationThreshold: Flow<Long> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_THRESHOLD] ?: 0L
        }

    val notificationLowTrafficMode: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_LOW_TRAFFIC_MODE] ?: 0 // 0: Static, 1: Dynamic
        }

    val notificationUseCustomColor: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_USE_CUSTOM_COLOR] ?: false
        }

    val notificationColor: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATION_COLOR] ?: 0
        }

    val overlayAutoHideThreshold: Flow<Long> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_AUTO_HIDE_THRESHOLD] ?: 0L
        }

    val appThemeMode: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_APP_THEME_MODE] ?: AppThemeMode.Dynamic.value
        }

    val appThemeColor: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_APP_THEME_COLOR] ?: AppThemeMode.DEFAULT_THEME_COLOR
        }

    val isAppThemeUseAmoledBlack: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_APP_THEME_USE_AMOLED_BLACK] ?: false
        }

    suspend fun markOnboardingShown() {
        dataStore.edit { preferences ->
            if (preferences.asMap().isEmpty()) {
                preferences[KEY_NOTIFICATION_ENABLED] = false
            }
            preferences[KEY_ONBOARDING_SHOWN] = true
        }
    }

    suspend fun completeOnboarding(
        notificationEnabled: Boolean,
        liveUpdateEnabled: Boolean,
        overlayEnabled: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_SHOWN] = true
            preferences[KEY_NOTIFICATION_ENABLED] = notificationEnabled
            preferences[KEY_LIVE_UPDATE] = liveUpdateEnabled
            preferences[KEY_OVERLAY_ENABLED] = overlayEnabled
        }
    }

    suspend fun setLiveUpdateEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_LIVE_UPDATE] = enabled
        }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setOverlayLocked(locked: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_LOCKED] = locked
        }
    }

    suspend fun setOverlayShowOnStatusBar(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_SHOW_ON_STATUS_BAR] = show
        }
    }

    suspend fun saveOverlayPosition(x: Int, y: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_X] = x
            preferences[KEY_OVERLAY_Y] = y
        }
    }

    suspend fun setSamplingInterval(interval: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_SAMPLING_INTERVAL] = interval
        }
    }

    suspend fun setOverlayBgColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_BG_COLOR] = color
        }
    }

    suspend fun setOverlayTextColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_TEXT_COLOR] = color
        }
    }

    suspend fun setOverlayCornerRadius(radius: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_CORNER_RADIUS] = radius
        }
    }

    suspend fun setOverlayPadding(padding: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_PADDING] = padding
        }
    }

    suspend fun setOverlayTextSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_TEXT_SIZE] = size
        }
    }

    suspend fun setOverlayTextUp(text: String) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_TEXT_UP] = text
        }
    }

    suspend fun setOverlayTextDown(text: String) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_TEXT_DOWN] = text
        }
    }

    suspend fun setOverlayOrderUpFirst(upFirst: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_ORDER_UP_FIRST] = upFirst
        }
    }

    suspend fun setOverlayHideBackground(hideBackground: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_HIDE_BACKGROUND] = hideBackground
        }
    }

    suspend fun setOverlayDirection(direction: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_DIRECTION] = direction
        }
    }

    suspend fun setOverlayAlignment(alignment: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_ALIGNMENT] = alignment
        }
    }

    suspend fun setOverlayMeterSpacing(spacing: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_METER_SPACING] = spacing
        }
    }

    suspend fun setNotificationTextUp(text: String) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_TEXT_UP] = text
        }
    }

    suspend fun setNotificationTextDown(text: String) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_TEXT_DOWN] = text
        }
    }

    suspend fun setNotificationOrderUpFirst(upFirst: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_ORDER_UP_FIRST] = upFirst
        }
    }

    suspend fun setNotificationDisplayMode(mode: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_DISPLAY_MODE] = mode
        }
    }

    suspend fun setNotificationTextSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_TEXT_SIZE] = size
        }
    }

    suspend fun setNotificationUnitSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_UNIT_SIZE] = size
        }
    }

    suspend fun setNotificationThreshold(threshold: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_THRESHOLD] = threshold
        }
    }

    suspend fun setNotificationLowTrafficMode(mode: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_LOW_TRAFFIC_MODE] = mode
        }
    }

    suspend fun setNotificationUseCustomColor(useCustom: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_USE_CUSTOM_COLOR] = useCustom
        }
    }

    suspend fun setNotificationColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_COLOR] = color
        }
    }

    suspend fun setOverlayAutoHideThreshold(threshold: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_AUTO_HIDE_THRESHOLD] = threshold
        }
    }

    suspend fun setAppThemeMode(mode: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_THEME_MODE] = AppThemeMode.fromValue(mode).value
        }
    }

    suspend fun setAppThemeColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_THEME_COLOR] = color
        }
    }

    suspend fun setAppThemeUseAmoledBlack(useAmoledBlack: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_THEME_USE_AMOLED_BLACK] = useAmoledBlack
        }
    }

    val isHideFromRecents: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_HIDE_FROM_RECENTS] ?: false
        }

    suspend fun setHideFromRecents(hide: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HIDE_FROM_RECENTS] = hide
        }
    }

    val isOverlayUseDefaultColors: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_USE_DEFAULT_COLORS] ?: false
        }

    suspend fun setOverlayUseDefaultColors(useDefault: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_USE_DEFAULT_COLORS] = useDefault
        }
    }

    val isAutoStartServiceEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_AUTO_START_SERVICE] ?: false
        }

    suspend fun setAutoStartServiceEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_START_SERVICE] = enabled
        }
    }

    val speedUnit: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_SPEED_UNIT] ?: 0
        }

    suspend fun setSpeedUnit(unit: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_SPEED_UNIT] = unit
        }
    }

    val minSpeedUnit: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[KEY_MIN_SPEED_UNIT] ?: 0
        }

    suspend fun setMinSpeedUnit(unit: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_MIN_SPEED_UNIT] = unit
        }
    }

    val isOverlayPortraitOnly: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_PORTRAIT_ONLY] ?: false
        }

    suspend fun setOverlayPortraitOnly(portraitOnly: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_PORTRAIT_ONLY] = portraitOnly
        }
    }

    val isOverlayHideInImmersiveMode: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_OVERLAY_HIDE_IN_IMMERSIVE_MODE] ?: false
        }

    suspend fun setOverlayHideInImmersiveMode(hideInImmersiveMode: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_HIDE_IN_IMMERSIVE_MODE] = hideInImmersiveMode
        }
    }
}
