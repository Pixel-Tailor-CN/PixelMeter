package vip.mystery0.pixel.meter.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import vip.mystery0.pixel.meter.MainActivity
import vip.mystery0.pixel.meter.R
import vip.mystery0.pixel.meter.data.model.AppThemeMode
import kotlin.math.roundToLong

@Composable
fun GeneralSettingsSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val interval by viewModel.samplingInterval.collectAsState(initial = 1500L)
    val speedUnit by viewModel.speedUnit.collectAsState(initial = 0)
    val speedRateUnit by viewModel.speedRateUnit.collectAsState(initial = 0)
    val hideOverlayAndIconSpeedSuffix by viewModel.hideOverlayAndIconSpeedSuffix.collectAsState()
    val minSpeedUnit by viewModel.minSpeedUnit.collectAsState(initial = 0)
    val appThemeMode by viewModel.appThemeMode.collectAsState(
        initial = AppThemeMode.Dynamic.value,
    )
    val appThemeColor by viewModel.appThemeColor.collectAsState(
        initial = AppThemeMode.DEFAULT_THEME_COLOR,
    )
    val useAmoledBlack by viewModel.isAppThemeUseAmoledBlack.collectAsState(initial = false)
    val isAutoStartEnabled by viewModel.isAutoStartServiceEnabled.collectAsState(initial = false)
    val canEnableAutoStart by viewModel.canEnableAutoStart.collectAsState()
    val hasOverlayPermission by viewModel.canOverlay.collectAsState()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()

    val rateBytes = stringResource(R.string.settings_speed_rate_unit_bytes)
    val rateBits = stringResource(R.string.settings_speed_rate_unit_bits)
    val rateUnitLabel = if (speedRateUnit == 1) rateBits else rateBytes

    val labelAuto = stringResource(R.string.settings_speed_unit_auto)
    val unitLabels = if (speedRateUnit == 1) {
        listOf("b/s", "kb/s", "Mb/s", "Gb/s")
    } else {
        listOf("B/s", "KB/s", "MB/s", "GB/s")
    }
    val speedUnitValues = listOf(labelAuto) + unitLabels
    val speedUnitLabel = speedUnitValues.getOrElse(speedUnit) { labelAuto }

    val labelNone = stringResource(R.string.settings_min_speed_unit_none)
    val minSpeedUnitValues = listOf(labelNone) + unitLabels.drop(1)
    val minSpeedUnitLabel = minSpeedUnitValues.getOrElse(minSpeedUnit) { labelNone }

    SettingsSection(title = stringResource(R.string.settings_section_speed_and_units)) {
        SettingsGroup(
            rows = listOf(
                SettingsRowItem("sampling-interval") { modifier ->
                    SettingsSliderRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_sampling_interval),
                        summary = stringResource(R.string.settings_sampling_interval_desc),
                        icon = Icons.Rounded.Speed,
                        value = interval.toFloat(),
                        onValueChange = { viewModel.setSamplingInterval(it.roundToLong()) },
                        valueRange = 1000f..3000f,
                        steps = 19,
                        valueText = "${interval}ms",
                    )
                },
                SettingsRowItem("speed-rate-unit") { modifier ->
                    SettingsChoiceRow(
                        modifier = modifier,
                        value = rateUnitLabel,
                        options = listOf(rateBytes, rateBits),
                        onValueChange = { viewModel.setSpeedRateUnit(if (it == rateBits) 1 else 0) },
                        title = stringResource(R.string.settings_speed_rate_unit_title),
                        summaryContent = {
                            ChoiceValueAndDescription(
                                value = rateUnitLabel,
                                description = stringResource(R.string.settings_speed_rate_unit_desc),
                            )
                        },
                        icon = Icons.Rounded.SwapVert,
                        optionLabel = { it },
                    )
                },
                SettingsRowItem("speed-unit") { modifier ->
                    SettingsChoiceRow(
                        modifier = modifier,
                        value = speedUnitLabel,
                        options = speedUnitValues,
                        onValueChange = {
                            viewModel.setSpeedUnit(speedUnitValues.indexOf(it).coerceAtLeast(0))
                        },
                        title = stringResource(R.string.settings_speed_unit_title),
                        summaryContent = {
                            ChoiceValueAndDescription(
                                value = speedUnitLabel,
                                description = stringResource(R.string.settings_speed_unit_desc),
                            )
                        },
                        icon = Icons.Rounded.Straighten,
                        optionLabel = { it },
                    )
                },
                SettingsRowItem("min-speed-unit") { modifier ->
                    SettingsChoiceRow(
                        modifier = modifier,
                        value = minSpeedUnitLabel,
                        options = minSpeedUnitValues,
                        onValueChange = {
                            viewModel.setMinSpeedUnit(
                                minSpeedUnitValues.indexOf(it).coerceAtLeast(0),
                            )
                        },
                        title = stringResource(R.string.settings_min_speed_unit_title),
                        summaryContent = {
                            ChoiceValueAndDescription(
                                value = minSpeedUnitLabel,
                                description = stringResource(R.string.settings_min_speed_unit_desc),
                            )
                        },
                        icon = Icons.Rounded.FilterAlt,
                        enabled = speedUnit == 0,
                        optionLabel = { it },
                    )
                },
                SettingsRowItem("hide-speed-suffix") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = hideOverlayAndIconSpeedSuffix,
                        onCheckedChange = { viewModel.setHideOverlayAndIconSpeedSuffix(it) },
                        title = stringResource(R.string.settings_hide_overlay_and_icon_speed_suffix_title),
                        summary = stringResource(R.string.settings_hide_overlay_and_icon_speed_suffix_desc),
                        icon = Icons.Rounded.TextFields,
                    )
                },
            ),
        )
    }

    val labelThemeDynamic = stringResource(R.string.settings_theme_mode_dynamic)
    val labelThemeFixed = stringResource(R.string.settings_theme_mode_fixed)
    val selectedThemeMode = AppThemeMode.fromValue(appThemeMode)
    val themeModeValues = listOf(labelThemeDynamic, labelThemeFixed)
    val themeModeLabel = when (selectedThemeMode) {
        AppThemeMode.Fixed -> labelThemeFixed
        AppThemeMode.Dynamic -> labelThemeDynamic
    }

    SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
        val appearanceRows = buildList {
            add(
                SettingsRowItem("theme-mode") { modifier ->
                    SettingsChoiceRow(
                        modifier = modifier,
                        value = themeModeLabel,
                        options = themeModeValues,
                        onValueChange = {
                            val mode = when (it) {
                                labelThemeFixed -> AppThemeMode.Fixed
                                else -> AppThemeMode.Dynamic
                            }
                            viewModel.setAppThemeMode(mode.value)
                        },
                        title = stringResource(R.string.settings_theme_mode_title),
                        summaryContent = {
                            ChoiceValueAndDescription(
                                value = themeModeLabel,
                                description = stringResource(R.string.settings_theme_mode_desc),
                            )
                        },
                        icon = Icons.Rounded.Palette,
                        optionLabel = { it },
                    )
                },
            )
            if (selectedThemeMode == AppThemeMode.Fixed) {
                add(
                    SettingsRowItem("theme-color") { modifier ->
                        ColorPreference(
                            modifier = modifier,
                            title = stringResource(R.string.settings_theme_color_title),
                            color = Color(appThemeColor).copy(alpha = 1f),
                            icon = Icons.Rounded.ColorLens,
                            showAlpha = false,
                            onColorSelected = {
                                viewModel.setAppThemeColor(it.copy(alpha = 1f).toArgb())
                            },
                        )
                    },
                )
                add(
                    SettingsRowItem("theme-amoled") { modifier ->
                        SettingsSwitchRow(
                            modifier = modifier,
                            checked = useAmoledBlack,
                            onCheckedChange = { viewModel.setAppThemeUseAmoledBlack(it) },
                            title = stringResource(R.string.settings_theme_amoled_black_title),
                            summary = stringResource(R.string.settings_theme_amoled_black_desc),
                            icon = Icons.Rounded.DarkMode,
                        )
                    },
                )
            }
        }
        SettingsGroup(rows = appearanceRows)
    }

    val autoStartSummary = if (canEnableAutoStart) {
        stringResource(R.string.settings_auto_start_service_desc)
    } else {
        stringResource(R.string.settings_auto_start_disabled_reason)
    }
    val overlayPermissionSummary = stringResource(
        if (hasOverlayPermission) {
            R.string.settings_permission_granted
        } else {
            R.string.settings_permission_denied
        },
    )
    val notificationPermissionSummary = stringResource(
        if (hasNotificationPermission) {
            R.string.settings_permission_granted
        } else {
            R.string.settings_permission_denied
        },
    )

    SettingsSection(title = stringResource(R.string.settings_section_startup_and_permissions)) {
        SettingsGroup(
            rows = listOf(
                SettingsRowItem("run-onboarding") { modifier ->
                    SettingsItemRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_run_onboarding_title),
                        summary = stringResource(R.string.settings_run_onboarding_desc),
                        icon = Icons.Rounded.AutoFixHigh,
                        onClick = {
                            context.startActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    putExtra(MainActivity.EXTRA_OPEN_ONBOARDING, true)
                                },
                            )
                        },
                    )
                },
                SettingsRowItem("auto-start-service") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = isAutoStartEnabled,
                        onCheckedChange = { viewModel.setAutoStartServiceEnabled(it) },
                        enabled = canEnableAutoStart,
                        title = stringResource(R.string.settings_auto_start_service_title),
                        summary = autoStartSummary,
                        icon = Icons.Rounded.PowerSettingsNew,
                    )
                },
                SettingsRowItem("permission-overlay") { modifier ->
                    SettingsItemRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_permission_overlay),
                        summary = overlayPermissionSummary,
                        icon = Icons.Rounded.Layers,
                        trailing = SettingsRowTrailing.EXTERNAL_LINK,
                        onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            intent.data = "package:${context.packageName}".toUri()
                            context.startActivity(intent)
                        },
                    )
                },
                SettingsRowItem("permission-notification") { modifier ->
                    SettingsItemRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_permission_notification),
                        summary = notificationPermissionSummary,
                        icon = Icons.Rounded.NotificationsActive,
                        trailing = SettingsRowTrailing.EXTERNAL_LINK,
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            context.startActivity(intent)
                        },
                    )
                },
            ),
        )
    }
}

@Composable
private fun ChoiceValueAndDescription(
    value: String,
    description: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
