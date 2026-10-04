package com.walkspot.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.walkspot.app.ui.theme.AppColors
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Sizes

private val BarGap = 10.dp
private val LineWidth = 3.dp
private val GoalWidth = 2.dp
private val GoalDash = 6.dp
private val GoalGap = 4.dp
private val PointRadius = 3.dp
private val LastPointRadius = 5.dp
private val PointStroke = 2.dp
private const val GRID_LINES = 3

/**
 * A line of values over pale bars of steps. Every value is 0..1 of the height; the chart is
 * divided into [slots] columns and the data fills them from the left.
 */
@Composable
fun LineChart(
    line: List<Float>,
    bars: List<Float>,
    slots: Int,
    modifier: Modifier = Modifier,
    goal: Float? = null,
) {
    val colors = AppTheme.colors
    Canvas(modifier) {
        val slot = size.width / slots
        repeat(GRID_LINES) { index ->
            val y = size.height * index / (GRID_LINES - 1)
            drawLine(colors.divider, Offset(0f, y), Offset(size.width, y), Sizes.hairline.toPx())
        }
        val barWidth = slot - BarGap.toPx()
        bars.forEachIndexed { index, value ->
            drawRoundRect(
                color = colors.surfaceTint,
                topLeft = Offset(index * slot + BarGap.toPx() / 2, size.height * (1 - value)),
                size = Size(barWidth, size.height * value),
                cornerRadius = CornerRadius(barWidth / 2),
            )
        }
        if (goal != null) {
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
        val points = line.mapIndexed { index, value ->
            Offset((index + 0.5f) * slot, size.height * (1 - value))
        }
        drawSeries(points, colors)
    }
}

private fun DrawScope.drawSeries(points: List<Offset>, colors: AppColors) {
    if (points.isEmpty()) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path = path,
        brush = Brush.horizontalGradient(
            colors = listOf(colors.accent, colors.accentLight),
            startX = points.first().x,
            endX = points.last().x,
        ),
        style = Stroke(LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    points.dropLast(1).forEach { point ->
        drawCircle(colors.surface, PointRadius.toPx(), point)
        drawCircle(colors.accent, PointRadius.toPx(), point, style = Stroke(PointStroke.toPx()))
    }
    drawCircle(colors.accentLight, LastPointRadius.toPx(), points.last())
    drawCircle(colors.surface, LastPointRadius.toPx(), points.last(), style = Stroke(PointStroke.toPx()))
}
