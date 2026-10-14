package com.walkpick.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.ui.components.SegmentedControl
import com.walkpick.app.ui.counted
import com.walkpick.app.ui.distanceText
import com.walkpick.app.ui.grouped
import com.walkpick.app.ui.stats.StatsState
import com.walkpick.app.ui.stats.StatsTab
import com.walkpick.app.ui.theme.AppShadows
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TileHeight = 72.dp
private const val HALF_TURN = 180f
private const val NOTHING = "–"
private const val SECONDS_IN_MINUTE = 60
private const val MINUTES_IN_HOUR = 60

private val MonthDay = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
private val MonthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val Weekday = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val MonthName = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

class PeriodActions(
    val onTab: (StatsTab) -> Unit,
    val onMove: (Long) -> Unit,
    val onPickBar: (Int) -> Unit,
    val onPickDay: (LocalDate) -> Unit,
    val onWalk: (String) -> Unit,
    val onCloseDay: () -> Unit,
    val photo: suspend (FindEntity) -> File? = { null },
)

/** The walks by Week, Month, Year and All: the period, its chart or calendar, and four totals. */
@Composable
fun WalkPeriods(state: StatsState, actions: PeriodActions) {
    SegmentedControl(
        options = StatsTab.entries.map { stringResource(it.titleRes()) },
        selectedIndex = state.tab.ordinal,
        onSelect = { actions.onTab(StatsTab.entries[it]) },
        modifier = Modifier.padding(horizontal = Spacing.ml),
    )
    Spacer(Modifier.height(Spacing.m))
    Period(state, actions.onMove)
    Spacer(Modifier.height(Spacing.m))
    StatsChartCard(state, actions.onPickBar, actions.onPickDay, Modifier.padding(horizontal = Spacing.ml))
    Spacer(Modifier.height(Spacing.ml))
    Column(
        modifier = Modifier.padding(horizontal = Spacing.ml),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        tiles(state).chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                pair.forEach { (value, label) -> Tile(value, label) }
            }
        }
    }
    if (state.day != null && state.dayWalks.isNotEmpty()) {
        DayWalksSheet(state, actions)
    }
}

internal fun StatsTab.titleRes() = when (this) {
    StatsTab.Week -> R.string.stats_week
    StatsTab.Month -> R.string.stats_month
    StatsTab.Year -> R.string.stats_year
    StatsTab.All -> R.string.stats_all
}

@Composable
private fun Period(state: StatsState, onMove: (Long) -> Unit) {
    val range = state.range
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ml),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when (state.tab) {
                StatsTab.Week -> "${range.from.format(MonthDay)} – ${range.to.format(MonthDay)}"
                StatsTab.Month -> range.from.format(MonthYear)
                StatsTab.Year -> range.from.year.toString()
                StatsTab.All -> stringResource(R.string.stats_since, range.from.format(MonthYear))
            },
            style = MaterialTheme.typography.titleSmall,
            color = AppTheme.colors.textPrimary,
        )
        if (state.tab != StatsTab.All) {
            val earlier = Modifier.align(Alignment.CenterStart)
            val later = Modifier
                .align(Alignment.CenterEnd)
                .rotate(HALF_TURN)
            Arrow(R.string.stats_previous, state.canGoBack, earlier) { onMove(-1) }
            Arrow(R.string.stats_next, state.canGoForward, later) { onMove(1) }
        }
    }
}

@Composable
private fun Arrow(descriptionRes: Int, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Icon(
        painter = painterResource(R.drawable.ic_back),
        contentDescription = stringResource(descriptionRes),
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        tint = with(AppTheme.colors) { if (enabled) textPrimary else divider },
    )
}

@Composable
private fun RowScope.Tile(value: String, label: String) {
    val shape = MaterialTheme.shapes.large
    Column(
        Modifier
            .weight(1f)
            .height(TileHeight)
            .dropShadow(shape, AppShadows.card)
            .background(AppTheme.colors.surface, shape)
            .padding(start = Spacing.m, top = Spacing.sm),
    ) {
        val colors = AppTheme.colors
        Text(value, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1)
    }
}

// Four per tab, in the order of the design: left to right, top to bottom.
@Composable
private fun tiles(state: StatsState): List<Pair<String, String>> {
    val totals = state.totals
    val long = state.tab == StatsTab.Year || state.tab == StatsTab.All
    val distance = distanceText(totals.distanceM, state.distanceUnit) to
        stringResource(R.string.today_distance)
    val time = durationText(totals.durationS, hoursOnly = long) to stringResource(R.string.stats_time)
    val steps = grouped(totals.steps) to stringResource(R.string.stats_steps)
    val best = totals.bestDay?.takeIf { totals.bestDaySteps > 0 }
    val bestDay = (best?.let { grouped(totals.bestDaySteps) } ?: NOTHING) to when {
        best == null -> stringResource(R.string.stats_best_day)
        state.tab == StatsTab.Week -> stringResource(R.string.stats_best_day_on, best.format(Weekday))
        else -> stringResource(R.string.stats_best_day_on, best.format(MonthDay))
    }
    return when (state.tab) {
        StatsTab.Week, StatsTab.Month -> listOf(distance, time, steps, bestDay)
        StatsTab.Year -> listOf(
            distance,
            time,
            steps,
            (state.bestMonth?.format(MonthName) ?: NOTHING) to stringResource(R.string.stats_best_month),
        )
        StatsTab.All -> listOf(
            distance,
            time,
            counted(state.longestStreak, R.string.stats_day_one, R.string.stats_day_many) to
                stringResource(R.string.stats_longest_streak),
            bestDay,
        )
    }
}

// "48 min", "2 h 25 min"; a year or all the time in whole hours: "96 h".
@Composable
internal fun durationText(seconds: Int, hoursOnly: Boolean): String {
    val minutes = (seconds + SECONDS_IN_MINUTE / 2) / SECONDS_IN_MINUTE
    val hours = minutes / MINUTES_IN_HOUR
    return when {
        hoursOnly && hours > 0 -> {
            val rounded = (minutes + MINUTES_IN_HOUR / 2) / MINUTES_IN_HOUR
            stringResource(R.string.stats_hours, rounded)
        }
        hours > 0 -> stringResource(R.string.stats_hours_minutes, hours, minutes % MINUTES_IN_HOUR)
        else -> stringResource(R.string.walk_minutes, minutes)
    }
}
