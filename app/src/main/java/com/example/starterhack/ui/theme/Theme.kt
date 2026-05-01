package com.example.starterhack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RedactoColorScheme = lightColorScheme(
    primary = TealAccent,
    onPrimary = CardWhite,
    primaryContainer = TealWash,
    onPrimaryContainer = NavyPrimary,
    secondary = NavyPrimary,
    onSecondary = CardWhite,
    background = BackgroundWarm,
    onBackground = NavyPrimary,
    surface = CardWhite,
    onSurface = NavyPrimary,
    surfaceVariant = BackgroundWarm,
    onSurfaceVariant = Color(0xFF666666),
    outline = Hairline,
)

@Composable
fun RedactoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RedactoColorScheme,
        typography = RedactoTypography,
        content = content
    )
}

// Legacy aliases so existing code still compiles
@Composable
fun ShieldTextTheme(content: @Composable () -> Unit) = RedactoTheme(content)

@Composable
fun StarterHackTheme(content: @Composable () -> Unit) = RedactoTheme(content)
