package vip.mystery0.pixel.meter.ui.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.TextFieldPreference
import vip.mystery0.pixel.meter.R
import vip.mystery0.pixel.meter.format.SpeedFormatter

@Composable
fun OverlaySettingsSection(viewModel: SettingsViewModel) {
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val canOverlay by viewModel.canOverlay.collectAsState()

    val isEnabled by viewModel.isOverlayEnabled.collectAsState(initial = false)
    val isLocked by viewModel.isOverlayLocked.collectAsState(initial = false)
    val isOverlayShowOnStatusBar by viewModel.isOverlayShowOnStatusBar.collectAsState(initial = false)
    val isOverlayUseDefaultColors by viewModel.isOverlayUseDefaultColors.collectAsState(initial = false)
    val bgColor by viewModel.overlayBgColor.collectAsState(initial = 0)
    val textColor by viewModel.overlayTextColor.collectAsState(initial = 0)
    val cornerRadius by viewModel.overlayCornerRadius.collectAsState(initial = 8)
    val padding by viewModel.overlayPadding.collectAsState(initial = 8)
    val textSize by viewModel.overlayTextSize.collectAsState(initial = 10f)
    val textUp by viewModel.overlayTextUp.collectAsState(initial = "\u25B2 ")
    val textDown by viewModel.overlayTextDown.collectAsState(initial = "\u25BC ")
    val upFirst by viewModel.overlayOrderUpFirst.collectAsState(initial = true)
    val isOverlayHideBackground by viewModel.isOverlayHideBackground.collectAsState(initial = false)
    val overlayX by viewModel.overlayX.collectAsState(initial = 100)
    val overlayY by viewModel.overlayY.collectAsState(initial = 200)
    val isOverlayPortraitOnly by viewModel.isOverlayPortraitOnly.collectAsState(initial = false)
    val isOverlayHideInImmersiveMode by viewModel.isOverlayHideInImmersiveMode.collectAsState(
        initial = false
    )
    val overlayAutoHideThreshold by viewModel.overlayAutoHideThreshold.collectAsState(initial = 0L)
    val direction by viewModel.overlayDirection.collectAsState(initial = 0)
    val displayMode by viewModel.overlayDisplayMode.collectAsState(initial = 0)
    val alignment by viewModel.overlayAlignment.collectAsState(initial = 0)
    val meterSpacing by viewModel.overlayMeterSpacing.collectAsState(initial = 8)
    val speedRateUnit by viewModel.speedRateUnit.collectAsState(initial = 0)
    val platformLocale = LocalLocale.current.platformLocale

    PreferenceCategory(title = { Text(stringResource(R.string.settings_category_overlay)) })
    val isSwitchEnabled = !isServiceRunning || canOverlay
    val summaryText = if (isSwitchEnabled) {
        stringResource(R.string.config_enable_overlay_desc)
    } else {
        stringResource(R.string.config_overlay_disabled_reason)
    }
    SwitchPreference(
        value = isEnabled,
        onValueChange = { viewModel.setOverlayEnabled(it) },
        enabled = isSwitchEnabled,
        title = { Text(stringResource(R.string.config_enable_overlay)) },
        summary = { Text(summaryText) }
    )

    if (isEnabled) {
        SwitchPreference(
            value = isLocked,
            onValueChange = { viewModel.setOverlayLocked(it) },
            title = { Text(stringResource(R.string.settings_lock_overlay)) },
            summary = { Text(stringResource(R.string.config_lock_overlay_desc)) }
        )
        val labelBoth = stringResource(R.string.settings_display_mode_both)
        val labelUpload = stringResource(R.string.settings_display_mode_upload)
        val labelDownload = stringResource(R.string.settings_display_mode_download)
        val labelTotal = stringResource(R.string.settings_display_mode_total)
        val displayModeValues = listOf(labelBoth, labelUpload, labelDownload, labelTotal)
        val displayModeLabel = displayModeValues.getOrElse(displayMode) { labelBoth }
        ListPreference(
            value = displayModeLabel,
            onValueChange = {
                viewModel.setOverlayDisplayMode(displayModeValues.indexOf(it).coerceAtLeast(0))
            },
            title = { Text(stringResource(R.string.settings_overlay_display_mode)) },
            values = displayModeValues,
            summary = { Text(displayModeLabel) }
        )
        SwitchPreference(
            value = isOverlayShowOnStatusBar,
            onValueChange = { viewModel.setOverlayShowOnStatusBar(it) },
            title = { Text(stringResource(R.string.settings_overlay_show_on_status_bar)) },
            summary = { Text(stringResource(R.string.settings_overlay_show_on_status_bar_desc)) }
        )
        TextFieldPreference(
            value = overlayX.toString(),
            onValueChange = {
                val x = it.toIntOrNull()
                if (x != null) {
                    viewModel.setOverlayPosition(x, overlayY)
                }
            },
            title = { Text(stringResource(R.string.settings_overlay_position_x)) },
            summary = { Text(stringResource(R.string.settings_overlay_position_x_desc)) },
            textToValue = { it },
        )
        TextFieldPreference(
            value = overlayY.toString(),
            onValueChange = {
                val y = it.toIntOrNull()
                if (y != null) {
                    viewModel.setOverlayPosition(overlayX, y)
                }
            },
            title = { Text(stringResource(R.string.settings_overlay_position_y)) },
            summary = { Text(stringResource(R.string.settings_overlay_position_y_desc)) },
            textToValue = { it },
        )
        SwitchPreference(
            value = isOverlayPortraitOnly,
            onValueChange = { viewModel.setOverlayPortraitOnly(it) },
            title = { Text(stringResource(R.string.settings_overlay_portrait_only)) },
            summary = { Text(stringResource(R.string.settings_overlay_portrait_only_desc)) }
        )
        SwitchPreference(
            value = isOverlayHideInImmersiveMode,
            onValueChange = { viewModel.setOverlayHideInImmersiveMode(it) },
            title = { Text(stringResource(R.string.settings_overlay_hide_in_immersive_mode)) },
            summary = {
                Text(stringResource(R.string.settings_overlay_hide_in_immersive_mode_desc))
            }
        )
        SliderPreference(
            value = 0F,
            onValueChange = { },
            sliderValue = overlayAutoHideThreshold.toFloat() / 1024,
            onSliderValueChange = {
                viewModel.setOverlayAutoHideThreshold((it * 1024).toLong())
            },
            valueRange = 0f..1024f,
            valueSteps = 20,
            title = { Text(stringResource(R.string.settings_overlay_auto_hide_threshold)) },
            summary = {
                if (overlayAutoHideThreshold == 0L) {
                    Text(stringResource(R.string.settings_overlay_auto_hide_threshold_disabled))
                } else {
                    val thresholdText = SpeedFormatter.formatSpeedLine(overlayAutoHideThreshold, rateUnit = speedRateUnit)
                    Text(
                        stringResource(
                            R.string.settings_overlay_auto_hide_threshold_desc,
                            thresholdText
                        )
                    )
                }
            },
            valueText = {
                Text(SpeedFormatter.formatSpeedLine(overlayAutoHideThreshold, rateUnit = speedRateUnit))
            }
        )
        TextFieldPreference(
            value = SpeedFormatter.formatThresholdInputValue(
                overlayAutoHideThreshold,
                speedRateUnit
            ),
            onValueChange = {
                SpeedFormatter.parseThresholdInputValue(it, speedRateUnit)?.let(
                    viewModel::setOverlayAutoHideThreshold
                )
            },
            title = {
                Text(
                    stringResource(
                        R.string.settings_overlay_auto_hide_threshold_input_title,
                        if (speedRateUnit == 1) "kb/s" else "KB/s"
                    )
                )
            },
            summary = {
                Text(
                    stringResource(
                        R.string.settings_overlay_auto_hide_threshold_input_summary,
                        if (speedRateUnit == 1) "kb/s" else "KB/s"
                    )
                )
            },
            textToValue = { it },
        )
        SwitchPreference(
            value = isOverlayUseDefaultColors,
            onValueChange = { viewModel.setOverlayUseDefaultColors(it) },
            title = { Text(stringResource(R.string.settings_overlay_use_default_colors)) },
            summary = { Text(stringResource(R.string.settings_overlay_use_default_colors_desc)) }
        )
        SwitchPreference(
            value = isOverlayHideBackground,
            onValueChange = { viewModel.setOverlayHideBackground(it) },
            title = { Text(stringResource(R.string.settings_overlay_hide_background)) },
            summary = { Text(stringResource(R.string.settings_overlay_hide_background_desc)) }
        )
        ColorPreference(
            title = stringResource(R.string.settings_overlay_bg_color),
            color = Color(bgColor),
            enabled = !isOverlayUseDefaultColors && !isOverlayHideBackground,
            onColorSelected = { viewModel.setOverlayBgColor(it.toArgb()) }
        )
        ColorPreference(
            title = stringResource(R.string.settings_overlay_text_color),
            color = Color(textColor),
            enabled = !isOverlayUseDefaultColors,
            onColorSelected = { viewModel.setOverlayTextColor(it.toArgb()) }
        )
        SliderPreference(
            value = 0F,
            onValueChange = { },
            sliderValue = cornerRadius.toFloat(),
            onSliderValueChange = { viewModel.setOverlayCornerRadius(it.toInt()) },
            valueRange = 0f..32f,
            valueSteps = 32,
            title = { Text(stringResource(R.string.settings_overlay_corner_radius)) },
            valueText = { Text("${cornerRadius}dp") }
        )
        SliderPreference(
            value = 0F,
            onValueChange = { },
            sliderValue = padding.toFloat(),
            onSliderValueChange = { viewModel.setOverlayPadding(it.toInt()) },
            valueRange = 0f..24f,
            valueSteps = 24,
            title = { Text(stringResource(R.string.settings_overlay_padding)) },
            valueText = { Text("${padding}dp") }
        )
        SliderPreference(
            value = 0F,
            onValueChange = { },
            sliderValue = textSize,
            onSliderValueChange = { viewModel.setOverlayTextSize(it) },
            valueRange = 7f..24f,
            title = { Text(stringResource(R.string.settings_overlay_text_size)) },
            valueText = { Text("${"%.1f".format(platformLocale, textSize)}sp") }
        )
        TextFieldPreference(
            value = textUp,
            onValueChange = { viewModel.setOverlayTextUp(it) },
            textToValue = { it },
            enabled = displayMode == 0 || displayMode == 1,
            title = { Text(stringResource(R.string.settings_text_prefix_up)) },
            summary = { Text(stringResource(R.string.settings_text_prefix_up_desc, textUp)) },
        )
        TextFieldPreference(
            value = textDown,
            onValueChange = { viewModel.setOverlayTextDown(it) },
            textToValue = { it },
            enabled = displayMode == 0 || displayMode == 2,
            title = { Text(stringResource(R.string.settings_text_prefix_down)) },
            summary = { Text(stringResource(R.string.settings_text_prefix_down_desc, textDown)) },
        )
        SwitchPreference(
            value = upFirst,
            onValueChange = { viewModel.setOverlayOrderUpFirst(it) },
            enabled = displayMode == 0,
            title = { Text(stringResource(R.string.settings_show_up_first)) },
            summary = { Text(stringResource(R.string.settings_show_up_first_desc)) }
        )

        val labelHorizontal = stringResource(R.string.settings_overlay_direction_horizontal)
        val labelVertical = stringResource(R.string.settings_overlay_direction_vertical)
        val directionLabel = when (direction) {
            1 -> labelVertical
            else -> labelHorizontal
        }

        ListPreference(
            value = directionLabel,
            onValueChange = {
                val dir = when (it) {
                    labelVertical -> 1
                    else -> 0
                }
                viewModel.setOverlayDirection(dir)
            },
            title = { Text(stringResource(R.string.settings_overlay_direction)) },
            values = listOf(labelHorizontal, labelVertical),
            summary = { Text(directionLabel) },
            enabled = displayMode == 0
        )

        SliderPreference(
            value = 0F,
            onValueChange = { },
            sliderValue = meterSpacing.toFloat(),
            onSliderValueChange = { viewModel.setOverlayMeterSpacing(it.toInt()) },
            valueRange = 0f..120f,
            valueSteps = 24,
            title = { Text(stringResource(R.string.settings_overlay_meter_spacing)) },
            summary = { Text(stringResource(R.string.settings_overlay_meter_spacing_desc)) },
            valueText = { Text("${meterSpacing}dp") },
            enabled = displayMode == 0 && direction == 0
        )

        val labelAlignStart = stringResource(R.string.settings_overlay_alignment_start)
        val labelAlignCenter = stringResource(R.string.settings_overlay_alignment_center)
        val labelAlignEnd = stringResource(R.string.settings_overlay_alignment_end)
        val alignmentLabel = when (alignment) {
            1 -> labelAlignCenter
            2 -> labelAlignEnd
            else -> labelAlignStart
        }

        ListPreference(
            value = alignmentLabel,
            onValueChange = {
                val align = when (it) {
                    labelAlignCenter -> 1
                    labelAlignEnd -> 2
                    else -> 0
                }
                viewModel.setOverlayAlignment(align)
            },
            title = { Text(stringResource(R.string.settings_overlay_alignment)) },
            values = listOf(labelAlignStart, labelAlignCenter, labelAlignEnd),
            summary = { Text(alignmentLabel) },
            enabled = displayMode == 0 && direction == 1
        )
    }
}
