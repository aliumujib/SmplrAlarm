package de.coldtea.smplr.alarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Simple Material3 color scheme tuned for readability in both light and dark
// modes. This is intentionally opinionated but neutral; apps embedding the
// library sample can fork/replace this as needed.

private val LightColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),

    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,

    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF381E72),
    onPrimaryContainer = Color(0xFFEADDFF),

    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,

    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),

    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E1E5),
)

private val AppTypography = Typography()

@Composable
fun SmplrAlarmTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
