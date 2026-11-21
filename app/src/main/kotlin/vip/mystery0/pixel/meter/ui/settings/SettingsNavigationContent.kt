package vip.mystery0.pixel.meter.ui.settings

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Exiting pages remain composed until the transition finishes; dialogs and side effects should guard on this. */
internal val LocalNavigationActive = staticCompositionLocalOf { true }

/**
 * Transitions between settings destinations by depth with RTL awareness and Predictive Back preview.
 * Modeled on PixelTelo's NavigationContent.
 */
@Composable
internal fun <T> SettingsNavigationContent(
    targetState: T,
    depth: (T) -> Int,
    modifier: Modifier = Modifier,
    canPreviewBack: Boolean = false,
    backTarget: T? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    val parentActive = LocalNavigationActive.current
    val layoutDirection = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    val state = remember { SeekableTransitionState(targetState) }
    val transition = rememberTransition(state, label = "settingsNavigation")
    val latestTarget by rememberUpdatedState(targetState)
    val latestCanPreviewBack by rememberUpdatedState(canPreviewBack)
    val latestBackTarget by rememberUpdatedState(backTarget)
    val latestOnBack by rememberUpdatedState(onBack)
    var predicting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var hasPreviewTarget by remember { mutableStateOf(false) }
    var previewTarget by remember { mutableStateOf(targetState) }

    // Commit navigation only when the predictive back gesture completes.
    PredictiveBackHandler(enabled = parentActive && onBack != null) { events ->
        val start = latestTarget
        val canPreview = latestCanPreviewBack
        @Suppress("UNCHECKED_CAST")
        val target = latestBackTarget as T
        hasPreviewTarget = canPreview
        if (canPreview) {
            previewTarget = target
        }
        progress = 0f
        predicting = canPreview
        try {
            events.collect { event ->
                if (latestTarget != start) throw CancellationException("Navigation changed")
                progress = event.progress.coerceIn(0f, 1f)
            }
            if (latestTarget == start) latestOnBack?.invoke()
        } catch (_: CancellationException) {
            // Rewind smoothly when the user cancels the back gesture.
        } finally {
            predicting = false
        }
    }

    LaunchedEffect(predicting, progress, hasPreviewTarget, previewTarget) {
        if (predicting && hasPreviewTarget) {
            state.seekTo(progress, previewTarget)
        }
    }
    LaunchedEffect(targetState, predicting) {
        if (predicting) return@LaunchedEffect
        if (state.currentState != targetState) {
            state.animateTo(targetState)
        } else if (state.targetState != targetState) {
            // Cancelled predictive back: rewind along the same transition to avoid jumping.
            val duration = (state.fraction * transition.totalDurationNanos / 1_000_000).toInt()
            coroutineScope {
                animate(state.fraction, 0f, animationSpec = tween(duration)) { value, _ ->
                    launch { state.seekTo(value) }
                }
            }
            state.snapTo(targetState)
        }
    }

    transition.AnimatedContent(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds(),
        transitionSpec = {
            val forward = depth(this.targetState) >= depth(initialState)
            val enter = if (forward) {
                settingsEnterTransition(layoutDirection)
            } else {
                settingsPopEnterTransition(layoutDirection)
            }
            val exit = if (forward) {
                settingsExitTransition(layoutDirection)
            } else {
                settingsPopExitTransition(layoutDirection)
            }
            (enter togetherWith exit)
                .using(null)
                .apply { targetContentZIndex = if (forward) 1f else -1f }
        },
    ) { page ->
        val active = parentActive && page == targetState
        val interaction = if (active && !predicting) {
            Modifier
        } else {
            Modifier
                .clearAndSetSemantics { }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    }
                }
        }
        CompositionLocalProvider(LocalNavigationActive provides active) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .then(interaction),
            ) {
                content(page)
            }
        }
    }
}
