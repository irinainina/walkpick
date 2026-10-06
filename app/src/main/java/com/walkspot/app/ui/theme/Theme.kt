package com.walkspot.app.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
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

/** How a tap is answered; Material's own answer is a grey. */
@OptIn(ExperimentalMaterial3Api::class)
object AppRipples {
    // The light end of the gradient; Android draws the wave at about a fifth of the colour.
    val soft = RippleConfiguration(color = Cyan)

    /** On the gradient, where blue would not show. */
    val onAccent = RippleConfiguration(color = walkSpotColors.onAccent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkSpotTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppColors provides walkSpotColors,
        LocalRippleConfiguration provides AppRipples.soft,
    ) {
        MaterialTheme(
            colorScheme = AppColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
