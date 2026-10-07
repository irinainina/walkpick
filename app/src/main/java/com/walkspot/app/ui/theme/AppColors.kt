package com.walkspot.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush

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
    /** The ground of a round button standing on the gradient. */
    val onAccentOverlay: Color,
    /** The header and the main buttons. */
    val accentGradient: Brush,
    /** The same gradient turned 45 degrees: the steps ring. */
    val accentDiagonal: Brush,
    val canvas: Color,
    /** The land of the map, for the pictures of routes drawn without one. */
    val mapGround: Color,
    val surface: Color,
    /** A chart track and anything inactive. */
    val surfaceTint: Color,
    /** The track of a switch that is off. */
    val switchOff: Color,
    /** A chart bar of a day that is over and did not reach the goal. */
    val barPast: Color,
    val divider: Color,
    /** A place of the map that was not visited yet: its icon and the ring around it. */
    val unvisited: Color,
    val unvisitedRing: Color,
    /** The dimmed screen under a sheet. */
    val scrim: Color,
    val streak: Color,
    /** The badge behind a streak or notice icon. */
    val streakTint: Color,
    val positive: Color,
    /** The badge behind the icon of a good remark. */
    val positiveTint: Color,
    /** The five moods, awful first. */
    val moods: List<Color>,
    val danger: Color,
    /** The badge behind the icon of something that cannot be undone. */
    val dangerTint: Color,
)

// Figma turns the gradient inside the box of the node: the colour is 0.7 * (x / width + y / height).
private const val DIAGONAL_SCALE = 0.7f

private class DiagonalGradient(private val colors: List<Color>) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val squares = size.width * size.width + size.height * size.height
        val reach = size.width * size.height / (DIAGONAL_SCALE * squares.coerceAtLeast(1f))
        return LinearGradientShader(Offset.Zero, Offset(size.height * reach, size.width * reach), colors)
    }
}

internal val walkSpotColors = AppColors(
    textPrimary = Ink,
    textSecondary = Grey,
    textInactive = SteelBlue,
    accent = Blue,
    accentLight = Cyan,
    onAccent = White,
    onAccentOverlay = White.copy(alpha = 0.2f),
    accentGradient = Brush.horizontalGradient(listOf(Blue, Cyan)),
    accentDiagonal = DiagonalGradient(listOf(Blue, Cyan)),
    canvas = Canvas,
    mapGround = Fog,
    surface = White,
    surfaceTint = PaleBlue,
    switchOff = Mist,
    barPast = LightBlue,
    divider = Line,
    unvisited = LightBlue,
    unvisitedRing = Sky,
    scrim = Ink.copy(alpha = 0.48f),
    streak = Orange,
    streakTint = Peach,
    positive = Green,
    positiveTint = Mint,
    moods = listOf(Red, Orange, Amber, Lime, Green),
    danger = Red,
    dangerTint = Blush,
)

internal val LocalAppColors = staticCompositionLocalOf { walkSpotColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
