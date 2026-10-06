package com.walkspot.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.walkspot.app.ui.theme.AppTheme

private val RingWidth = 12.dp
private const val START_ANGLE = -90f
private const val FULL_TURN = 360f

/** A ring filled clockwise from the top; [progress] is 0..1 and the caller sets the size. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val track = AppTheme.colors.surfaceTint
    val fill = AppTheme.colors.accentDiagonal
    Box(
        modifier = modifier.drawBehind {
            val stroke = Stroke(RingWidth.toPx())
            val inset = Offset(stroke.width / 2, stroke.width / 2)
            val arc = Size(size.width - stroke.width, size.height - stroke.width)
            drawArc(track, START_ANGLE, FULL_TURN, useCenter = false, inset, arc, style = stroke)
            drawArc(
                brush = fill,
                startAngle = START_ANGLE,
                sweepAngle = FULL_TURN * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = inset,
                size = arc,
                style = stroke,
            )
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}
