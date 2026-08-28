package vip.mystery0.pixel.meter.ui.overlay

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.launch
import vip.mystery0.pixel.meter.data.repository.NetworkRepository
import vip.mystery0.pixel.meter.data.source.NetSpeedData
import vip.mystery0.pixel.meter.format.SpeedFormatter
import vip.mystery0.pixel.meter.ui.theme.PixelPulseTheme
import kotlin.math.roundToInt

class OverlayWindow(
    private val context: Context,
    private val repository: NetworkRepository
) : LifecycleOwner, ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private companion object {
        const val LOW_TRAFFIC_HIDE_SAMPLE_COUNT = 3
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var lifecycleRegistry = LifecycleRegistry(this)
    private var savedStateRegistryController = SavedStateRegistryController.create(this)
    private var store = ViewModelStore()

    private var speedState by mutableStateOf(NetSpeedData(0, 0))
    private var speedUpdateVersion by mutableLongStateOf(0L)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun show() {
        if (view != null) return

        lifecycleRegistry = LifecycleRegistry(this)
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)
        store = ViewModelStore()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        lifecycleScope.launch {
            val (initialX, initialY) = repository.getOverlayPosition()

            val isShowOnStatusBarInitially = repository.isOverlayShowOnStatusBar.value
            val extraFlags =
                if (isShowOnStatusBarInitially) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0

            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or extraFlags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = initialX
                y = initialY
                if (isShowOnStatusBarInitially) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            val composeView = ComposeView(context)
            composeView.setViewTreeLifecycleOwner(this@OverlayWindow)
            composeView.setViewTreeViewModelStoreOwner(this@OverlayWindow)
            composeView.setViewTreeSavedStateRegistryOwner(this@OverlayWindow)

            composeView.setContent {
                val appThemeMode by repository.appThemeMode.collectAsState()
                val appThemeColor by repository.appThemeColor.collectAsState()
                val useAmoledBlack by repository.isAppThemeUseAmoledBlack.collectAsState()
                PixelPulseTheme(
                    themeMode = appThemeMode,
                    themeColor = appThemeColor,
                    useAmoledBlack = useAmoledBlack
                ) {
                    val isLocked by repository.isOverlayLocked.collectAsState()
                    val isShowOnStatusBar by repository.isOverlayShowOnStatusBar.collectAsState()
                    val bgColor by repository.overlayBgColor.collectAsState()
                    val textColor by repository.overlayTextColor.collectAsState()
                    val cornerRadius by repository.overlayCornerRadius.collectAsState()
                    val padding by repository.overlayPadding.collectAsState()
                    val textSize by repository.overlayTextSize.collectAsState()
                    val textUp by repository.overlayTextUp.collectAsState()
                    val textDown by repository.overlayTextDown.collectAsState()
                    val upFirst by repository.overlayOrderUpFirst.collectAsState()
                    val isOverlayHideBackground by repository.isOverlayHideBackground
                        .collectAsState()
                    val overlayX by repository.overlayX.collectAsState()
                    val overlayY by repository.overlayY.collectAsState()
                    val direction by repository.overlayDirection.collectAsState()
                    val displayMode by repository.overlayDisplayMode.collectAsState()
                    val alignment by repository.overlayAlignment.collectAsState()
                    val meterSpacing by repository.overlayMeterSpacing.collectAsState()
                    val isOverlayPortraitOnly by repository.isOverlayPortraitOnly.collectAsState()
                    val isOverlayHideInImmersiveMode by repository.isOverlayHideInImmersiveMode
                        .collectAsState()
                    val overlayAutoHideThreshold by repository.overlayAutoHideThreshold
                        .collectAsState()
                    val isOverlayUseDefaultColors by repository.isOverlayUseDefaultColors.collectAsState()
                    val speedUnit by repository.speedUnit.collectAsState()
                    val minSpeedUnit by repository.minSpeedUnit.collectAsState()
                    val speedRateUnit by repository.speedRateUnit.collectAsState()
                    val context = LocalContext.current
                    val initialConfig = LocalConfiguration.current
                    var orientation by remember { mutableIntStateOf(initialConfig.orientation) }
                    var isStatusBarVisible by remember { mutableStateOf(true) }
                    var isNavigationBarVisible by remember { mutableStateOf(true) }
                    var lowTrafficSamples by remember { mutableIntStateOf(0) }
                    var lowTrafficDisplayMode by remember { mutableIntStateOf(displayMode) }

                    DisposableEffect(context) {
                        val callbacks = object : ComponentCallbacks {
                            override fun onConfigurationChanged(newConfig: Configuration) {
                                orientation = newConfig.orientation
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onLowMemory() {}
                        }
                        context.applicationContext.registerComponentCallbacks(callbacks)
                        onDispose {
                            context.applicationContext.unregisterComponentCallbacks(callbacks)
                        }
                    }

                    DisposableEffect(composeView) {
                        val insetsListener = View.OnApplyWindowInsetsListener { _, insets ->
                            isStatusBarVisible =
                                insets.isVisible(WindowInsets.Type.statusBars())
                            isNavigationBarVisible =
                                insets.isVisible(WindowInsets.Type.navigationBars())
                            insets
                        }
                        val attachListener = object : View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(view: View) {
                                view.requestApplyInsets()
                                view.removeOnAttachStateChangeListener(this)
                            }

                            override fun onViewDetachedFromWindow(view: View) = Unit
                        }
                        composeView.setOnApplyWindowInsetsListener(insetsListener)
                        if (composeView.isAttachedToWindow) {
                            composeView.requestApplyInsets()
                        } else {
                            composeView.addOnAttachStateChangeListener(attachListener)
                        }
                        onDispose {
                            composeView.setOnApplyWindowInsetsListener(null)
                            composeView.removeOnAttachStateChangeListener(attachListener)
                        }
                    }

                    LaunchedEffect(isShowOnStatusBar) {
                        params?.let { p ->
                            var changed = false
                            if (isShowOnStatusBar) {
                                if ((p.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS) == 0) {
                                    p.flags =
                                        p.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                                    changed = true
                                }
                                if (p.layoutInDisplayCutoutMode != WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES) {
                                    p.layoutInDisplayCutoutMode =
                                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                                    changed = true
                                }
                            } else {
                                if ((p.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS) != 0) {
                                    p.flags =
                                        p.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv()
                                    changed = true
                                }
                                if (p.layoutInDisplayCutoutMode != WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT) {
                                    p.layoutInDisplayCutoutMode =
                                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                                    changed = true
                                }
                            }
                            if (changed) {
                                windowManager.updateViewLayout(composeView, p)
                            }
                        }
                    }

                    LaunchedEffect(overlayX, overlayY) {
                        params?.let { p ->
                            if (p.x != overlayX || p.y != overlayY) {
                                p.x = overlayX
                                p.y = overlayY
                                windowManager.updateViewLayout(composeView, p)
                            }
                        }
                    }

                    val shouldHideForLandscape =
                        isOverlayPortraitOnly &&
                                orientation == Configuration.ORIENTATION_LANDSCAPE
                    val shouldHideForImmersiveMode =
                        isOverlayHideInImmersiveMode &&
                                (!isStatusBarVisible || !isNavigationBarVisible)
                    val lowTrafficSpeed = when (displayMode) {
                        1 -> speedState.uploadSpeed
                        2 -> speedState.downloadSpeed
                        else -> speedState.totalSpeed
                    }
                    LaunchedEffect(speedUpdateVersion, overlayAutoHideThreshold, displayMode) {
                        if (displayMode != lowTrafficDisplayMode) {
                            lowTrafficDisplayMode = displayMode
                            lowTrafficSamples = 0
                            return@LaunchedEffect
                        }
                        lowTrafficSamples = when {
                            speedUpdateVersion == 0L -> 0
                            overlayAutoHideThreshold <= 0L -> 0
                            lowTrafficSpeed < overlayAutoHideThreshold ->
                                (lowTrafficSamples + 1)
                                    .coerceAtMost(LOW_TRAFFIC_HIDE_SAMPLE_COUNT)

                            else -> 0
                        }
                    }
                    val shouldHideForLowTraffic =
                        overlayAutoHideThreshold > 0L &&
                                lowTrafficSamples >= LOW_TRAFFIC_HIDE_SAMPLE_COUNT

                    val shouldHideOverlay =
                        shouldHideForLandscape || shouldHideForImmersiveMode ||
                                shouldHideForLowTraffic
                    LaunchedEffect(shouldHideOverlay) {
                        // Keep the root ComposeView attached to receive WindowInsets and restore visibility with system bars.
                        composeView.visibility = View.VISIBLE
                        composeView.alpha = if (shouldHideOverlay) 0f else 1f

                        params?.let { p ->
                            val isNotTouchable =
                                (p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0
                            val shouldUpdateTouchFlag =
                                shouldHideOverlay != isNotTouchable
                            if (shouldUpdateTouchFlag) {
                                p.flags = if (shouldHideOverlay) {
                                    p.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                } else {
                                    p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                                }
                                windowManager.updateViewLayout(composeView, p)
                            }
                        }
                    }

                    OverlayContent(
                        speed = speedState,
                        useDefaultColors = isOverlayUseDefaultColors,
                        hideBackground = isOverlayHideBackground,
                        bgColor = bgColor,
                        textColor = textColor,
                        cornerRadius = cornerRadius,
                        padding = padding,
                        textSize = textSize,
                        textUp = textUp,
                        textDown = textDown,
                        upFirst = upFirst,
                        direction = direction,
                        displayMode = displayMode,
                        alignment = alignment,
                        meterSpacing = meterSpacing,
                        speedUnit = speedUnit,
                        minSpeedUnit = minSpeedUnit,
                        speedRateUnit = speedRateUnit,
                        onDrag = { x, y ->
                            if (!isLocked) {
                                params?.let { p ->
                                    p.x += x.roundToInt()
                                    p.y += y.roundToInt()
                                    windowManager.updateViewLayout(composeView, p)
                                }
                            }
                        },
                        onDragEnd = {
                            params?.let { p ->
                                repository.saveOverlayPosition(p.x, p.y)
                            }
                        }
                    )
                }
            }

            windowManager.addView(composeView, params)
            view = composeView
        }

    }

    fun update(speed: NetSpeedData) {
        speedState = speed
        speedUpdateVersion++
    }

    fun hide() {
        if (view != null) {
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.w("OverlayWindow", "removeView failed", e)
            }
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
            view = null
        }
    }
}

@Composable
fun OverlayContent(
    speed: NetSpeedData,
    useDefaultColors: Boolean,
    hideBackground: Boolean,
    bgColor: Int,
    textColor: Int,
    cornerRadius: Int,
    padding: Int,
    textSize: Float,
    textUp: String,
    textDown: String,
    upFirst: Boolean,
    direction: Int = 0,
    displayMode: Int = 0,
    alignment: Int = 0,
    meterSpacing: Int = 8,
    speedUnit: Int = 0,
    minSpeedUnit: Int = 0,
    speedRateUnit: Int = 0,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val textColorValue = if (useDefaultColors) MaterialTheme.colorScheme.onSurface else Color(textColor)
    val textStyle = MaterialTheme.typography.labelSmall.copy(fontSize = textSize.sp)
    Surface(
        shape = RoundedCornerShape(cornerRadius.dp),
        color = when {
            hideBackground -> Color.Transparent
            useDefaultColors -> MaterialTheme.colorScheme.surface
            else -> Color(bgColor)
        },
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                },
                onDragEnd = onDragEnd
            )
        }
    ) {
        val upSpeed = SpeedFormatter.formatSpeedLine(
            speed.uploadSpeed, speedUnit, minSpeedUnit, speedRateUnit
        )
        val downSpeed = SpeedFormatter.formatSpeedLine(
            speed.downloadSpeed, speedUnit, minSpeedUnit, speedRateUnit
        )
        val singleText = when (displayMode) {
            1 -> "$textUp$upSpeed"
            2 -> "$textDown$downSpeed"
            3 -> SpeedFormatter.formatSpeedLine(
                speed.totalSpeed, speedUnit, minSpeedUnit, speedRateUnit
            )
            else -> null
        }
        if (singleText != null) {
            Text(
                text = singleText,
                modifier = Modifier.padding(
                    horizontal = padding.coerceAtLeast(0).dp,
                    vertical = (padding / 2).coerceAtLeast(0).dp
                ),
                style = textStyle,
                color = textColorValue
            )
            return@Surface
        }

        val prefix1 = if (upFirst) textUp else textDown
        val prefix2 = if (upFirst) textDown else textUp
        val speed1 = if (upFirst) upSpeed else downSpeed
        val speed2 = if (upFirst) downSpeed else upSpeed

        if (direction == 0) {
            Row(
                modifier = Modifier.padding(
                    horizontal = padding.coerceAtLeast(0).dp,
                    vertical = (padding / 2).coerceAtLeast(0).dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "$prefix1$speed1", style = textStyle, color = textColorValue)
                Spacer(modifier = Modifier.width(meterSpacing.coerceAtLeast(0).dp))
                Text(text = "$prefix2$speed2", style = textStyle, color = textColorValue)
            }
        } else {
            val horizontalAlignment = when (alignment) {
                1 -> Alignment.CenterHorizontally
                2 -> Alignment.End
                else -> Alignment.Start
            }
            Row(
                modifier = Modifier.padding(
                    horizontal = padding.coerceAtLeast(0).dp,
                    vertical = (padding / 2).coerceAtLeast(0).dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(text = prefix1, style = textStyle, color = textColorValue)
                    Text(text = prefix2, style = textStyle, color = textColorValue)
                }
                Column(horizontalAlignment = horizontalAlignment) {
                    Text(text = speed1, style = textStyle, color = textColorValue)
                    Text(text = speed2, style = textStyle, color = textColorValue)
                }
            }
        }
    }
}
