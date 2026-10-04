package com.walkspot.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val textPrimary: Color,
    val textSecondary: Color,
    /** An unselected tab or chip. */
    val textInactive: Color,
    val accent: Color,
    /** The light end of the gradient: the latest point of a chart. */
    val accentLight: Color,
    val onAccent: Color,
    /** The header and the main buttons. */
    val accentGradient: Brush,
    val canvas: Color,
    val surface: Color,
    /** A chart track and anything inactive. */
    val surfaceTint: Color,
    /** A chart bar of a day that is over and did not reach the goal. */
    val barPast: Color,
    val divider: Color,
    /** The dimmed screen under a sheet. */
    val scrim: Color,
    val streak: Color,
    val positive: Color,
    val danger: Color,
)

internal val walkSpotColors = AppColors(
    textPrimary = Ink,
    textSecondary = Grey,
    textInactive = SteelBlue,
    accent = Blue,
    accentLight = Cyan,
    onAccent = White,
    accentGradient = Brush.horizontalGradient(listOf(Blue, Cyan)),
    canvas = Canvas,
    surface = White,
    surfaceTint = PaleBlue,
    barPast = LightBlue,
    divider = Line,
    scrim = Ink.copy(alpha = 0.48f),
    streak = Orange,
    positive = Green,
    danger = Red,
)

internal val LocalAppColors = staticCompositionLocalOf { walkSpotColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
