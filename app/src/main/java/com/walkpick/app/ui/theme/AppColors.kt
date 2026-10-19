package com.walkpick.app.ui.theme

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
import com.walkpick.app.data.walk.Season

@Immutable
data class AppColors(
    val textPrimary: Color,
    val textSecondary: Color,
    /** An unselected tab or chip. */
    val textInactive: Color,
    val accent: Color,
    /** The dark end of the gradient: the selected tab. */
    val accentDeep: Color,
    /** The light end of the gradient: the latest point of a chart. */
    val accentLight: Color,
    val onAccent: Color,
    /** The ground of a round button standing on the gradient. */
    val onAccentOverlay: Color,
    /** The header and the main buttons. */
    val accentGradient: Brush,
    /** Stop walk: white on its dark part keeps a contrast of 3 (author, 2026-10-11). */
    val stopGradient: Brush,
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
    /** The camera before its picture comes, and what is laid over the picture: buttons, hint, bar. */
    val photoGround: Color,
    val photoButton: Color,
    val photoHint: Color,
    val photoBar: Color,
    /** The ground of a button or a caption over a photo shown on the dark ground. */
    val photoChrome: Color,
    /** A card of glass over a photo and the light line around it. */
    val glass: Color,
    val glassEdge: Color,
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
private const val ACCENT_STOP = 0.6f

private class DiagonalGradient(private val colors: List<Color>) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val squares = size.width * size.width + size.height * size.height
        val reach = size.width * size.height / (DIAGONAL_SCALE * squares.coerceAtLeast(1f))
        return LinearGradientShader(Offset.Zero, Offset(size.height * reach, size.width * reach), colors)
    }
}

internal fun appColors(season: Season): AppColors = when (season) {
    Season.SPRING -> appColors(SpringDeep, Spring, SpringLight)
    Season.SUMMER -> appColors(SummerDeep, Summer, SummerLight)
    Season.AUTUMN -> appColors(AutumnDeep, Autumn, AutumnLight)
    Season.WINTER -> appColors(WinterDeep, Winter, WinterLight)
}

private fun appColors(deep: Color, accent: Color, light: Color) = AppColors(
    textPrimary = Ink,
    textSecondary = Grey,
    textInactive = Tan,
    accent = accent,
    accentDeep = deep,
    accentLight = light,
    onAccent = White,
    onAccentOverlay = White.copy(alpha = 0.2f),
    accentGradient = Brush.linearGradient(
        0f to deep,
        ACCENT_STOP to accent,
        1f to light,
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f),
    ),
    accentDiagonal = DiagonalGradient(listOf(deep, light)),
    stopGradient = Brush.horizontalGradient(ACCENT_STOP to Flame, 1f to Orange),
    canvas = Canvas,
    mapGround = Fog,
    surface = White,
    surfaceTint = Cream,
    switchOff = Mist,
    barPast = Sand,
    divider = Line,
    unvisited = Sand,
    unvisitedRing = Linen,
    scrim = Ink.copy(alpha = 0.48f),
    photoGround = Night,
    photoButton = Black.copy(alpha = 0.32f),
    photoHint = Black.copy(alpha = 0.4f),
    photoBar = Black.copy(alpha = 0.48f),
    photoChrome = White.copy(alpha = 0.12f),
    glass = White.copy(alpha = 0.16f),
    glassEdge = White.copy(alpha = 0.55f),
    streak = Orange,
    streakTint = Peach,
    positive = Green,
    positiveTint = Mint,
    moods = listOf(Red, Orange, Amber, Lime, Green),
    danger = Red,
    dangerTint = Blush,
)

internal val LocalSeason = staticCompositionLocalOf { Season.AUTUMN }

internal val LocalAppColors = staticCompositionLocalOf { appColors(Season.AUTUMN) }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val season: Season
        @Composable
        @ReadOnlyComposable
        get() = LocalSeason.current
}
