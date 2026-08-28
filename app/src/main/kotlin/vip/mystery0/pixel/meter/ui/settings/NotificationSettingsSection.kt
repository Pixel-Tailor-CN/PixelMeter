package vip.mystery0.pixel.meter.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.TextFieldPreference
import vip.mystery0.pixel.meter.R
import vip.mystery0.pixel.meter.format.SpeedFormatter

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
    val textSize by viewModel.notificationTextSize.collectAsState(initial = 0.65f)
    val unitSize by viewModel.notificationUnitSize.collectAsState(initial = 0.35f)

    PreferenceCategory(title = { Text(stringResource(R.string.settings_category_notification)) })
    SwitchPreference(
        value = isEnabled,
        onValueChange = { viewModel.setNotificationEnabled(it) },
        title = { Text(stringResource(R.string.config_enable_notification)) },
        summary = { Text(stringResource(R.string.config_enable_notification_desc)) }
    )

    if (isEnabled) {
        SwitchPreference(
            value = isLiveUpdateEnabled,
            onValueChange = { viewModel.setLiveUpdateEnabled(it) },
            title = { Text(stringResource(R.string.config_enable_live_update)) },
            summary = { Text(stringResource(R.string.config_enable_live_update_desc)) }
        )
        TextFieldPreference(
            value = textUp,
            onValueChange = { viewModel.setNotificationTextUp(it) },
            textToValue = { it },
            title = { Text(stringResource(R.string.settings_text_prefix_up)) },
            summary = { Text(stringResource(R.string.settings_text_prefix_up_desc, textUp)) },
        )
        TextFieldPreference(
            value = textDown,
            onValueChange = { viewModel.setNotificationTextDown(it) },
            textToValue = { it },
            title = { Text(stringResource(R.string.settings_text_prefix_down)) },
            summary = { Text(stringResource(R.string.settings_text_prefix_down_desc, textDown)) },
        )
        SwitchPreference(
            value = upFirst,
            onValueChange = { viewModel.setNotificationOrderUpFirst(it) },
            title = { Text(stringResource(R.string.settings_show_up_first)) },
            summary = { Text(stringResource(R.string.settings_show_up_first_desc)) }
        )

        val labelBoth = stringResource(R.string.settings_display_mode_both)
        val labelTotal = stringResource(R.string.settings_display_mode_total)
        val labelUpload = stringResource(R.string.settings_display_mode_upload)
        val labelDownload = stringResource(R.string.settings_display_mode_download)
        val displayModeLabel = when (displayMode) {
            1 -> labelUpload
            2 -> labelDownload
            else -> labelBoth
        }

        ListPreference(
            value = displayModeLabel,
            onValueChange = {
                val mode = when (it) {
                    labelUpload -> 1
                    labelDownload -> 2
                    else -> 0
                }
                viewModel.setNotificationDisplayMode(mode)
            },
            title = { Text(stringResource(R.string.settings_notification_display_mode)) },
            values = listOf(
                labelBoth,
                labelUpload,
                labelDownload
            ),
            summary = { Text(displayModeLabel) }
        )

        val iconModeValues = listOf(labelTotal, labelUpload, labelDownload)
        val iconModeLabel = iconModeValues.getOrElse(iconMode) { labelTotal }
        ListPreference(
            value = iconModeLabel,
            onValueChange = {
                viewModel.setNotificationIconMode(iconModeValues.indexOf(it).coerceAtLeast(0))
            },
            title = { Text(stringResource(R.string.settings_notification_icon_mode)) },
            values = iconModeValues,
            summary = { Text(iconModeLabel) }
        )

        SliderPreference(
            enabled = !isLiveUpdateEnabled,
            value = 0F,
            onValueChange = { },
            sliderValue = textSize,
            onSliderValueChange = { viewModel.setNotificationTextSize(it) },
            valueRange = 0.1f..1.0f,
            title = { Text(stringResource(R.string.settings_notification_text_size)) },
            valueText = { Text("%.2f".format(textSize)) }
        )

        SliderPreference(
            enabled = !isLiveUpdateEnabled,
            value = 0F,
            onValueChange = { },
            sliderValue = unitSize,
            onSliderValueChange = { viewModel.setNotificationUnitSize(it) },
            valueRange = 0.1f..1.0f,
            title = { Text(stringResource(R.string.settings_notification_unit_size)) },
            valueText = { Text("%.2f".format(unitSize)) }
        )

        val threshold by viewModel.notificationThreshold.collectAsState(initial = 0L)
        val lowTrafficMode by viewModel.notificationLowTrafficMode.collectAsState(initial = 0)

        SliderPreference(
            value = 0F,
            onValueChange = { },
            sliderValue = threshold.toFloat() / 1024,
            onSliderValueChange = { viewModel.setNotificationThreshold((it * 1024).toLong()) },
            valueRange = 0f..1024f,
            valueSteps = 20,
            title = { Text(stringResource(R.string.settings_notification_threshold)) },
            summary = {
                if (threshold == 0L) {
                    Text(stringResource(R.string.settings_notification_threshold_disabled))
                } else {
                    val thresholdText = SpeedFormatter.formatSpeedLine(threshold, rateUnit = speedRateUnit)
                    Text(
                        stringResource(
                            R.string.settings_notification_threshold_desc,
                            thresholdText
                        )
                    )
                }
            },
            valueText = {
                Text(SpeedFormatter.formatSpeedLine(threshold, rateUnit = speedRateUnit))
            }
        )

        TextFieldPreference(
            value = SpeedFormatter.formatThresholdInputValue(threshold, speedRateUnit),
            onValueChange = {
                SpeedFormatter.parseThresholdInputValue(it, speedRateUnit)?.let(
                    viewModel::setNotificationThreshold
                )
            },
            title = {
                Text(
                    stringResource(
                        R.string.settings_notification_threshold_input_title,
                        if (speedRateUnit == 1) "kb/s" else "KB/s"
                    )
                )
            },
            summary = {
                Text(
                    stringResource(
                        R.string.settings_notification_threshold_input_summary,
                        if (speedRateUnit == 1) "kb/s" else "KB/s"
                    )
                )
            },
            textToValue = { it },
        )

        val labelStatic = stringResource(R.string.settings_notification_low_traffic_mode_static)
        val labelDynamic = stringResource(R.string.settings_notification_low_traffic_mode_dynamic)
        val lowTrafficModeLabel = when (lowTrafficMode) {
            1 -> labelDynamic
            else -> labelStatic
        }

        ListPreference(
            value = lowTrafficModeLabel,
            onValueChange = {
                val mode = when (it) {
                    labelDynamic -> 1
                    else -> 0
                }
                viewModel.setNotificationLowTrafficMode(mode)
            },
            title = { Text(stringResource(R.string.settings_notification_low_traffic_mode)) },
            values = listOf(
                labelStatic,
                labelDynamic
            ),
            summary = {
                Column {
                    Text(lowTrafficModeLabel)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_notification_low_traffic_mode_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        val useCustomColor by viewModel.notificationUseCustomColor.collectAsState(initial = false)
        val notificationColor by viewModel.notificationColor.collectAsState(initial = 0)

        SwitchPreference(
            value = useCustomColor,
            onValueChange = { viewModel.setNotificationUseCustomColor(it) },
            title = { Text(stringResource(R.string.settings_notification_use_custom_color_title)) },
            summary = { Text(stringResource(R.string.settings_notification_use_custom_color_desc)) }
        )

        ColorPreference(
            title = stringResource(R.string.settings_notification_color_title),
            color = Color(notificationColor),
            enabled = useCustomColor,
            onColorSelected = { viewModel.setNotificationColor(it.toArgb()) }
        )
    }
}
