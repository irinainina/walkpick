package com.walkspot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.walkspot.app.ui.theme.AppColors
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.PillShape
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing

enum class BarKind { Past, Today, GoalReached }

/**
 * [fraction] is 0..1 of the track; 0 leaves the track empty. [tip] is shown when the bar is picked.
 * [accentLabel] writes the label bold and blue, the way today is marked.
 */
data class Bar(
    val fraction: Float,
    val label: String,
    val kind: BarKind = BarKind.Past,
    val tip: String? = null,
    val accentLabel: Boolean = kind == BarKind.Today,
)

private val TipSize = DpSize(72.dp, 28.dp)

/**
 * Bars on tracks with a label under each; [goal] draws the dashed goal line at that fraction.
 * Each bar stands in a slot [slotWidth] wide, the slots are spread evenly.
 */
@Composable
fun BarChart(
    bars: List<Bar>,
    barWidth: Dp,
    trackHeight: Dp,
    modifier: Modifier = Modifier,
    slotWidth: Dp = 32.dp,
    labelGap: Dp = Spacing.xs,
    goal: Float? = null,
    selected: Int? = null,
    onSelect: ((Int) -> Unit)? = null,
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
        bars.forEachIndexed { index, bar ->
            val fraction = bar.fraction.coerceIn(0f, 1f)
            val pick = if (onSelect == null) {
                Modifier
            } else {
                // No ripple: the tip that appears is the answer to the tap.
                Modifier.clickable(remember { MutableInteractionSource() }, null) { onSelect(index) }
            }
            Column(
                modifier = Modifier
                    .width(slotWidth)
                    .zIndex(if (index == selected) 1f else 0f)
                    .then(pick),
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
                        .fillMaxHeight(fraction)
                    when (bar.kind) {
                        BarKind.Past -> Box(fill.background(colors.barPast, PillShape))
                        BarKind.Today -> Box(fill.background(today(colors), PillShape))
                        BarKind.GoalReached -> Box(fill.background(colors.accent, PillShape))
                    }
                    if (index == selected && bar.tip != null) {
                        Tip(bar.tip, Modifier.padding(bottom = trackHeight * fraction + Spacing.s))
                    }
                }
                Text(
                    text = bar.label,
                    modifier = Modifier.wrapContentWidth(unbounded = true),
                    style = with(MaterialTheme.typography) { if (bar.accentLabel) labelSmall else bodySmall },
                    color = if (bar.accentLabel) colors.accent else colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

// Wider than its bar, so it is laid out past the edges of the slot.
@Composable
private fun Tip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .wrapContentSize(unbounded = true)
            .size(TipSize)
            .background(AppTheme.colors.textPrimary, MaterialTheme.shapes.extraSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.onAccent)
    }
}

private fun today(colors: AppColors) = Brush.verticalGradient(listOf(colors.accent, colors.accentLight))
