package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.walkspot.app.R
import com.walkspot.app.ui.components.AppTopBar
import com.walkspot.app.ui.components.SegmentedControl
import com.walkspot.app.ui.counted
import com.walkspot.app.ui.distanceText
import com.walkspot.app.ui.grouped
import com.walkspot.app.ui.stats.StatsState
import com.walkspot.app.ui.stats.StatsTab
import com.walkspot.app.ui.theme.AppShadows
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TileHeight = 72.dp
private const val HALF_TURN = 180f
private const val NOTHING = "–"

private val MonthDay = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
private val MonthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val Weekday = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val MonthName = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

@Composable
fun StatsScreen(
    state: StatsState,
    onBack: () -> Unit,
    onTab: (StatsTab) -> Unit,
    onMove: (Long) -> Unit,
    onPickBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        AppTopBar(stringResource(R.string.stats_title), onBack, Modifier.padding(top = Spacing.xs))
        Spacer(Modifier.height(Spacing.sm))
        SegmentedControl(
            options = StatsTab.entries.map { stringResource(it.titleRes()) },
            selectedIndex = state.tab.ordinal,
            onSelect = { onTab(StatsTab.entries[it]) },
            modifier = Modifier.padding(horizontal = Spacing.ml),
        )
        Spacer(Modifier.height(Spacing.m))
        Period(state, onMove)
        Spacer(Modifier.height(Spacing.m))
        StatsChartCard(state, onPickBar, Modifier.padding(horizontal = Spacing.ml))
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
        Spacer(Modifier.height(Spacing.l))
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
    val stats = state.stats
    val total = grouped(stats.steps) to stringResource(R.string.stats_total)
    val average = grouped(stats.averageSteps) to stringResource(R.string.stats_average)
    val distance = distanceText(stats.distanceM, state.distanceUnit) to
        stringResource(R.string.today_distance)
    val goalDays = stringResource(R.string.stats_goal_days)
    val best = stats.bestDay?.takeIf { it.steps > 0 }
    val bestDay = (best?.let { grouped(it.steps) } ?: NOTHING) to when {
        best == null -> stringResource(R.string.stats_best_day)
        state.tab == StatsTab.Week -> stringResource(R.string.stats_best_day_on, best.day.format(Weekday))
        else -> stringResource(R.string.stats_best_day_on, best.day.format(MonthDay))
    }
    return when (state.tab) {
        StatsTab.Week -> listOf(total, average, distance, bestDay)
        StatsTab.Month -> listOf(
            total,
            average,
            stringResource(R.string.stats_days_of, stats.goalDays, state.calendar.size) to goalDays,
            bestDay,
        )
        StatsTab.Year -> listOf(
            average,
            distance,
            stats.goalDays.toString() to goalDays,
            (state.bestMonth?.format(MonthName) ?: NOTHING) to stringResource(R.string.stats_best_month),
        )
        StatsTab.All -> listOf(
            distance,
            grouped(state.walks) to stringResource(R.string.stats_walks),
            counted(state.longestStreak, R.string.stats_day_one, R.string.stats_day_many) to
                stringResource(R.string.stats_longest_streak),
            bestDay,
        )
    }
}
