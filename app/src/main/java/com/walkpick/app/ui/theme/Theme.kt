package com.walkpick.app.ui.theme

import androidx.compose.foundation.text.LocalAutofillHighlightBrush
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.walkpick.app.data.walk.Season

// Only for Material's own components; screens take colours from AppTheme.colors.
private fun AppColors.scheme() =
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkPickTheme(season: Season, content: @Composable () -> Unit) {
    val colors = remember(season) { appColors(season) }
    CompositionLocalProvider(
        LocalSeason provides season,
        LocalAppColors provides colors,
        // A tap is answered in the accent; Android draws the wave at about a fifth of it.
        LocalRippleConfiguration provides RippleConfiguration(color = colors.accent),
        // Android paints a field yellow after filling it from a suggestion.
        LocalAutofillHighlightBrush provides SolidColor(Color.Transparent),
    ) {
        MaterialTheme(
            colorScheme = colors.scheme(),
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
