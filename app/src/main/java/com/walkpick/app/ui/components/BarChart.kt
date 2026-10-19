package com.walkpick.app.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walkpick.app.ui.theme.AppColors
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.PillShape
import com.walkpick.app.ui.theme.Spacing

/** A past bar, a day of the week that had a walk, and the bar of today. */
enum class BarKind { Past, Walked, Today }

/**
 * [fraction] is 0..1 of the track; 0 leaves the track empty.
 * [accentLabel] writes the label bold and blue, the way today is marked.
 */
data class Bar(
    val fraction: Float,
    val label: String,
    val kind: BarKind = BarKind.Past,
    val accentLabel: Boolean = kind == BarKind.Today,
)

/**
 * Bars on tracks with a label under each. Each bar stands in a slot [slotWidth] wide, the slots are
 * spread evenly.
 */
@Composable
fun BarChart(
    bars: List<Bar>,
    barWidth: Dp,
    trackHeight: Dp,
    modifier: Modifier = Modifier,
    slotWidth: Dp = 32.dp,
    labelGap: Dp = Spacing.xs,
    onSelect: ((Int) -> Unit)? = null,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        bars.forEachIndexed { index, bar ->
            val fraction = bar.fraction.coerceIn(0f, 1f)
            val pick = if (onSelect == null) {
                Modifier
            } else {
                // No ripple: the screen that opens is the answer to the tap.
                Modifier.clickable(remember { MutableInteractionSource() }, null) { onSelect(index) }
            }
            Column(
                modifier = Modifier
                    .width(slotWidth)
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
                        BarKind.Walked, BarKind.Today -> Box(fill.background(walked(colors), PillShape))
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

private fun walked(colors: AppColors) = Brush.verticalGradient(listOf(colors.accentDeep, colors.accentLight))
