package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SkipFrameDarkColorScheme = darkColorScheme(
    primary = ElectricAmber,
    onPrimary = Color.Black,
    primaryContainer = ElectricAmberMuted,
    onPrimaryContainer = ElectricAmber,

    secondary = CyberTeal,
    onSecondary = Color.Black,
    secondaryContainer = CyberTealMuted,
    onSecondaryContainer = CyberTeal,

    tertiary = CoralFlame,
    onTertiary = Color.White,
    tertiaryContainer = CoralFlameMuted,
    onTertiaryContainer = CoralFlame,

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline = BorderStroke,
    outlineVariant = BorderStroke.copy(alpha = 0.5f)
)

@Composable
fun SkipFrameTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SkipFrameDarkColorScheme,
        typography = Typography,
        content = content
    )
}
