package com.walkpick.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.BarChart
import com.walkpick.app.ui.counted
import com.walkpick.app.ui.stats.CalendarDay
import com.walkpick.app.ui.stats.StatsState
import com.walkpick.app.ui.stats.StatsTab
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val TrackHeight = 120.dp
private val ChartSide = 24.dp
private val WeekBarWidth = 20.dp
private val YearBarWidth = 16.dp
private val AllBarWidth = 12.dp

// More months than fit are scrolled, each taking this much; the chart opens at the latest one.
private val AllBarStep = 17.dp
private const val ALL_BARS_FITTING = 16

// Three dashes of 4 with 2 between, as in the legend of the design.

private val DaySize = 32.dp
private val LegendDot = 12.dp
private val LegendGap = 6.dp
private val LegendSpace = 10.dp

@Composable
internal fun StatsChartCard(
    state: StatsState,
    onPickBar: (Int) -> Unit,
    onPickDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val current = state.today in state.range
    AppCard(modifier) {
        Row(Modifier.padding(start = Spacing.ml, top = Spacing.m, end = Spacing.ml)) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = counted(state.totals.walks, R.string.stats_walks_one, R.string.stats_walks_many),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(periodWord(state.tab, current)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            // The streak runs up to today: the week that holds today shows it.
            if (state.tab == StatsTab.Week && current && state.streak > 0) Streak(state.streak)
        }
        when (state.tab) {
            StatsTab.Month -> Calendar(state.calendar, state.today, onPickDay)
            else -> Bars(state, onPickBar)
        }
    }
}

@Composable
private fun Bars(state: StatsState, onPickBar: (Int) -> Unit) {
    val barWidth = when (state.tab) {
        StatsTab.Week -> WeekBarWidth
        StatsTab.Year -> YearBarWidth
        else -> AllBarWidth
    }
    val scrolled = state.bars.size > ALL_BARS_FITTING
    val width = if (scrolled) Modifier.width(AllBarStep * state.bars.size) else Modifier
    val scroll = if (scrolled) Modifier.horizontalScroll(rememberScrollState(Int.MAX_VALUE)) else Modifier
    Box(
        Modifier
            .padding(top = Spacing.ml, bottom = Spacing.sm)
            .then(scroll)
            .padding(horizontal = ChartSide),
    ) {
        BarChart(
            bars = state.bars,
            barWidth = barWidth,
            trackHeight = TrackHeight,
            modifier = width,
            slotWidth = if (state.tab == StatsTab.Week) DaySize else barWidth,
            labelGap = Spacing.s,
            onSelect = onPickBar,
        )
    }
}

@Composable
private fun Calendar(days: List<CalendarDay>, today: LocalDate, onPickDay: (LocalDate) -> Unit) {
    val colors = AppTheme.colors
    Column(Modifier.padding(start = Spacing.ml, top = Spacing.sm, end = Spacing.ml, bottom = LegendSpace)) {
        Row {
            DayOfWeek.entries.forEach {
                Text(
                    text = it.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        // Empty cells stand before the first day, so that it falls under its weekday.
        val cells = List(days.firstOrNull()?.day?.dayOfWeek?.ordinal ?: 0) { null } + days
        cells.chunked(DayOfWeek.entries.size).forEach { week ->
            Spacer(Modifier.height(Spacing.xs))
            Row {
                week.forEach { day ->
                    Box(Modifier.weight(1f), Alignment.Center) { if (day != null) Day(day, today, onPickDay) }
                }
                repeat(DayOfWeek.entries.size - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(LegendSpace))
        Row(
            horizontalArrangement = Arrangement.spacedBy(LegendGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(LegendDot).background(colors.surfaceTint, CircleShape))
            Text(
                text = stringResource(R.string.stats_walk_day),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun Day(day: CalendarDay, today: LocalDate, onPick: (LocalDate) -> Unit) {
    val colors = AppTheme.colors
    val ground = when {
        day.day == today -> Modifier.background(colors.accentGradient, CircleShape)
        day.walked -> Modifier.background(colors.surfaceTint, CircleShape)
        else -> Modifier
    }
    // A day with a walk leads to it.
    val tap = if (day.walked) Modifier.clip(CircleShape).clickable { onPick(day.day) } else Modifier
    Box(Modifier.size(DaySize).then(ground).then(tap), contentAlignment = Alignment.Center) {
        Text(
            text = day.day.dayOfMonth.toString(),
            style = with(MaterialTheme.typography) { if (ground == Modifier) bodyMedium else labelMedium },
            color = when {
                day.day == today -> colors.onAccent
                day.walked -> colors.accent
                day.day > today -> colors.textSecondary
                else -> colors.textPrimary
            },
        )
    }
}

// Beside the number of walks, a little lower, as in the frame.
@Composable
private fun Streak(days: Int) {
    val color = AppTheme.colors.streak
    Row(
        modifier = Modifier.padding(top = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_flame),
            contentDescription = null,
            modifier = Modifier.size(Sizes.iconSmall),
            tint = color,
        )
        Text(
            text = counted(days, R.string.today_streak_one, R.string.today_streak_many),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

// "this week" while the period holds today, "that week" for an earlier one.
internal fun periodWord(tab: StatsTab, current: Boolean) = when (tab) {
    StatsTab.All -> R.string.stats_in_total
    StatsTab.Week -> if (current) R.string.stats_this_week else R.string.stats_that_week
    StatsTab.Month -> if (current) R.string.stats_this_month else R.string.stats_that_month
    StatsTab.Year -> if (current) R.string.stats_this_year else R.string.stats_that_year
}
