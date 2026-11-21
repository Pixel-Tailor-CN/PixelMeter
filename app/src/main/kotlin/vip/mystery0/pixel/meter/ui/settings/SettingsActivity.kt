package vip.mystery0.pixel.meter.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import vip.mystery0.pixel.meter.R
import vip.mystery0.pixel.meter.ui.theme.PixelPulseTheme
import vip.mystery0.pixel.meter.util.AppLocaleManager

private val SettingsTwoPaneMinWidth = 840.dp
private val SettingsSidebarWidth = 320.dp

class SettingsActivity : ComponentActivity() {
    private val viewModel by viewModels<SettingsViewModel>()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrapBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appThemeMode by viewModel.appThemeMode.collectAsState()
            val appThemeColor by viewModel.appThemeColor.collectAsState()
            val useAmoledBlack by viewModel.isAppThemeUseAmoledBlack.collectAsState()
            PixelPulseTheme(
                themeMode = appThemeMode,
                themeColor = appThemeColor,
                useAmoledBlack = useAmoledBlack,
            ) {
                val windowBackground = MaterialTheme.colorScheme.background
                SideEffect {
                    window.setBackgroundDrawable(windowBackground.toArgb().toDrawable())
                }
                SettingsScreen()
            }
        }
    }

    @Composable
    fun SettingsScreen() {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshOverlaySettings()
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }

        var selectedPageName by rememberSaveable { mutableStateOf<String?>(null) }
        val selectedPage = selectedPageName?.let { name ->
            SettingsPage.entries.firstOrNull { it.name == name }
        }
        val pageStateHolder = rememberSaveableStateHolder()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            val isTwoPane = maxWidth >= SettingsTwoPaneMinWidth

            if (isTwoPane) {
                val effectivePage = selectedPage ?: SettingsPage.General
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.title_settings)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = stringResource(R.string.content_description_back),
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                scrolledContainerColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    },
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding),
                    ) {
                        pageStateHolder.SaveableStateProvider("settings-home") {
                            MainSettingsPage(
                                viewModel = viewModel,
                                selectedPage = effectivePage,
                                twoPane = true,
                                onPageSelected = { selectedPageName = it.name },
                                modifier = Modifier
                                    .width(SettingsSidebarWidth)
                                    .fillMaxHeight(),
                            )
                        }

                        VerticalDivider()

                        pageStateHolder.SaveableStateProvider(effectivePage.name) {
                            SettingsDetailPane(
                                page = effectivePage,
                                viewModel = viewModel,
                                showHeaderTitle = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            } else {
                SettingsNavigationContent(
                    targetState = selectedPage,
                    depth = { page -> if (page == null) 0 else 1 },
                    canPreviewBack = selectedPage != null,
                    backTarget = null,
                    onBack = if (selectedPage != null) ({ selectedPageName = null }) else null,
                ) { displayedPage ->
                    val isActive = LocalNavigationActive.current
                    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
                        canScroll = { isActive },
                    )
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        stringResource(
                                            displayedPage?.titleRes ?: R.string.title_settings,
                                        ),
                                    )
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = {
                                            if (displayedPage != null) {
                                                selectedPageName = null
                                            } else {
                                                finish()
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = stringResource(R.string.content_description_back),
                                        )
                                    }
                                },
                                scrollBehavior = scrollBehavior,
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                                ),
                            )
                        },
                    ) { innerPadding ->
                        pageStateHolder.SaveableStateProvider(displayedPage?.name ?: "settings-home") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .consumeWindowInsets(innerPadding),
                            ) {
                                if (displayedPage == null) {
                                    MainSettingsPage(
                                        viewModel = viewModel,
                                        selectedPage = null,
                                        twoPane = false,
                                        onPageSelected = { selectedPageName = it.name },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    SettingsDetailPane(
                                        page = displayedPage,
                                        viewModel = viewModel,
                                        showHeaderTitle = false,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsDetailPane(
    page: SettingsPage,
    viewModel: SettingsViewModel,
    showHeaderTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (showHeaderTitle) {
            Text(
                text = stringResource(page.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    .semantics { heading() },
            )
        }
        when (page) {
            SettingsPage.General -> GeneralSettingsSection(viewModel)
            SettingsPage.Notification -> NotificationSettingsSection(viewModel)
            SettingsPage.Overlay -> OverlaySettingsSection(viewModel)
            SettingsPage.Background -> BackgroundSettingsSection(viewModel)
            SettingsPage.About -> AboutSettingsSection()
        }
    }
}
