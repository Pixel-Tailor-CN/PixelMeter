package vip.mystery0.pixel.meter.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.meter.BuildConfig
import vip.mystery0.pixel.meter.R

@Composable
fun MainSettingsPage(
    viewModel: SettingsViewModel,
    selectedPage: SettingsPage?,
    twoPane: Boolean,
    onPageSelected: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interval by viewModel.samplingInterval.collectAsState(initial = 1500L)
    val speedRateUnit by viewModel.speedRateUnit.collectAsState(initial = 0)
    val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsState(initial = false)
    val isLiveUpdateEnabled by viewModel.isLiveUpdateEnabled.collectAsState(initial = false)
    val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsState(initial = false)
    val isOverlayLocked by viewModel.isOverlayLocked.collectAsState(initial = false)
    val isIgnoringBatteryOptimizations by viewModel.isIgnoringBatteryOptimizations.collectAsState()

    val rateUnitLabel = stringResource(
        if (speedRateUnit == 1) {
            R.string.settings_speed_rate_unit_bits
        } else {
            R.string.settings_speed_rate_unit_bytes
        },
    )
    val generalSummary = stringResource(R.string.settings_summary_general, interval, rateUnitLabel)
    val notificationSummary = when {
        !isNotificationEnabled -> stringResource(R.string.settings_state_disabled)
        isLiveUpdateEnabled -> stringResource(R.string.settings_summary_notification_live_update)
        else -> stringResource(R.string.settings_state_enabled)
    }
    val overlaySummary = when {
        !isOverlayEnabled -> stringResource(R.string.settings_state_disabled)
        isOverlayLocked -> stringResource(R.string.settings_summary_overlay_locked)
        else -> stringResource(R.string.settings_state_enabled)
    }
    val backgroundSummary = stringResource(
        if (isIgnoringBatteryOptimizations) {
            R.string.settings_ignore_battery_optimizations_on
        } else {
            R.string.settings_ignore_battery_optimizations_off
        },
    )

    val summaryFor: (SettingsPage) -> String = { page ->
        when (page) {
            SettingsPage.General -> generalSummary
            SettingsPage.Notification -> notificationSummary
            SettingsPage.Overlay -> overlaySummary
            SettingsPage.Background -> backgroundSummary
            SettingsPage.About -> BuildConfig.VERSION_NAME
        }
    }

    val groupedPages = SettingsPage.entries.groupBy { it.group }

    LazyColumn(
        modifier = modifier,
        state = rememberLazyListState(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        groupedPages.forEach { (group, pages) ->
            item(key = "category_group_$group") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    pages.forEachIndexed { index, page ->
                        val isSelected = twoPane && page == selectedPage
                        val colors = MaterialTheme.colorScheme
                        val containerColor = if (group == 0) {
                            colors.primaryContainer
                        } else {
                            colors.secondaryContainer
                        }
                        val iconTint = if (group == 0) {
                            colors.onPrimaryContainer
                        } else {
                            colors.onSecondaryContainer
                        }

                        Box(
                            modifier = Modifier.semantics { selected = isSelected },
                        ) {
                            SettingsRowContainer(
                                first = index == 0,
                                last = index == pages.lastIndex,
                                active = isSelected,
                            ) {
                                SettingsItemRow(
                                    title = stringResource(page.titleRes),
                                    summary = summaryFor(page),
                                    titleColor = if (isSelected) {
                                        colors.onSecondaryContainer
                                    } else {
                                        colors.onSurface
                                    },
                                    summaryColor = if (isSelected) {
                                        colors.onSecondaryContainer.copy(alpha = 0.8f)
                                    } else {
                                        colors.onSurfaceVariant
                                    },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(containerColor, CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = page.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = iconTint,
                                            )
                                        }
                                    },
                                    trailing = if (twoPane) SettingsRowTrailing.NONE else SettingsRowTrailing.CHEVRON,
                                    trailingContent = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = stringResource(R.string.settings_selected_page),
                                                tint = colors.onSecondaryContainer,
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    onClick = { onPageSelected(page) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
