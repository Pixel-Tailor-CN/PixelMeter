package vip.mystery0.pixel.meter.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import vip.mystery0.pixel.meter.R

@Composable
fun BackgroundSettingsSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val isIgnoringBatteryOptimizations by viewModel.isIgnoringBatteryOptimizations.collectAsState()
    val isHideFromRecents by viewModel.isHideFromRecents.collectAsState(initial = false)

    val batterySummary = stringResource(
        if (isIgnoringBatteryOptimizations) {
            R.string.settings_ignore_battery_optimizations_on
        } else {
            R.string.settings_ignore_battery_optimizations_off
        },
    )

    SettingsSection(title = stringResource(R.string.settings_section_background_execution)) {
        SettingsGroup(
            rows = listOf(
                SettingsRowItem("ignore-battery-optimizations") { modifier ->
                    SettingsItemRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_ignore_battery_optimizations_title),
                        summary = batterySummary,
                        icon = Icons.Rounded.BatterySaver,
                        trailing = SettingsRowTrailing.EXTERNAL_LINK,
                        onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            intent.data = "package:${context.packageName}".toUri()
                            context.startActivity(intent)
                        },
                    )
                },
                SettingsRowItem("hide-from-recents") { modifier ->
                    SettingsSwitchRow(
                        modifier = modifier,
                        checked = isHideFromRecents,
                        onCheckedChange = { viewModel.setHideFromRecents(it) },
                        title = stringResource(R.string.settings_hide_from_recents_title),
                        summary = stringResource(R.string.settings_hide_from_recents_desc),
                        icon = Icons.Rounded.VisibilityOff,
                    )
                },
                SettingsRowItem("dont-kill-my-app") { modifier ->
                    SettingsItemRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_dont_kill_my_app_title),
                        summary = stringResource(R.string.settings_dont_kill_my_app_desc),
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        trailing = SettingsRowTrailing.EXTERNAL_LINK,
                        onClick = {
                            val customTabsIntent = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                            customTabsIntent.launchUrl(context, "https://dontkillmyapp.com/".toUri())
                        },
                    )
                },
            ),
        )
    }
}
