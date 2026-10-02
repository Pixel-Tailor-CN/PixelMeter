package vip.mystery0.pixel.meter.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.meter.R
import vip.mystery0.pixel.meter.permissions.DisplayPermissionState

@Composable
fun OnboardingScreen(
    liveUpdateSupported: Boolean,
    notificationPermissionGranted: Boolean,
    overlayPermissionGranted: Boolean,
    onSkip: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onComplete: (
        notificationEnabled: Boolean,
        liveUpdateEnabled: Boolean,
        overlayEnabled: Boolean,
        canStartService: Boolean
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var notificationEnabled by remember { mutableStateOf(false) }
    var liveUpdateEnabled by remember { mutableStateOf(false) }
    var overlayEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.onboarding_title)) },
                actions = {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_step, step + 1, 3),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            when (step) {
                0 -> WelcomeStep(
                    onStart = { step = 1 },
                    onSkip = onSkip
                )

                1 -> DisplayModeStep(
                    liveUpdateSupported = liveUpdateSupported,
                    notificationEnabled = notificationEnabled,
                    liveUpdateEnabled = liveUpdateEnabled,
                    overlayEnabled = overlayEnabled,
                    onNotificationEnabledChange = {
                        notificationEnabled = it
                        if (!it) liveUpdateEnabled = false
                    },
                    onLiveUpdateEnabledChange = { liveUpdateEnabled = it },
                    onOverlayEnabledChange = { overlayEnabled = it },
                    onUseRecommended = {
                        notificationEnabled = true
                        liveUpdateEnabled = liveUpdateSupported
                        overlayEnabled = false
                    },
                    onBack = { step = 0 },
                    onNext = { step = 2 }
                )

                else -> PermissionStep(
                    notificationEnabled = notificationEnabled,
                    overlayEnabled = overlayEnabled,
                    notificationPermissionGranted = notificationPermissionGranted,
                    overlayPermissionGranted = overlayPermissionGranted,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onBack = { step = 1 },
                    onFinish = {
                        onComplete(
                            notificationEnabled,
                            liveUpdateEnabled,
                            overlayEnabled,
                            notificationEnabled || overlayEnabled
                        )
                    },
                    onFinishLater = {
                        onComplete(
                            notificationEnabled,
                            liveUpdateEnabled,
                            overlayEnabled,
                            false
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WelcomeStep(
    onStart: () -> Unit,
    onSkip: () -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_welcome_title),
        style = MaterialTheme.typography.headlineMedium
    )
    Text(
        text = stringResource(R.string.onboarding_welcome_desc),
        style = MaterialTheme.typography.bodyLarge
    )
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = stringResource(R.string.onboarding_vpn_benefit),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
    Button(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.onboarding_start_setup))
    }
    TextButton(
        onClick = onSkip,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.onboarding_setup_later))
    }
}

@Composable
private fun DisplayModeStep(
    liveUpdateSupported: Boolean,
    notificationEnabled: Boolean,
    liveUpdateEnabled: Boolean,
    overlayEnabled: Boolean,
    onNotificationEnabledChange: (Boolean) -> Unit,
    onLiveUpdateEnabledChange: (Boolean) -> Unit,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onUseRecommended: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_display_title),
        style = MaterialTheme.typography.headlineSmall
    )
    Text(
        text = stringResource(R.string.onboarding_display_desc),
        style = MaterialTheme.typography.bodyMedium
    )
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_recommended_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (liveUpdateSupported) {
                    stringResource(R.string.onboarding_recommended_desc)
                } else {
                    stringResource(R.string.onboarding_recommended_legacy_desc)
                },
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(onClick = onUseRecommended) {
                Text(stringResource(R.string.onboarding_use_recommended))
            }
        }
    }
    OnboardingSwitchRow(
        title = stringResource(R.string.config_enable_notification),
        description = stringResource(R.string.onboarding_notification_desc),
        checked = notificationEnabled,
        onCheckedChange = onNotificationEnabledChange
    )
    OnboardingSwitchRow(
        title = stringResource(R.string.config_enable_live_update),
        description = if (liveUpdateSupported) {
            stringResource(R.string.onboarding_live_update_desc)
        } else {
            stringResource(R.string.onboarding_live_update_unavailable)
        },
        checked = liveUpdateEnabled,
        enabled = notificationEnabled && liveUpdateSupported,
        onCheckedChange = onLiveUpdateEnabledChange
    )
    OnboardingSwitchRow(
        title = stringResource(R.string.config_enable_overlay),
        description = stringResource(R.string.onboarding_overlay_desc),
        checked = overlayEnabled,
        onCheckedChange = onOverlayEnabledChange
    )
    Text(
        text = stringResource(R.string.onboarding_foreground_notification_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.onboarding_back))
        }
        Button(
            onClick = onNext,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.onboarding_next))
        }
    }
}

@Composable
private fun OnboardingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun PermissionStep(
    notificationEnabled: Boolean,
    overlayEnabled: Boolean,
    notificationPermissionGranted: Boolean,
    overlayPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onFinishLater: () -> Unit
) {
    val permissions = DisplayPermissionState(
        notificationEnabled = notificationEnabled,
        overlayEnabled = overlayEnabled,
        notificationGranted = notificationPermissionGranted,
        overlayGranted = overlayPermissionGranted
    )
    val requiredPermissionsGranted = permissions.canStartMonitoring
    Text(
        text = stringResource(R.string.onboarding_permission_title),
        style = MaterialTheme.typography.headlineSmall
    )
    Text(
        text = stringResource(R.string.onboarding_permission_desc),
        style = MaterialTheme.typography.bodyMedium
    )
    if (notificationEnabled) {
        PermissionCard(
            title = stringResource(R.string.settings_permission_notification),
            description = stringResource(R.string.onboarding_notification_permission_desc),
            granted = notificationPermissionGranted,
            onRequest = onRequestNotificationPermission
        )
    }
    if (overlayEnabled) {
        PermissionCard(
            title = stringResource(R.string.settings_permission_overlay),
            description = stringResource(R.string.onboarding_overlay_permission_desc),
            granted = overlayPermissionGranted,
            onRequest = onRequestOverlayPermission
        )
    }
    if (permissions.missingNotificationPermission && overlayEnabled) {
        Text(
            text = stringResource(R.string.notification_permission_overlay_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (!requiredPermissionsGranted) {
        Text(
            text = stringResource(R.string.onboarding_permission_optional_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.onboarding_back))
        }
        Button(
            onClick = onFinish,
            enabled = requiredPermissionsGranted,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                stringResource(
                    if (permissions.hasSelectedDisplay) {
                        R.string.onboarding_finish_and_start
                    } else {
                        R.string.onboarding_finish
                    }
                )
            )
        }
    }
    if (!requiredPermissionsGranted) {
        TextButton(
            onClick = onFinishLater,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.onboarding_finish_later))
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (granted) {
                    stringResource(R.string.settings_permission_granted)
                } else {
                    stringResource(R.string.settings_permission_denied)
                },
                color = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            if (!granted) {
                OutlinedButton(onClick = onRequest) {
                    Text(stringResource(R.string.onboarding_grant_permission))
                }
            }
        }
    }
}
