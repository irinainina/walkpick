package com.walkspot.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walkspot.app.ui.theme.AppTheme

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
) {
    val colors = AppTheme.colors
    Canvas(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(colors.mapGround),
    ) {
        if (sketch.isEmpty()) return@Canvas
        val edge = SketchEdge.toPx()
        val (width, height) = size.width - edge * 2 to size.height - edge * 2
        val points = sketch.map { (x, y) -> Offset(edge + x * width, edge + y * height) }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val round = Stroke(line.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val gradient = Brush.horizontalGradient(listOf(colors.accent, colors.accentLight), edge, edge + width)
        drawPath(path, gradient, style = round)

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
