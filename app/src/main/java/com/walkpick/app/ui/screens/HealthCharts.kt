package com.walkpick.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.GradientBarChart
import com.walkpick.app.ui.components.LineChart
import com.walkpick.app.ui.components.Mood
import com.walkpick.app.ui.glucoseNumber
import com.walkpick.app.ui.health.HealthChart
import com.walkpick.app.ui.health.HealthState
import com.walkpick.app.ui.health.moodLevel
import com.walkpick.app.ui.oneDecimal
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.PillShape
import com.walkpick.app.ui.theme.Spacing
import kotlin.math.roundToInt

private val ChartHeight = 140.dp
private val LabelHeight = 16.dp
private val LineSwatch = 16.dp
private val BarSwatch = 12.dp
private val BarSwatchShape = RoundedCornerShape(3.dp)
private val TargetWidth = 2.dp
private val FirstLegendWidth = 112.dp

// A target label stands 4 above its line; the upper one of two stands 2 below, clear of the lower label.
private val LabelAbove = 20.dp
private val LabelBelow = 2.dp

/** The values of the tab over the average steps of the same days or months. */
@Composable
internal fun ChartCard(state: HealthState, chart: HealthChart, modifier: Modifier = Modifier) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    AppCard(modifier) {
        Column(Modifier.padding(horizontal = Spacing.ml, vertical = Spacing.m)) {
            val title = stringResource(state.tab.chartTitleRes())
            Text(title, style = type.titleSmall, color = colors.textPrimary)
            Spacer(Modifier.height(Spacing.m))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(ChartHeight),
            ) {
                if (state.tab == HealthKind.MOOD) {
                    val ways = chart.moods.map { way ->
                        way?.let { colors.moods[it.before - 1] to colors.moods[it.after - 1] }
                    }
                    val levels = colors.moods.indices.map { moodLevel(it + 1) }
                    GradientBarChart(chart.bars, ways, levels, Modifier.fillMaxSize())
                } else {
                    val goals = chart.goals.map { it.at }
                    LineChart(chart.lines, chart.bars, chart.columns, Modifier.fillMaxSize(), goals)
                }
                chart.goals.forEachIndexed { index, goal ->
                    val line = ChartHeight * (1 - goal.at)
                    val top = if (chart.goals.size > 1 && index == 0) line + LabelBelow else line - LabelAbove
                    Text(
                        text = stringResource(R.string.health_target_value, targetText(goal.value, state)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = top.coerceIn(0.dp, ChartHeight - LabelHeight)),
                        style = type.labelSmall,
                        color = colors.streak,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.s))
            ColumnLabels(chart)
            Spacer(Modifier.height(Spacing.s))
            Legend(state, targets = chart.goals.isNotEmpty())
        }
    }
}

private fun HealthKind.chartTitleRes() = when (this) {
    HealthKind.MOOD -> R.string.health_mood_walking
    HealthKind.WEIGHT -> R.string.health_weight_walking
    HealthKind.PRESSURE -> R.string.health_pressure_walking
    HealthKind.GLUCOSE -> R.string.health_glucose_walking
}

private fun targetText(value: Double, state: HealthState) = when (state.tab) {
    HealthKind.WEIGHT -> oneDecimal(value * state.units.weight.perMetric)
    HealthKind.GLUCOSE -> glucoseNumber(value, state.units.glucose)
    else -> value.roundToInt().toString()
}

@Composable
private fun ColumnLabels(chart: HealthChart) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth()) {
        chart.labels.forEachIndexed { index, label ->
            val current = index == chart.currentLabel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(LabelHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    // A year is wider than the column of its first month.
                    modifier = Modifier.wrapContentWidth(unbounded = true),
                    style = if (current) type.labelSmall else type.bodySmall,
                    color = if (current) colors.accent else colors.textSecondary,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun Legend(state: HealthState, targets: Boolean) {
    val colors = AppTheme.colors
    val line = Modifier.size(LineSwatch, Spacing.xs)
    val pressure = state.tab == HealthKind.PRESSURE
    Row(
        modifier = Modifier.fillMaxWidth(),
        // Four items share the width; of three the steps stand at a fixed place and the target at the end.
        horizontalArrangement = if (pressure) Arrangement.SpaceBetween else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val name = when (state.tab) {
            HealthKind.WEIGHT -> stringResource(R.string.health_legend_weight, state.units.weight.symbol)
            HealthKind.GLUCOSE -> stringResource(R.string.health_glucose)
            HealthKind.PRESSURE -> stringResource(R.string.profile_systolic)
            HealthKind.MOOD -> stringResource(R.string.health_mood_way)
        }
        val mood = state.tab == HealthKind.MOOD
        val first = if (pressure || mood) Modifier else Modifier.width(FirstLegendWidth)
        LegendItem(name, first) {
            if (state.tab == HealthKind.MOOD) {
                val (top, foot) = colors.moods[Mood.Great.ordinal] to colors.moods[Mood.Neutral.ordinal]
                val way = Brush.verticalGradient(listOf(top, foot))
                Box(Modifier.size(BarSwatch).background(way, BarSwatchShape))
            } else {
                Box(line.background(colors.accentGradient, PillShape))
            }
        }
        if (pressure) {
            LegendItem(stringResource(R.string.profile_diastolic)) {
                Box(line.background(colors.positive, PillShape))
            }
        }
        val bars = when {
            pressure -> R.string.health_steps
            state.tab == HealthKind.MOOD -> R.string.health_no_mood
            else -> R.string.health_avg_steps
        }
        if (state.tab == HealthKind.MOOD) Spacer(Modifier.width(Spacing.ml))
        LegendItem(stringResource(bars)) {
            Box(Modifier.size(BarSwatch).background(colors.surfaceTint, BarSwatchShape))
        }
        if (targets) {
            if (!pressure) Spacer(Modifier.weight(1f))
            LegendItem(stringResource(R.string.health_target)) {
                Canvas(Modifier.size(LineSwatch, TargetWidth)) {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(Spacing.xs.toPx(), TargetWidth.toPx()))
                    val end = Offset(size.width, size.height / 2)
                    drawLine(colors.streak, end.copy(x = 0f), end, size.height, pathEffect = dash)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(text: String, modifier: Modifier = Modifier, swatch: @Composable () -> Unit) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        swatch()
        Text(text, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
    }
}
