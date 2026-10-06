package com.walkspot.app.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.Bar
import com.walkspot.app.ui.components.BarChart
import com.walkspot.app.ui.components.BarKind
import com.walkspot.app.ui.components.LineChart
import com.walkspot.app.ui.components.ProgressRing
import com.walkspot.app.ui.components.RowChevron
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing

private val RingSize = 176.dp
private val WeekBarWidth = 10.dp
private val WeekTrackHeight = 48.dp
private val ChartSize = DpSize(280.dp, 140.dp)
private const val CHART_HEIGHT = 140f

private val week = listOf(
    Bar(39 / 48f, "M"),
    Bar(26 / 48f, "T"),
    Bar(31 / 48f, "W", BarKind.Today),
    Bar(0f, "T"),
    Bar(0f, "F"),
    Bar(0f, "S"),
    Bar(0f, "S"),
)
private val steps = listOf(75, 79, 87, 93, 107, 104, 98, 101, 111).map { it / CHART_HEIGHT }
private val weight = listOf(15, 17, 24, 28, 35, 39, 33, 48, 62).map { 1 - it / CHART_HEIGHT }

/** The charts with the data of the Figma frames, for the comparison with the design. */
@Composable
fun ChartGallery() {
    val ink = AppTheme.colors.textPrimary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .statusBarsPadding()
            .padding(horizontal = Spacing.ml, vertical = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        ProgressRing(progress = 0.64f, modifier = Modifier.size(RingSize)) {
            Column(
                modifier = Modifier.padding(bottom = Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("6,420", style = MaterialTheme.typography.displayMedium, color = ink)
                Text(
                    text = "of 10,000 steps",
                    modifier = Modifier.padding(top = Spacing.xs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
        AppCard {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.ml, top = Spacing.m, end = Spacing.m),
            ) {
                Text("This week", style = MaterialTheme.typography.titleSmall, color = ink)
                Box(Modifier.align(Alignment.TopEnd)) { RowChevron() }
            }
            BarChart(
                bars = week,
                barWidth = WeekBarWidth,
                trackHeight = WeekTrackHeight,
                modifier = Modifier.padding(Spacing.ml, Spacing.s, Spacing.ml, Spacing.sm),
            )
        }
        LineChart(
            lines = listOf(weight),
            bars = steps,
            slots = 12,
            modifier = Modifier.size(ChartSize),
            goals = listOf(1 - 111 / CHART_HEIGHT),
        )
    }
}
