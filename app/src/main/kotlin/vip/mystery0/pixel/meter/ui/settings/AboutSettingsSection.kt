package vip.mystery0.pixel.meter.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.meter.BuildConfig
import vip.mystery0.pixel.meter.R

@Composable
fun AboutSettingsSection() {
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        SettingsGroup(
            rows = listOf(
                SettingsRowItem("app-version") { modifier ->
                    SettingsItemRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_app_version),
                        summary = BuildConfig.VERSION_NAME,
                        icon = Icons.Rounded.Info,
                        trailing = SettingsRowTrailing.NONE,
                    )
                },
                SettingsRowItem("github") { modifier ->
                    SettingsItemRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_github),
                        summary = "https://github.com/Pixel-Tailor-CN/PixelMeter",
                        icon = Icons.Rounded.Code,
                        trailing = SettingsRowTrailing.EXTERNAL_LINK,
                        onClick = { uriHandler.openUri("https://github.com/Pixel-Tailor-CN/PixelMeter") },
                    )
                },
                SettingsRowItem("pixel-tailor") { modifier ->
                    SettingsItemRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_pixel_tailor),
                        summary = stringResource(R.string.settings_pixel_tailor_desc),
                        leadingContent = {
                            Image(
                                painter = painterResource(R.drawable.ic_pixel_tailor),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        trailing = SettingsRowTrailing.EXTERNAL_LINK,
                        onClick = { uriHandler.openUri("https://pixel.mystery0.app") },
                    )
                },
                SettingsRowItem("telegram-channel") { modifier ->
                    SettingsItemRow(
                        modifier = modifier,
                        title = stringResource(R.string.settings_telegram_channel),
                        summary = stringResource(R.string.settings_telegram_channel_desc),
                        icon = Icons.Rounded.Forum,
                        trailing = SettingsRowTrailing.EXTERNAL_LINK,
                        onClick = { uriHandler.openUri("https://t.me/pixel_tailor_cn") },
                    )
                },
            ),
        )
    }
}
