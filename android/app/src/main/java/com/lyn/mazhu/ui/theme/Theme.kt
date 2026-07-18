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
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = MazhuAmber,
    onTertiary = Color.White,
    tertiaryContainer = MazhuAmberContainer,
    onTertiaryContainer = Color(0xFF251A00),
    background = MazhuBackground,
    onBackground = MazhuInk,
    surface = MazhuSurface,
    onSurface = MazhuInk,
    surfaceContainerLow = MazhuSurfaceLow,
    surfaceContainer = MazhuSurfaceContainer,
    surfaceContainerHigh = MazhuSurfaceHigh,
    surfaceContainerHighest = MazhuSurfaceHighest,
    outline = Color(0xFF73807A),
    outlineVariant = Color(0xFFD5DDD8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    primaryContainer = MazhuRedDark,
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFCBD5E1),
    secondaryContainer = Color(0xFF334155),
    tertiary = Color(0xFFFACC15),
    tertiaryContainer = Color(0xFF713F12),
    background = MazhuDarkBackground,
    surface = Color(0xFF151F1B),
    surfaceContainerLow = Color(0xFF18231F),
    surfaceContainer = Color(0xFF1F2A25),
    surfaceContainerHigh = Color(0xFF293631),
    surfaceContainerHighest = Color(0xFF34433D),
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
