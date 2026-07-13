package com.lyn.mazhu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = MazhuRed,
    onPrimary = Color.White,
    primaryContainer = MazhuPinkContainer,
    onPrimaryContainer = MazhuRedDark,
    secondary = MazhuSage,
    onSecondary = Color.White,
    secondaryContainer = MazhuSageContainer,
    onSecondaryContainer = Color(0xFF0F2A20),
    tertiary = MazhuAmber,
    onTertiary = Color.White,
    tertiaryContainer = MazhuAmberContainer,
    onTertiaryContainer = Color(0xFF251A00),
    background = MazhuBackground,
    onBackground = MazhuInk,
    surface = MazhuSurface,
    onSurface = MazhuInk,
    surfaceContainerLow = MazhuSurfaceLow,
    surfaceContainer = MazhuSurface,
    surfaceContainerHigh = MazhuSurfaceHigh,
    surfaceContainerHighest = Color(0xFFECE0E2),
    outline = Color(0xFF857376),
    outlineVariant = Color(0xFFD7C2C6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB2BF),
    primaryContainer = MazhuRedDark,
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFFBBD3C7),
    secondaryContainer = Color(0xFF344E43),
    tertiary = Color(0xFFE9C450),
    tertiaryContainer = Color(0xFF5A4400),
    background = MazhuDarkBackground,
    surface = Color(0xFF1D191B),
    surfaceContainerLow = Color(0xFF211D1F),
    surfaceContainer = Color(0xFF272123),
    surfaceContainerHigh = Color(0xFF332B2E),
    surfaceContainerHighest = Color(0xFF40373A),
)

@Composable
fun MazhuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
