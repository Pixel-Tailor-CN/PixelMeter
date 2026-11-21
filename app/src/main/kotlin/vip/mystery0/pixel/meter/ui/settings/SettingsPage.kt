package vip.mystery0.pixel.meter.ui.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import vip.mystery0.pixel.meter.R

enum class SettingsPage(
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
    val group: Int,
) {
    General(R.string.settings_category_general, Icons.Rounded.Tune, 0),
    Notification(R.string.settings_category_notification, Icons.Rounded.Notifications, 0),
    Overlay(R.string.settings_category_overlay, Icons.Rounded.PictureInPictureAlt, 0),
    Background(R.string.settings_category_background, Icons.Rounded.BatterySaver, 1),
    About(R.string.settings_category_about, Icons.Rounded.Info, 1),
}
