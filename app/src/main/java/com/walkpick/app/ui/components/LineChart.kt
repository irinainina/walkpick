package com.walkpick.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.walkpick.app.ui.theme.AppColors
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Sizes

// A bar takes four sevenths of its column, as in the design: 13.33 of 23.33; among dots 6.33 of 9.33.
private const val BAR_SHARE = 4f / 7
private const val DOT_BAR_SHARE = 0.68f
private val LineWidth = 3.dp
private val GoalWidth = 2.dp
private val GoalDash = 6.dp
private val GoalGap = 4.dp
private val PointRadius = 3.dp
private val LastPointRadius = 5.dp
private val PointStroke = 2.dp
private val DotRadius = 4.dp
private val LineLevels = listOf(0f, 0.5f, 1f)

/**
 * Lines of values over pale bars of steps. Every value is 0..1 of the height; the chart is divided
 * into [slots] columns, a line has a value or a gap for each. The first line is the main one, drawn in
 * the gradient; a second one is green.
 */
@Composable
fun LineChart(
    lines: List<List<Float?>>,
    bars: List<Float>,
    slots: Int,
    modifier: Modifier = Modifier,
    goals: List<Float> = emptyList(),
) {
    val colors = AppTheme.colors
    Canvas(modifier) {
        val slot = size.width / slots
        drawGround(LineLevels, bars, slot, BAR_SHARE, colors)
        goals.forEach { goal ->
            val y = size.height * (1 - goal)
            drawLine(
                color = colors.streak,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = GoalWidth.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(GoalDash.toPx(), GoalGap.toPx())),
            )
        }
        lines.forEachIndexed { index, line ->
            val points = line.mapIndexedNotNull { column, value ->
                value?.let { Offset((column + 0.5f) * slot, size.height * (1 - it)) }
            }
            drawSeries(points, colors, main = index == 0)
        }
    }
}

/** A dot of its own colour in every column that has one, on grid lines at [levels], over bars of steps. */
@Composable
fun DotChart(
    dots: List<Pair<Float, Color>?>,
    bars: List<Float>,
    levels: List<Float>,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Canvas(modifier) {
        val slot = size.width / dots.size.coerceAtLeast(1)
        drawGround(levels, bars, slot, DOT_BAR_SHARE, colors)
        dots.forEachIndexed { column, dot ->
            if (dot != null) {
                val centre = Offset((column + 0.5f) * slot, size.height * (1 - dot.first))
                drawCircle(dot.second, DotRadius.toPx(), centre)
                drawCircle(colors.surface, DotRadius.toPx(), centre, style = Stroke(PointStroke.toPx()))
            }
        }
    }
}

private fun DrawScope.drawGround(
    levels: List<Float>,
    bars: List<Float>,
    slot: Float,
    share: Float,
    colors: AppColors,
) {
    levels.forEach { level ->
        val y = size.height * (1 - level)
        drawLine(colors.divider, Offset(0f, y), Offset(size.width, y), Sizes.hairline.toPx())
    }
    val barWidth = slot * share
    bars.forEachIndexed { index, value ->
        drawRoundRect(
            color = colors.surfaceTint,
            topLeft = Offset(index * slot + (slot - barWidth) / 2, size.height * (1 - value)),
            size = Size(barWidth, size.height * value),
            cornerRadius = CornerRadius(barWidth / 2),
        )
    }
}

private fun DrawScope.drawSeries(points: List<Offset>, colors: AppColors, main: Boolean) {
    if (points.isEmpty()) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    val brush = if (main) {
        Brush.horizontalGradient(listOf(colors.accent, colors.accentLight), points.first().x, points.last().x)
    } else {
        SolidColor(colors.positive)
    }
    drawPath(path, brush, style = Stroke(LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    // The latest point of the main line stands out; the second line keeps all its points alike.
    points.dropLast(if (main) 1 else 0).forEach { point ->
        drawCircle(colors.surface, PointRadius.toPx(), point)
        val ring = if (main) colors.accent else colors.positive
        drawCircle(ring, PointRadius.toPx(), point, style = Stroke(PointStroke.toPx()))
    }
    if (main) {
        drawCircle(colors.accentLight, LastPointRadius.toPx(), points.last())
        drawCircle(colors.surface, LastPointRadius.toPx(), points.last(), style = Stroke(PointStroke.toPx()))
    }
}
