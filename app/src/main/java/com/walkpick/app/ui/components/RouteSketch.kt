package com.walkpick.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walkpick.app.ui.theme.AppTheme

/** How far the line keeps from the sides of its tile. */
val SketchEdge = 20.dp

// The flag is drawn inside its ring; all of it grows with the ring.
private const val RING_LINE = 1 / 8f
private const val FLAG_HALF_WIDTH = 3 / 16f
private const val FLAG_HALF_HEIGHT = 1 / 4f
private const val FLAG_NOTCH = 1 / 16f
private const val FLAG_LINE = 3 / 32f

/**
 * The line of a walk alone, on a tile of the colour of the map: a real map in every card would be too
 * heavy. [sketch] holds the points as shares of the tile inside its edge, see routeSketch; the size
 * comes with [modifier].
 */
@Composable
fun RouteSketch(
    sketch: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier,
    line: Dp = 3.dp,
    start: Dp = 16.dp,
    look: RouteLook = RouteLook(),
) {
    val colors = AppTheme.colors
    val moods = look.ends().takeIf { look.moodBefore != null || look.moodAfter != null }
    Canvas(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(colors.mapGround),
    ) {
        if (sketch.isEmpty()) return@Canvas
        val edge = SketchEdge.toPx()
        val (width, height) = size.width - edge * 2 to size.height - edge * 2
        val points = sketch.map { (x, y) -> Offset(edge + x * width, edge + y * height) }
        val round = Stroke(line.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val gradient = Brush.horizontalGradient(listOf(colors.accent, colors.accentLight), edge, edge + width)
        val dashes = PathEffect.dashPathEffect(floatArrayOf(line.toPx() * 2, line.toPx() * 2))
        val legs = look.legs(points)
        var done = 0
        legs.forEachIndexed { index, leg ->
            if (moods == null) {
                val path = Path().apply {
                    moveTo(leg.first().x, leg.first().y)
                    leg.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, gradient, style = round)
            } else {
                // The moods run along the way walked, not across the tile: each piece has its own shade.
                leg.zipWithNext().forEachIndexed { at, (from, to) ->
                    val shade = lerp(moods.first, moods.second, (done + at) / (points.size - 1f))
                    drawLine(shade, from, to, line.toPx(), StrokeCap.Round)
                }
            }
            // The way not walked during a pause.
            legs.getOrNull(index + 1)?.let { next ->
                val shade = moods?.let { lerp(it.first, it.second, (done + leg.size) / (points.size - 1f)) }
                val thin = line.toPx() / 2
                drawLine(shade ?: colors.accent, leg.last(), next.first(), thin, pathEffect = dashes)
            }
            done += leg.size
        }

        val ring = start.toPx()
        val (x, y) = points.first()
        drawCircle(colors.surface, ring / 2, points.first())
        drawCircle(colors.accent, ring / 2, points.first(), style = Stroke(ring * RING_LINE))
        val (halfWidth, halfHeight) = ring * FLAG_HALF_WIDTH to ring * FLAG_HALF_HEIGHT
        val flag = Path().apply {
            moveTo(x - halfWidth, y + halfHeight)
            lineTo(x - halfWidth, y - halfHeight)
            lineTo(x + halfWidth, y - halfHeight)
            lineTo(x + halfWidth - ring * FLAG_NOTCH, y - halfHeight / 2)
            lineTo(x + halfWidth, y)
            lineTo(x - halfWidth, y)
        }
        val thin = Stroke(ring * FLAG_LINE, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(flag, colors.accent, style = thin)
    }
}
