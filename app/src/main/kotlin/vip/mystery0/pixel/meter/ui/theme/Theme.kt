package vip.mystery0.pixel.meter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import vip.mystery0.pixel.meter.data.model.AppThemeMode

private val FixedLightColorScheme = lightColorScheme(
    primary = Color(0xFF006A66),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF72F7EF),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF4A6361),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E5),
    onSecondaryContainer = Color(0xFF051F1E),
    tertiary = Color(0xFF46617A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCDE5FF),
    onTertiaryContainer = Color(0xFF001D33),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFDFC),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFAFDFC),
    onSurface = Color(0xFF191C1C),
    surfaceContainerLow = Color(0xFFF0F5F4),
    surfaceVariant = Color(0xFFDAE5E3),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7978),
    inverseSurface = Color(0xFF2D3131),
    inverseOnSurface = Color(0xFFEFF1F0),
    inversePrimary = Color(0xFF50DBD3),
)

private val FixedDarkColorScheme = darkColorScheme(
    primary = Color(0xFF50DBD3),
    onPrimary = Color(0xFF003735),
    primaryContainer = Color(0xFF00504D),
    onPrimaryContainer = Color(0xFF72F7EF),
    secondary = Color(0xFFB0CCCA),
    onSecondary = Color(0xFF1B3533),
    secondaryContainer = Color(0xFF324B49),
    onSecondaryContainer = Color(0xFFCCE8E5),
    tertiary = Color(0xFFAECAE6),
    onTertiary = Color(0xFF16334A),
    tertiaryContainer = Color(0xFF2E4961),
    onTertiaryContainer = Color(0xFFCDE5FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101414),
    onBackground = Color(0xFFE0E3E2),
    surface = Color(0xFF101414),
    onSurface = Color(0xFFE0E3E2),
    surfaceContainerLow = Color(0xFF191E1E),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C7),
    outline = Color(0xFF899391),
    inverseSurface = Color(0xFFE0E3E2),
    inverseOnSurface = Color(0xFF2D3131),
    inversePrimary = Color(0xFF006A66),
)

@Composable
fun PixelPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: Int = AppThemeMode.Dynamic.value,
    themeColor: Int = AppThemeMode.DEFAULT_THEME_COLOR,
    useAmoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val fixedThemeColor = Color(themeColor).copy(alpha = 1f)
    val colorScheme = when (AppThemeMode.fromValue(themeMode)) {
        AppThemeMode.Dynamic ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        AppThemeMode.Fixed ->
            fixedColorScheme(
                darkTheme = darkTheme,
                themeColor = fixedThemeColor,
                useAmoledBlack = useAmoledBlack
            )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private fun fixedColorScheme(
    darkTheme: Boolean,
    themeColor: Color,
    useAmoledBlack: Boolean,
): ColorScheme {
    val baseColorScheme = if (darkTheme) FixedDarkColorScheme else FixedLightColorScheme
    val primary = if (darkTheme) {
        themeColor.blendWith(Color.White, 0.32f)
    } else {
        themeColor
    }
    val primaryContainer = if (darkTheme) {
        themeColor.blendWith(Color.Black, 0.55f)
    } else {
        themeColor.blendWith(Color.White, 0.78f)
    }
    val secondary = if (darkTheme) {
        themeColor.blendWith(Color.White, 0.62f)
    } else {
        themeColor.blendWith(Color(0xFF4A6361), 0.55f)
    }
    val tertiary = if (darkTheme) {
        themeColor.blendWith(Color(0xFFAECAE6), 0.55f)
    } else {
        themeColor.blendWith(Color(0xFF46617A), 0.55f)
    }

    val colorScheme = baseColorScheme.copy(
        primary = primary,
        onPrimary = primary.contentColor(),
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.contentColor(),
        secondary = secondary,
        onSecondary = secondary.contentColor(),
        tertiary = tertiary,
        onTertiary = tertiary.contentColor(),
        inversePrimary = if (darkTheme) themeColor else primary.blendWith(Color.White, 0.32f),
    )

    return if (darkTheme && useAmoledBlack) colorScheme.toAmoled() else colorScheme
}

private fun ColorScheme.toAmoled(): ColorScheme =
    copy(
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceContainerLow = Color(0xFF121212),
        surfaceVariant = Color.Black,
        onSurfaceVariant = Color.White,
        inverseSurface = Color.White,
        inverseOnSurface = Color.Black,
    )

private fun Color.blendWith(target: Color, fraction: Float): Color =
    Color(
        red = red + (target.red - red) * fraction,
        green = green + (target.green - green) * fraction,
        blue = blue + (target.blue - blue) * fraction,
        alpha = 1f
    )

private fun Color.contentColor(): Color =
    if (luminance() > 0.5f) Color.Black else Color.White
