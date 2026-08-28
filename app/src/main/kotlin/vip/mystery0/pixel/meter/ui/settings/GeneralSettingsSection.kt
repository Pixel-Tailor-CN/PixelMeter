package vip.mystery0.pixel.meter.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import vip.mystery0.pixel.meter.MainActivity
import vip.mystery0.pixel.meter.R
import vip.mystery0.pixel.meter.data.model.AppThemeMode

@Composable
fun GeneralSettingsSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val interval by viewModel.samplingInterval.collectAsState(initial = 1500L)
    val speedUnit by viewModel.speedUnit.collectAsState(initial = 0)
    val speedRateUnit by viewModel.speedRateUnit.collectAsState(initial = 0)
    val minSpeedUnit by viewModel.minSpeedUnit.collectAsState(initial = 0)
    val appThemeMode by viewModel.appThemeMode.collectAsState(
        initial = AppThemeMode.Dynamic.value
    )
    val appThemeColor by viewModel.appThemeColor.collectAsState(
        initial = AppThemeMode.DEFAULT_THEME_COLOR
    )
    val useAmoledBlack by viewModel.isAppThemeUseAmoledBlack.collectAsState(initial = false)
    val isAutoStartEnabled by viewModel.isAutoStartServiceEnabled.collectAsState(initial = false)
    val canEnableAutoStart by viewModel.canEnableAutoStart.collectAsState()
    val hasOverlayPermission by viewModel.canOverlay.collectAsState()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()

    PreferenceCategory(title = { Text(stringResource(R.string.settings_category_general)) })

    SliderPreference(
        value = 0F,
        onValueChange = { },
        sliderValue = interval.toFloat(),
        onSliderValueChange = { viewModel.setSamplingInterval(it.toLong()) },
        valueRange = 1000f..3000f,
        valueSteps = 19,
        title = { Text(stringResource(R.string.settings_sampling_interval)) },
        summary = { Text(stringResource(R.string.settings_sampling_interval_desc)) },
        valueText = { Text("${interval}ms") }
    )

    val rateBytes = stringResource(R.string.settings_speed_rate_unit_bytes)
    val rateBits = stringResource(R.string.settings_speed_rate_unit_bits)
    val rateUnitLabel = if (speedRateUnit == 1) rateBits else rateBytes
    ListPreference(
        value = rateUnitLabel,
        onValueChange = { viewModel.setSpeedRateUnit(if (it == rateBits) 1 else 0) },
        title = { Text(stringResource(R.string.settings_speed_rate_unit_title)) },
        values = listOf(rateBytes, rateBits),
        summary = { Text(stringResource(R.string.settings_speed_rate_unit_desc)) }
    )

    val labelAuto = stringResource(R.string.settings_speed_unit_auto)
    val unitLabels = if (speedRateUnit == 1) {
        listOf("b/s", "kb/s", "Mb/s", "Gb/s")
    } else {
        listOf("B/s", "KB/s", "MB/s", "GB/s")
    }
    val speedUnitValues = listOf(labelAuto) + unitLabels
    val speedUnitLabel = speedUnitValues.getOrElse(speedUnit) { labelAuto }
    ListPreference(
        value = speedUnitLabel,
        onValueChange = { viewModel.setSpeedUnit(speedUnitValues.indexOf(it).coerceAtLeast(0)) },
        title = { Text(stringResource(R.string.settings_speed_unit_title)) },
        values = speedUnitValues,
        summary = { Text(stringResource(R.string.settings_speed_unit_desc)) }
    )

    val labelNone = stringResource(R.string.settings_min_speed_unit_none)
    val minSpeedUnitValues = listOf(labelNone) + unitLabels.drop(1)
    val minSpeedUnitLabel = minSpeedUnitValues.getOrElse(minSpeedUnit) { labelNone }
    ListPreference(
        value = minSpeedUnitLabel,
        onValueChange = { viewModel.setMinSpeedUnit(minSpeedUnitValues.indexOf(it).coerceAtLeast(0)) },
        title = { Text(stringResource(R.string.settings_min_speed_unit_title)) },
        values = minSpeedUnitValues,
        summary = { Text(stringResource(R.string.settings_min_speed_unit_desc)) },
        enabled = speedUnit == 0
    )

    val labelThemeDynamic = stringResource(R.string.settings_theme_mode_dynamic)
    val labelThemeFixed = stringResource(R.string.settings_theme_mode_fixed)
    val selectedThemeMode = AppThemeMode.fromValue(appThemeMode)
    val themeModeValues = listOf(labelThemeDynamic, labelThemeFixed)
    val themeModeLabel = when (selectedThemeMode) {
        AppThemeMode.Fixed -> labelThemeFixed
        AppThemeMode.Dynamic -> labelThemeDynamic
    }
    ListPreference(
        value = themeModeLabel,
        onValueChange = {
            val mode = when (it) {
                labelThemeFixed -> AppThemeMode.Fixed
                else -> AppThemeMode.Dynamic
            }
            viewModel.setAppThemeMode(mode.value)
        },
        title = { Text(stringResource(R.string.settings_theme_mode_title)) },
        values = themeModeValues,
        summary = { Text(stringResource(R.string.settings_theme_mode_desc)) }
    )
    if (selectedThemeMode == AppThemeMode.Fixed) {
        ColorPreference(
            title = stringResource(R.string.settings_theme_color_title),
            color = Color(appThemeColor).copy(alpha = 1f),
            showAlpha = false,
            onColorSelected = {
                viewModel.setAppThemeColor(it.copy(alpha = 1f).toArgb())
            }
        )
        SwitchPreference(
            value = useAmoledBlack,
            onValueChange = { viewModel.setAppThemeUseAmoledBlack(it) },
            title = { Text(stringResource(R.string.settings_theme_amoled_black_title)) },
            summary = { Text(stringResource(R.string.settings_theme_amoled_black_desc)) }
        )
    }

    Preference(
        title = { Text(stringResource(R.string.settings_run_onboarding_title)) },
        summary = { Text(stringResource(R.string.settings_run_onboarding_desc)) },
        onClick = {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(MainActivity.EXTRA_OPEN_ONBOARDING, true)
                }
            )
        }
    )

    val autoStartSummary = if (canEnableAutoStart) {
        stringResource(R.string.settings_auto_start_service_desc)
    } else {
        stringResource(R.string.settings_auto_start_disabled_reason)
    }

    SwitchPreference(
        value = isAutoStartEnabled,
        onValueChange = { viewModel.setAutoStartServiceEnabled(it) },
        enabled = canEnableAutoStart,
        title = { Text(stringResource(R.string.settings_auto_start_service_title)) },
        summary = { Text(autoStartSummary) }
    )

    val overlayPermissionSummary = if (hasOverlayPermission) {
        stringResource(R.string.settings_permission_granted)
    } else {
        stringResource(R.string.settings_permission_denied)
    }
    Preference(
        title = { Text(stringResource(R.string.settings_permission_overlay)) },
        summary = { Text(overlayPermissionSummary) },
        onClick = {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = "package:${context.packageName}".toUri()
            context.startActivity(intent)
        }
    )

    val notificationPermissionSummary = if (hasNotificationPermission) {
        stringResource(R.string.settings_permission_granted)
    } else {
        stringResource(R.string.settings_permission_denied)
    }
    Preference(
        title = { Text(stringResource(R.string.settings_permission_notification)) },
        summary = { Text(notificationPermissionSummary) },
        onClick = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            context.startActivity(intent)
        }
    )
}
