package com.walkspot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walkspot.app.ui.theme.AppColors
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.PillShape
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing

enum class BarKind { Past, Today, GoalReached }

/** [fraction] is 0..1 of the track; 0 leaves the track empty. */
data class Bar(
    val fraction: Float,
    val label: String,
    val kind: BarKind = BarKind.Past,
)

private val LabelWidth = 32.dp

/** Bars on tracks with a label under each; [goal] draws the dashed goal line at that fraction. */
@Composable
fun BarChart(
    bars: List<Bar>,
    barWidth: Dp,
    trackHeight: Dp,
    modifier: Modifier = Modifier,
    labelGap: Dp = Spacing.xs,
    goal: Float? = null,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                if (goal != null) {
                    val y = trackHeight.toPx() * (1 - goal)
                    val dash = Spacing.xs.toPx()
                    drawLine(
                        color = colors.streak,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = Sizes.hairline.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                    )
                }
            },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        bars.forEach { bar ->
            val today = bar.kind == BarKind.Today
            Column(
                modifier = Modifier.width(LabelWidth),
                verticalArrangement = Arrangement.spacedBy(labelGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height(trackHeight)
                        .background(colors.surfaceTint, PillShape),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val fill = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(bar.fraction.coerceIn(0f, 1f))
                    when (bar.kind) {
                        BarKind.Past -> Box(fill.background(colors.barPast, PillShape))
                        BarKind.Today -> Box(fill.background(today(colors), PillShape))
                        BarKind.GoalReached -> Box(fill.background(colors.accent, PillShape))
                    }
                }
                Text(
                    text = bar.label,
                    modifier = Modifier.fillMaxWidth(),
                    style = with(MaterialTheme.typography) { if (today) labelSmall else bodySmall },
                    color = if (today) colors.accent else colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun today(colors: AppColors) = Brush.verticalGradient(listOf(colors.accent, colors.accentLight))
