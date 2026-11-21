package vip.mystery0.pixel.meter.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Padding
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import vip.mystery0.pixel.meter.R
import vip.mystery0.pixel.meter.format.SpeedFormatter
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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
        initial = false,
    )
    val overlayAutoHideThreshold by viewModel.overlayAutoHideThreshold.collectAsState(initial = 0L)
    val direction by viewModel.overlayDirection.collectAsState(initial = 0)
    val displayMode by viewModel.overlayDisplayMode.collectAsState(initial = 0)
    val alignment by viewModel.overlayAlignment.collectAsState(initial = 0)
    val meterSpacing by viewModel.overlayMeterSpacing.collectAsState(initial = 8)
    val speedRateUnit by viewModel.speedRateUnit.collectAsState(initial = 0)
    val platformLocale = LocalLocale.current.platformLocale

    val isSwitchEnabled = !isServiceRunning || canOverlay
    val enableSummary = if (isSwitchEnabled) {
        stringResource(R.string.config_enable_overlay_desc)
    } else {
        stringResource(R.string.config_overlay_disabled_reason)
    }

    SettingsSection(title = stringResource(R.string.settings_section_basic)) {
        SettingsGroup(
            rows = listOf(
                SettingsRowItem("overlay-enabled") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = isEnabled,
                        onCheckedChange = { viewModel.setOverlayEnabled(it) },
                        enabled = isSwitchEnabled,
                        title = stringResource(R.string.config_enable_overlay),
                        summary = enableSummary,
                        icon = Icons.Rounded.PictureInPictureAlt,
                    )
                },
            ),
        )
    }

    if (!isEnabled) return

    SettingsSection(title = stringResource(R.string.settings_section_behavior_and_position)) {
        SettingsGroup(
            rows = listOf(
                SettingsRowItem("overlay-locked") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = isLocked,
                        onCheckedChange = { viewModel.setOverlayLocked(it) },
                        title = stringResource(R.string.settings_lock_overlay),
                        summary = stringResource(R.string.config_lock_overlay_desc),
                        icon = Icons.Rounded.Lock,
                    )
                },
                SettingsRowItem("overlay-status-bar") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = isOverlayShowOnStatusBar,
                        onCheckedChange = { viewModel.setOverlayShowOnStatusBar(it) },
                        title = stringResource(R.string.settings_overlay_show_on_status_bar),
                        summary = stringResource(R.string.settings_overlay_show_on_status_bar_desc),
                        icon = Icons.Rounded.VerticalAlignTop,
                    )
                },
                SettingsRowItem("overlay-position-x") { modifier ->
                    SettingsTextFieldRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_overlay_position_x),
                        summary = "${overlayX}px · ${stringResource(R.string.settings_overlay_position_x_desc)}",
                        value = overlayX.toString(),
                        onValueChange = {
                            val x = it.trim().toIntOrNull()
                            if (x != null) {
                                viewModel.setOverlayPosition(x, overlayY)
                            }
                        },
                        icon = Icons.Rounded.OpenWith,
                    )
                },
                SettingsRowItem("overlay-position-y") { modifier ->
                    SettingsTextFieldRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_overlay_position_y),
                        summary = "${overlayY}px · ${stringResource(R.string.settings_overlay_position_y_desc)}",
                        value = overlayY.toString(),
                        onValueChange = {
                            val y = it.trim().toIntOrNull()
                            if (y != null) {
                                viewModel.setOverlayPosition(overlayX, y)
                            }
                        },
                        icon = Icons.Rounded.OpenWith,
                    )
                },
                SettingsRowItem("overlay-portrait-only") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = isOverlayPortraitOnly,
                        onCheckedChange = { viewModel.setOverlayPortraitOnly(it) },
                        title = stringResource(R.string.settings_overlay_portrait_only),
                        summary = stringResource(R.string.settings_overlay_portrait_only_desc),
                        icon = Icons.Rounded.ScreenLockPortrait,
                    )
                },
                SettingsRowItem("overlay-immersive-hide") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = isOverlayHideInImmersiveMode,
                        onCheckedChange = { viewModel.setOverlayHideInImmersiveMode(it) },
                        title = stringResource(R.string.settings_overlay_hide_in_immersive_mode),
                        summary = stringResource(R.string.settings_overlay_hide_in_immersive_mode_desc),
                        icon = Icons.Rounded.Fullscreen,
                    )
                },
            ),
        )
    }

    val thresholdUnit = if (speedRateUnit == 1) "kb/s" else "KB/s"
    val thresholdText = SpeedFormatter.formatSpeedLine(
        overlayAutoHideThreshold,
        rateUnit = speedRateUnit,
    )
    val autoHideSummary = if (overlayAutoHideThreshold == 0L) {
        stringResource(R.string.settings_overlay_auto_hide_threshold_disabled)
    } else {
        stringResource(R.string.settings_overlay_auto_hide_threshold_desc, thresholdText)
    }

    SettingsSection(title = stringResource(R.string.settings_section_auto_hide)) {
        SettingsGroup(
            rows = listOf(
                SettingsRowItem("overlay-auto-hide-threshold") { modifier ->
                    SettingsSliderRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_overlay_auto_hide_threshold),
                        summary = autoHideSummary,
                        icon = Icons.Rounded.VisibilityOff,
                        value = overlayAutoHideThreshold.toFloat() / 1024f,
                        onValueChange = {
                            viewModel.setOverlayAutoHideThreshold((it * 1024f).roundToLong())
                        },
                        valueRange = 0f..1024f,
                        steps = 20,
                        valueText = thresholdText,
                    )
                },
                SettingsRowItem("overlay-auto-hide-threshold-input") { modifier ->
                    SettingsTextFieldRow(
                        modifier = modifier,
                        title = stringResource(
                            R.string.settings_overlay_auto_hide_threshold_input_title,
                            thresholdUnit,
                        ),
                        summary = stringResource(
                            R.string.settings_overlay_auto_hide_threshold_input_summary,
                            thresholdUnit,
                        ),
                        value = SpeedFormatter.formatThresholdInputValue(
                            overlayAutoHideThreshold,
                            speedRateUnit,
                        ),
                        onValueChange = {
                            SpeedFormatter.parseThresholdInputValue(it, speedRateUnit)?.let(
                                viewModel::setOverlayAutoHideThreshold,
                            )
                        },
                        icon = Icons.Rounded.Edit,
                        keyboardType = KeyboardType.Decimal,
                    )
                },
            ),
        )
    }

    SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
        SettingsGroup(
            rows = listOf(
                SettingsRowItem("overlay-default-colors") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = isOverlayUseDefaultColors,
                        onCheckedChange = { viewModel.setOverlayUseDefaultColors(it) },
                        title = stringResource(R.string.settings_overlay_use_default_colors),
                        summary = stringResource(R.string.settings_overlay_use_default_colors_desc),
                        icon = Icons.Rounded.AutoAwesome,
                    )
                },
                SettingsRowItem("overlay-hide-background") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = isOverlayHideBackground,
                        onCheckedChange = { viewModel.setOverlayHideBackground(it) },
                        title = stringResource(R.string.settings_overlay_hide_background),
                        summary = stringResource(R.string.settings_overlay_hide_background_desc),
                        icon = Icons.Rounded.Opacity,
                    )
                },
                SettingsRowItem("overlay-bg-color") { modifier ->
                    ColorPreference(
                        modifier = modifier,
                        title = stringResource(R.string.settings_overlay_bg_color),
                        color = Color(bgColor),
                        icon = Icons.Rounded.FormatColorFill,
                        enabled = !isOverlayUseDefaultColors && !isOverlayHideBackground,
                        onColorSelected = { viewModel.setOverlayBgColor(it.toArgb()) },
                    )
                },
                SettingsRowItem("overlay-text-color") { modifier ->
                    ColorPreference(
                        modifier = modifier,
                        title = stringResource(R.string.settings_overlay_text_color),
                        color = Color(textColor),
                        icon = Icons.Rounded.FormatColorText,
                        enabled = !isOverlayUseDefaultColors,
                        onColorSelected = { viewModel.setOverlayTextColor(it.toArgb()) },
                    )
                },
                SettingsRowItem("overlay-corner-radius") { modifier ->
                    SettingsSliderRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_overlay_corner_radius),
                        icon = Icons.Rounded.RoundedCorner,
                        value = cornerRadius.toFloat(),
                        onValueChange = { viewModel.setOverlayCornerRadius(it.roundToInt()) },
                        valueRange = 0f..32f,
                        steps = 31,
                        valueText = "${cornerRadius}dp",
                    )
                },
                SettingsRowItem("overlay-padding") { modifier ->
                    SettingsSliderRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_overlay_padding),
                        icon = Icons.Rounded.Padding,
                        value = padding.toFloat(),
                        onValueChange = { viewModel.setOverlayPadding(it.roundToInt()) },
                        valueRange = 0f..24f,
                        steps = 23,
                        valueText = "${padding}dp",
                    )
                },
                SettingsRowItem("overlay-text-size") { modifier ->
                    SettingsSliderRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_overlay_text_size),
                        icon = Icons.Rounded.FormatSize,
                        value = textSize,
                        onValueChange = { viewModel.setOverlayTextSize(it) },
                        valueRange = 7f..24f,
                        valueText = "${"%.1f".format(platformLocale, textSize)}sp",
                    )
                },
            ),
        )
    }

    val labelBoth = stringResource(R.string.settings_display_mode_both)
    val labelUpload = stringResource(R.string.settings_display_mode_upload)
    val labelDownload = stringResource(R.string.settings_display_mode_download)
    val labelTotal = stringResource(R.string.settings_display_mode_total)
    val displayModeValues = listOf(labelBoth, labelUpload, labelDownload, labelTotal)
    val displayModeLabel = displayModeValues.getOrElse(displayMode) { labelBoth }

    val labelHorizontal = stringResource(R.string.settings_overlay_direction_horizontal)
    val labelVertical = stringResource(R.string.settings_overlay_direction_vertical)
    val directionLabel = when (direction) {
        1 -> labelVertical
        else -> labelHorizontal
    }

    val labelAlignStart = stringResource(R.string.settings_overlay_alignment_start)
    val labelAlignCenter = stringResource(R.string.settings_overlay_alignment_center)
    val labelAlignEnd = stringResource(R.string.settings_overlay_alignment_end)
    val alignmentLabel = when (alignment) {
        1 -> labelAlignCenter
        2 -> labelAlignEnd
        else -> labelAlignStart
    }

    SettingsSection(title = stringResource(R.string.settings_section_content_and_layout)) {
        SettingsGroup(
            rows = listOf(
                SettingsRowItem("overlay-display-mode") { modifier ->
                    SettingsChoiceRow(
                        modifier = modifier,
                        value = displayModeLabel,
                        options = displayModeValues,
                        onValueChange = {
                            viewModel.setOverlayDisplayMode(
                                displayModeValues.indexOf(it).coerceAtLeast(0),
                            )
                        },
                        title = stringResource(R.string.settings_overlay_display_mode),
                        summary = displayModeLabel,
                        icon = Icons.Rounded.Tune,
                        optionLabel = { it },
                    )
                },
                SettingsRowItem("overlay-text-up") { modifier ->
                    SettingsTextFieldRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_text_prefix_up),
                        summary = stringResource(R.string.settings_text_prefix_up_desc, textUp),
                        value = textUp,
                        onValueChange = { viewModel.setOverlayTextUp(it) },
                        enabled = displayMode == 0 || displayMode == 1,
                        icon = Icons.Rounded.ArrowUpward,
                    )
                },
                SettingsRowItem("overlay-text-down") { modifier ->
                    SettingsTextFieldRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_text_prefix_down),
                        summary = stringResource(R.string.settings_text_prefix_down_desc, textDown),
                        value = textDown,
                        onValueChange = { viewModel.setOverlayTextDown(it) },
                        enabled = displayMode == 0 || displayMode == 2,
                        icon = Icons.Rounded.ArrowDownward,
                    )
                },
                SettingsRowItem("overlay-up-first") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = upFirst,
                        onCheckedChange = { viewModel.setOverlayOrderUpFirst(it) },
                        enabled = displayMode == 0,
                        title = stringResource(R.string.settings_show_up_first),
                        summary = stringResource(R.string.settings_show_up_first_desc),
                        icon = Icons.Rounded.SwapVert,
                    )
                },
                SettingsRowItem("overlay-direction") { modifier ->
                    SettingsChoiceRow(
                        modifier = modifier,
                        value = directionLabel,
                        options = listOf(labelHorizontal, labelVertical),
                        onValueChange = {
                            val dir = when (it) {
                                labelVertical -> 1
                                else -> 0
                            }
                            viewModel.setOverlayDirection(dir)
                        },
                        title = stringResource(R.string.settings_overlay_direction),
                        summary = directionLabel,
                        icon = Icons.Rounded.ViewAgenda,
                        enabled = displayMode == 0,
                        optionLabel = { it },
                    )
                },
                SettingsRowItem("overlay-meter-spacing") { modifier ->
                    SettingsSliderRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_overlay_meter_spacing),
                        summary = stringResource(R.string.settings_overlay_meter_spacing_desc),
                        icon = Icons.Rounded.SpaceBar,
                        value = meterSpacing.toFloat(),
                        onValueChange = { viewModel.setOverlayMeterSpacing(it.roundToInt()) },
                        valueRange = 0f..120f,
                        steps = 23,
                        valueText = "${meterSpacing}dp",
                        enabled = displayMode == 0 && direction == 0,
                    )
                },
                SettingsRowItem("overlay-alignment") { modifier ->
                    SettingsChoiceRow(
                        modifier = modifier,
                        value = alignmentLabel,
                        options = listOf(labelAlignStart, labelAlignCenter, labelAlignEnd),
                        onValueChange = {
                            val align = when (it) {
                                labelAlignCenter -> 1
                                labelAlignEnd -> 2
                                else -> 0
                            }
                            viewModel.setOverlayAlignment(align)
                        },
                        title = stringResource(R.string.settings_overlay_alignment),
                        summary = alignmentLabel,
                        icon = Icons.AutoMirrored.Rounded.FormatAlignLeft,
                        enabled = displayMode == 0 && direction == 1,
                        optionLabel = { it },
                    )
                },
            ),
        )
    }
}
