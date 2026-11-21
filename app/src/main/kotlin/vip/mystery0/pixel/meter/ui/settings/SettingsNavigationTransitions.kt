package vip.mystery0.pixel.meter.ui.settings

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/** Activity-like whole-page transitions matching PixelText and PixelTelo, with RTL direction support. */
internal fun settingsEnterTransition(direction: Int): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth * direction },
        animationSpec = tween(
            durationMillis = ACTIVITY_FOREGROUND_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
    ) +
        fadeIn(animationSpec = tween(durationMillis = NAV_ENTER_FADE_DURATION_MS)) +
        scaleIn(
            initialScale = ACTIVITY_FOREGROUND_INITIAL_SCALE,
            animationSpec = tween(
                durationMillis = ACTIVITY_FOREGROUND_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
}

internal fun settingsExitTransition(direction: Int): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / ACTIVITY_BACKGROUND_OFFSET_DIVISOR * direction },
        animationSpec = tween(
            durationMillis = ACTIVITY_BACKGROUND_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
    ) +
        activityBackgroundFadeOut() +
        scaleOut(
            targetScale = ACTIVITY_BACKGROUND_TARGET_SCALE,
            animationSpec = tween(
                durationMillis = ACTIVITY_BACKGROUND_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
}

internal fun settingsPopEnterTransition(direction: Int): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / ACTIVITY_BACKGROUND_OFFSET_DIVISOR * direction },
        animationSpec = tween(
            durationMillis = ACTIVITY_BACKGROUND_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
    ) +
        fadeIn(animationSpec = tween(durationMillis = NAV_ENTER_FADE_DURATION_MS)) +
        scaleIn(
            initialScale = ACTIVITY_BACKGROUND_TARGET_SCALE,
            animationSpec = tween(
                durationMillis = ACTIVITY_BACKGROUND_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
}

internal fun settingsPopExitTransition(direction: Int): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth * direction },
        animationSpec = tween(
            durationMillis = ACTIVITY_FOREGROUND_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
    ) +
        activityForegroundFadeOut() +
        scaleOut(
            targetScale = ACTIVITY_FOREGROUND_POP_EXIT_SCALE,
            animationSpec = tween(
                durationMillis = ACTIVITY_FOREGROUND_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
}

private const val ACTIVITY_FOREGROUND_DURATION_MS = 300
private const val ACTIVITY_BACKGROUND_DURATION_MS = 260
private const val ACTIVITY_BACKGROUND_OFFSET_DIVISOR = 4
private const val ACTIVITY_FOREGROUND_INITIAL_SCALE = 0.96f
private const val ACTIVITY_FOREGROUND_POP_EXIT_SCALE = 0.90f
private const val ACTIVITY_BACKGROUND_TARGET_SCALE = 0.92f
private const val NAV_ENTER_FADE_DURATION_MS = 90
private const val NAV_EXIT_FADE_DURATION_MS = 90
private const val NAV_EXIT_FADE_DELAY_MS = 120
private const val NAV_FOREGROUND_EXIT_FADE_DELAY_MS = 210

private fun activityBackgroundFadeOut(): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = NAV_EXIT_FADE_DURATION_MS,
            delayMillis = NAV_EXIT_FADE_DELAY_MS,
        ),
    )
}

private fun activityForegroundFadeOut(): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = NAV_EXIT_FADE_DURATION_MS,
            delayMillis = NAV_FOREGROUND_EXIT_FADE_DELAY_MS,
        ),
    )
}
