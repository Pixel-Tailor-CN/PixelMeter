package vip.mystery0.pixel.meter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import vip.mystery0.pixel.meter.data.source.NetSpeedData
import vip.mystery0.pixel.meter.format.SpeedFormatter
import vip.mystery0.pixel.meter.ui.MainViewModel
import vip.mystery0.pixel.meter.ui.onboarding.OnboardingScreen
import vip.mystery0.pixel.meter.ui.settings.SettingsActivity
import vip.mystery0.pixel.meter.ui.theme.PixelPulseTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_ONBOARDING = "extra_open_onboarding"
    }

    private val viewModel by viewModels<MainViewModel>()
    private var manualOnboardingRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        manualOnboardingRequested = intent.getBooleanExtra(EXTRA_OPEN_ONBOARDING, false)
        intent.removeExtra(EXTRA_OPEN_ONBOARDING)
        setContent {
            val appThemeMode by viewModel.appThemeMode.collectAsState()
            val appThemeColor by viewModel.appThemeColor.collectAsState()
            val useAmoledBlack by viewModel.isAppThemeUseAmoledBlack.collectAsState()
            PixelPulseTheme(
                themeMode = appThemeMode,
                themeColor = appThemeColor,
                useAmoledBlack = useAmoledBlack
            ) {
                AppContent()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_ONBOARDING, false)) {
            manualOnboardingRequested = true
            intent.removeExtra(EXTRA_OPEN_ONBOARDING)
        }
    }

    @Composable
    private fun AppContent() {
        val context = LocalContext.current
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val isOnboardingShown by viewModel.isOnboardingShown.collectAsState()
        var notificationPermissionGranted by remember {
            mutableStateOf(
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            )
        }
        var overlayPermissionGranted by remember {
            mutableStateOf(Settings.canDrawOverlays(context))
        }

        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            notificationPermissionGranted = granted
        }
        val overlayPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            overlayPermissionGranted = Settings.canDrawOverlays(context)
        }

        DisposableEffect(lifecycle, context) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    notificationPermissionGranted =
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    overlayPermissionGranted = Settings.canDrawOverlays(context)
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }

        if (!isOnboardingShown || manualOnboardingRequested) {
            OnboardingScreen(
                liveUpdateSupported = Build.VERSION.SDK_INT >= 36,
                notificationPermissionGranted = notificationPermissionGranted,
                overlayPermissionGranted = overlayPermissionGranted,
                onSkip = {
                    viewModel.skipOnboarding()
                    manualOnboardingRequested = false
                },
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notificationPermissionGranted = true
                    }
                },
                onRequestOverlayPermission = {
                    overlayPermissionLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                    )
                },
                onComplete = { notificationEnabled, liveUpdateEnabled, overlayEnabled, canStartService ->
                    viewModel.completeOnboarding(
                        notificationEnabled = notificationEnabled,
                        liveUpdateEnabled = liveUpdateEnabled,
                        overlayEnabled = overlayEnabled,
                        startServiceAfterSaving = canStartService
                    )
                    manualOnboardingRequested = false
                }
            )
        } else {
            HomeScreen()
        }
    }

    private fun launchSpeedTest(context: android.content.Context) {
        val url = "https://speed.cloudflare.com"
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, url.toUri())
    }

    @Composable
    fun HomeScreen() {
        val context = LocalContext.current
        val speed by viewModel.currentSpeed.collectAsState()
        val isServiceRunning by viewModel.isServiceRunning.collectAsState()
        val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsState()
        val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsState()
        val isHideFromRecents by viewModel.isHideFromRecents.collectAsState(initial = false)
        val serviceError by viewModel.serviceStartError.collectAsState()
        val speedUnit by viewModel.speedUnit.collectAsState()
        val minSpeedUnit by viewModel.minSpeedUnit.collectAsState()
        val speedRateUnit by viewModel.speedRateUnit.collectAsState()

        LaunchedEffect(isHideFromRecents) {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val tasks = activityManager.appTasks
            if (tasks.isNotEmpty()) {
                tasks[0].setExcludeFromRecents(isHideFromRecents)
            }
        }

        // Permission Launcher
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    viewModel.clearError()
                }
            }
        )



        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = {
                            val intent = Intent(context, SettingsActivity::class.java)
                            context.startActivity(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.content_description_settings)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SpeedDashboardCard(speed, speedUnit, minSpeedUnit, speedRateUnit)
                }

                // Service Permission Error Card
                if (serviceError != null) {
                    item {
                        Text(
                            stringResource(R.string.title_configuration),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    serviceError?.first ?: stringResource(R.string.error_unknown),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Spacer(modifier = Modifier.weight(1F))
                                    Button(onClick = {
                                        serviceError?.let { (_, action) ->
                                            if (action == Settings.ACTION_APP_NOTIFICATION_SETTINGS) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    val intent = Intent(action)
                                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    intent.putExtra(
                                                        Settings.EXTRA_APP_PACKAGE,
                                                        context.packageName
                                                    )
                                                    context.startActivity(intent)
                                                }
                                            } else {
                                                val intent = Intent(action)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                intent.data =
                                                    "package:${context.packageName}".toUri()
                                                context.startActivity(intent)
                                                viewModel.clearError()
                                            }
                                        }
                                    }) {
                                        Text(stringResource(R.string.action_request_fix))
                                    }
                                    TextButton(
                                        onClick = { viewModel.clearError() },
                                    ) {
                                        Text(stringResource(R.string.action_dismiss))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.title_monitor_control),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isServiceRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isServiceRunning) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (isServiceRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isServiceRunning) stringResource(R.string.status_monitor_running) else stringResource(
                                        R.string.status_monitor_stopped
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isServiceRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Start Button
                                Button(
                                    onClick = { viewModel.startService() },
                                    enabled = !isServiceRunning,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.action_start))
                                }
                                // Stop Button
                                Button(
                                    onClick = { viewModel.stopService() },
                                    enabled = isServiceRunning,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.action_stop))
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.title_feature_config),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    ConfigRow(
                        title = stringResource(R.string.config_enable_overlay),
                        subtitle = stringResource(R.string.config_enable_overlay_desc),
                        checked = isOverlayEnabled,
                        onCheckedChange = { viewModel.setOverlayEnabled(it) }
                    )
                }

                item {
                    ConfigRow(
                        title = stringResource(R.string.config_enable_notification),
                        subtitle = stringResource(R.string.config_enable_notification_desc),
                        checked = isNotificationEnabled,
                        onCheckedChange = { viewModel.setNotificationEnabled(it) }
                    )
                }

                item {
                    Text(
                        stringResource(R.string.title_tools),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { launchSpeedTest(context) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.action_speed_test),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.desc_speed_test),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedDashboardCard(
    speed: NetSpeedData,
    speedUnit: Int = 0,
    minSpeedUnit: Int = 0,
    speedRateUnit: Int = 0
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.label_total_speed),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                SpeedFormatter.formatSpeedLine(speed.totalSpeed, speedUnit, minSpeedUnit, speedRateUnit),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.label_download),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "▼ " + SpeedFormatter.formatSpeedLine(
                            speed.downloadSpeed,
                            speedUnit,
                            minSpeedUnit,
                            speedRateUnit
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.label_upload),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "▲ " + SpeedFormatter.formatSpeedLine(
                            speed.uploadSpeed,
                            speedUnit,
                            minSpeedUnit,
                            speedRateUnit
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
