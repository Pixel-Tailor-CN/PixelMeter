package vip.mystery0.pixel.meter.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.LowPriority
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.meter.R
import vip.mystery0.pixel.meter.format.SpeedFormatter
import kotlin.math.roundToLong

@Composable
fun NotificationSettingsSection(viewModel: SettingsViewModel) {
    val isEnabled by viewModel.isNotificationEnabled.collectAsState(initial = false)
    val isLiveUpdateEnabled by viewModel.isLiveUpdateEnabled.collectAsState(initial = false)
    val textUp by viewModel.notificationTextUp.collectAsState(initial = "\u25B2 ")
    val textDown by viewModel.notificationTextDown.collectAsState(initial = "\u25BC ")
    val upFirst by viewModel.notificationOrderUpFirst.collectAsState(initial = true)
    val displayMode by viewModel.notificationDisplayMode.collectAsState(initial = 0)
    val iconMode by viewModel.notificationIconMode.collectAsState(initial = 0)
    val speedRateUnit by viewModel.speedRateUnit.collectAsState(initial = 0)
    val hideLiveUpdateSpeedSuffix by viewModel.hideLiveUpdateSpeedSuffix.collectAsState()
    val textSize by viewModel.notificationTextSize.collectAsState(initial = 0.65f)
    val unitSize by viewModel.notificationUnitSize.collectAsState(initial = 0.35f)
    val threshold by viewModel.notificationThreshold.collectAsState(initial = 0L)
    val lowTrafficMode by viewModel.notificationLowTrafficMode.collectAsState(initial = 0)
    val useCustomColor by viewModel.notificationUseCustomColor.collectAsState(initial = false)
    val notificationColor by viewModel.notificationColor.collectAsState(initial = 0)
    val platformLocale = LocalLocale.current.platformLocale

    SettingsSection(title = stringResource(R.string.settings_section_basic)) {
        val basicRows = buildList {
            add(
                SettingsRowItem("notification-enabled") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = isEnabled,
                        onCheckedChange = { viewModel.setNotificationEnabled(it) },
                        title = stringResource(R.string.config_enable_notification),
                        summary = stringResource(R.string.config_enable_notification_desc),
                        icon = Icons.Rounded.Notifications,
                    )
                },
            )
            if (isEnabled) {
                add(
                    SettingsRowItem("notification-live-update") { modifier ->
                        SettingsSwitchRow(
                            modifier = modifier,
                            checked = isLiveUpdateEnabled,
                            onCheckedChange = { viewModel.setLiveUpdateEnabled(it) },
                            title = stringResource(R.string.config_enable_live_update),
                            summary = stringResource(R.string.config_enable_live_update_desc),
                            icon = Icons.Rounded.Bolt,
                        )
                    },
                )
                if (isLiveUpdateEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    add(
                        SettingsRowItem("notification-hide-live-update-suffix") { modifier ->
                            SettingsSwitchRow(
                                modifier = modifier,
                                checked = hideLiveUpdateSpeedSuffix,
                                onCheckedChange = { viewModel.setHideLiveUpdateSpeedSuffix(it) },
                                title = stringResource(R.string.settings_hide_live_update_speed_suffix_title),
                                summary = stringResource(R.string.settings_hide_live_update_speed_suffix_desc),
                                icon = Icons.Rounded.TextFields,
                            )
                        },
                    )
                }
            }
        }
        SettingsGroup(rows = basicRows)
    }

    if (!isEnabled) return

    val labelBoth = stringResource(R.string.settings_display_mode_both)
    val labelTotal = stringResource(R.string.settings_display_mode_total)
    val labelUpload = stringResource(R.string.settings_display_mode_upload)
    val labelDownload = stringResource(R.string.settings_display_mode_download)
    val displayModeLabel = when (displayMode) {
        1 -> labelUpload
        2 -> labelDownload
        else -> labelBoth
    }

    SettingsSection(title = stringResource(R.string.settings_section_content_and_order)) {
        SettingsGroup(
            rows = listOf(
                SettingsRowItem("notification-display-mode") { modifier ->
                    SettingsChoiceRow(
                        modifier = modifier,
                        value = displayModeLabel,
                        options = listOf(labelBoth, labelUpload, labelDownload),
                        onValueChange = {
                            val mode = when (it) {
                                labelUpload -> 1
                                labelDownload -> 2
                                else -> 0
                            }
                            viewModel.setNotificationDisplayMode(mode)
                        },
                        title = stringResource(R.string.settings_notification_display_mode),
                        summary = displayModeLabel,
                        icon = Icons.Rounded.Tune,
                        optionLabel = { it },
                    )
                },
                SettingsRowItem("notification-text-up") { modifier ->
                    SettingsTextFieldRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_text_prefix_up),
                        summary = stringResource(R.string.settings_text_prefix_up_desc, textUp),
                        value = textUp,
                        onValueChange = { viewModel.setNotificationTextUp(it) },
                        icon = Icons.Rounded.ArrowUpward,
                    )
                },
                SettingsRowItem("notification-text-down") { modifier ->
                    SettingsTextFieldRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_text_prefix_down),
                        summary = stringResource(R.string.settings_text_prefix_down_desc, textDown),
                        value = textDown,
                        onValueChange = { viewModel.setNotificationTextDown(it) },
                        icon = Icons.Rounded.ArrowDownward,
                    )
                },
                SettingsRowItem("notification-up-first") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = upFirst,
                        onCheckedChange = { viewModel.setNotificationOrderUpFirst(it) },
                        title = stringResource(R.string.settings_show_up_first),
                        summary = stringResource(R.string.settings_show_up_first_desc),
                        icon = Icons.Rounded.SwapVert,
                    )
                },
            ),
        )
    }

    val iconModeValues = listOf(labelTotal, labelUpload, labelDownload)
    val iconModeLabel = iconModeValues.getOrElse(iconMode) { labelTotal }

    SettingsSection(title = stringResource(R.string.settings_section_status_bar_icon)) {
        SettingsGroup(
            rows = listOf(
                SettingsRowItem("notification-icon-mode") { modifier ->
                    SettingsChoiceRow(
                        modifier = modifier,
                        value = iconModeLabel,
                        options = iconModeValues,
                        onValueChange = {
                            viewModel.setNotificationIconMode(
                                iconModeValues.indexOf(it).coerceAtLeast(0),
                            )
                        },
                        title = stringResource(R.string.settings_notification_icon_mode),
                        summary = iconModeLabel,
                        icon = Icons.Rounded.SignalCellularAlt,
                        optionLabel = { it },
                    )
                },
                SettingsRowItem("notification-text-size") { modifier ->
                    SettingsSliderRow(
                        modifier = modifier,
                        enabled = !isLiveUpdateEnabled,
                        title = stringResource(R.string.settings_notification_text_size),
                        icon = Icons.Rounded.FormatSize,
                        value = textSize,
                        onValueChange = { viewModel.setNotificationTextSize(it) },
                        valueRange = 0.1f..1.0f,
                        valueText = "%.2f".format(platformLocale, textSize),
                    )
                },
                SettingsRowItem("notification-unit-size") { modifier ->
                    SettingsSliderRow(
                        modifier = modifier,
                        enabled = !isLiveUpdateEnabled,
                        title = stringResource(R.string.settings_notification_unit_size),
                        icon = Icons.Rounded.TextDecrease,
                        value = unitSize,
                        onValueChange = { viewModel.setNotificationUnitSize(it) },
                        valueRange = 0.1f..1.0f,
                        valueText = "%.2f".format(platformLocale, unitSize),
                    )
                },
                SettingsRowItem("notification-use-custom-color") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = useCustomColor,
                        onCheckedChange = { viewModel.setNotificationUseCustomColor(it) },
                        title = stringResource(R.string.settings_notification_use_custom_color_title),
                        summary = stringResource(R.string.settings_notification_use_custom_color_desc),
                        icon = Icons.Rounded.FormatColorFill,
                    )
                },
                SettingsRowItem("notification-color") { modifier ->
                    ColorPreference(
                        modifier = modifier,
                        title = stringResource(R.string.settings_notification_color_title),
                        color = Color(notificationColor),
                        icon = Icons.Rounded.ColorLens,
                        enabled = useCustomColor,
                        onColorSelected = { viewModel.setNotificationColor(it.toArgb()) },
                    )
                },
            ),
        )
    }

    val thresholdUnit = if (speedRateUnit == 1) "kb/s" else "KB/s"
    val thresholdText = SpeedFormatter.formatSpeedLine(threshold, rateUnit = speedRateUnit)
    val thresholdSummary = if (threshold == 0L) {
        stringResource(R.string.settings_notification_threshold_disabled)
    } else {
        stringResource(R.string.settings_notification_threshold_desc, thresholdText)
    }

    val labelStatic = stringResource(R.string.settings_notification_low_traffic_mode_static)
    val labelDynamic = stringResource(R.string.settings_notification_low_traffic_mode_dynamic)
    val lowTrafficModeLabel = when (lowTrafficMode) {
        1 -> labelDynamic
        else -> labelStatic
    }

    SettingsSection(title = stringResource(R.string.settings_section_low_traffic)) {
        SettingsGroup(
            rows = listOf(
                SettingsRowItem("notification-threshold") { modifier ->
                    SettingsSliderRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_notification_threshold),
                        summary = thresholdSummary,
                        icon = Icons.Rounded.Speed,
                        value = threshold.toFloat() / 1024f,
                        onValueChange = {
                            viewModel.setNotificationThreshold((it * 1024f).roundToLong())
                        },
                        valueRange = 0f..1024f,
                        steps = 20,
                        valueText = thresholdText,
                    )
                },
                SettingsRowItem("notification-threshold-input") { modifier ->
                    SettingsTextFieldRow(
                        modifier = modifier,
                        title = stringResource(
                            R.string.settings_notification_threshold_input_title,
                            thresholdUnit,
                        ),
                        summary = stringResource(
                            R.string.settings_notification_threshold_input_summary,
                            thresholdUnit,
                        ),
                        value = SpeedFormatter.formatThresholdInputValue(threshold, speedRateUnit),
                        onValueChange = {
                            SpeedFormatter.parseThresholdInputValue(it, speedRateUnit)?.let(
                                viewModel::setNotificationThreshold,
                            )
                        },
                        icon = Icons.Rounded.Edit,
                        keyboardType = KeyboardType.Decimal,
                    )
                },
                SettingsRowItem("notification-low-traffic-mode") { modifier ->
                    SettingsChoiceRow(
                        modifier = modifier,
                        value = lowTrafficModeLabel,
                        options = listOf(labelStatic, labelDynamic),
                        onValueChange = {
                            val mode = when (it) {
                                labelDynamic -> 1
                                else -> 0
                            }
                            viewModel.setNotificationLowTrafficMode(mode)
                        },
                        title = stringResource(R.string.settings_notification_low_traffic_mode),
                        summaryContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = lowTrafficModeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(R.string.settings_notification_low_traffic_mode_explanation),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        icon = Icons.Rounded.LowPriority,
                        optionLabel = { it },
                    )
                },
            ),
        )
    }
}
