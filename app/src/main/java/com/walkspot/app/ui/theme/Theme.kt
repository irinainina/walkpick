package com.walkspot.app.ui.theme

import androidx.compose.foundation.text.LocalAutofillHighlightBrush
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

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

// How a tap is answered: the blue of the buttons' glow, Android draws the wave at about a fifth of it.
@OptIn(ExperimentalMaterial3Api::class)
private val SoftRipple = RippleConfiguration(color = Olive)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkSpotTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppColors provides walkSpotColors,
        LocalRippleConfiguration provides SoftRipple,
        // Android paints a field yellow after filling it from a suggestion.
        LocalAutofillHighlightBrush provides SolidColor(Color.Transparent),
    ) {
        MaterialTheme(
            colorScheme = AppColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
