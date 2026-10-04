package com.walkspot.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// Only for Material's own components; screens take colours from AppTheme.colors.
private val AppColorScheme = with(walkSpotColors) {
    lightColorScheme(
        primary = accent,
        onPrimary = onAccent,
        background = canvas,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        outlineVariant = divider,
        error = danger,
        onError = onAccent,
    )
}

@Composable
fun WalkSpotTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides walkSpotColors) {
        MaterialTheme(
            colorScheme = AppColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
