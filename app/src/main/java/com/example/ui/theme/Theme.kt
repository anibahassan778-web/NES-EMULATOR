package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NesDarkColorScheme = darkColorScheme(
    primary = NesRed,
    onPrimary = Color.White,
    primaryContainer = NesRedDark,
    onPrimaryContainer = Color.White,
    secondary = NesCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color.White,
    tertiary = NesGold,
    background = NesDarkBackground,
    onBackground = NesTextPrimary,
    surface = NesSurface,
    onSurface = NesTextPrimary,
    surfaceVariant = NesSurfaceCard,
    onSurfaceVariant = NesTextSecondary,
    outline = NesBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Use intentional OLED theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NesDarkColorScheme,
        typography = Typography,
        content = content
    )
}
