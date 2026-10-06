package com.walkspot.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.data.local.HealthEntryEntity
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.settings.AppSettings
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.IconBadge
import com.walkspot.app.ui.components.Mood
import com.walkspot.app.ui.components.MoodFace
import com.walkspot.app.ui.glucoseNumber
import com.walkspot.app.ui.grouped
import com.walkspot.app.ui.health.ACTIVE_DAY_STEPS
import com.walkspot.app.ui.health.HealthCurrent
import com.walkspot.app.ui.health.HealthInsight
import com.walkspot.app.ui.health.HealthState
import com.walkspot.app.ui.localDay
import com.walkspot.app.ui.oneDecimal
import com.walkspot.app.ui.stats.StatsTab
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.ParagraphStyle
import com.walkspot.app.ui.theme.Spacing
import com.walkspot.app.ui.weightText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val CardHeight = 96.dp
private val FaceSize = 56.dp
private val ReadingHeight = 40.dp

// The time and the unit stand on the line of the large number, not on its box.
private val TimeDrop = 6.dp
private val UnitLift = 2.dp

private val WeekdayName = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
private val MonthDay = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
private val MonthName = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
private val MonthAndYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val ClockTime = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val DayAndTime = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH)

/** The latest entry of the tab, large, and how the period went under it. */
@Composable
internal fun CurrentCard(state: HealthState, modifier: Modifier = Modifier) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    val current = state.current
    val mood = state.tab == HealthKind.MOOD
    AppCard(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CardHeight)
                .padding(horizontal = Spacing.ml, vertical = if (mood) Spacing.ml else Spacing.m),
        ) {
            if (current != null && mood) {
                MoodFace(Mood.of(current.latest.value.roundToInt()), size = FaceSize)
                Spacer(Modifier.width(Spacing.m))
            }
            Column(Modifier.weight(1f)) {
                val note = if (current == null) {
                    Text(
                        text = stringResource(R.string.health_no_entries),
                        style = type.headlineSmall,
                        color = colors.textPrimary,
                    )
                    stringResource(R.string.health_add_hint) to false
                } else {
                    // The time stands beside the reading only, so the note under them has the whole width.
                    Row {
                        Reading(current.latest, state.units, Modifier.weight(1f))
                        Text(
                            text = momentText(current.latest.measuredAt, state.today),
                            modifier = Modifier.padding(top = if (mood) Spacing.xs else TimeDrop),
                            style = type.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                    periodNote(current, state)
                }
                if (note != null) {
                    if (!mood) Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = note.first,
                        style = if (current == null) type.bodyMedium else type.labelMedium,
                        color = if (note.second) colors.positive else colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun Reading(entry: HealthEntryEntity, units: AppSettings, modifier: Modifier = Modifier) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    if (entry.kind == HealthKind.MOOD) {
        val title = stringResource(Mood.of(entry.value.roundToInt()).titleRes)
        Text(title, modifier, style = type.headlineMedium, color = colors.textPrimary)
        return
    }
    val (number, unit) = when (entry.kind) {
        HealthKind.WEIGHT -> oneDecimal(entry.value * units.weight.perMetric) to units.weight.symbol
        HealthKind.GLUCOSE -> glucoseNumber(entry.value, units.glucose) to units.glucose.symbol
        else -> "${entry.value.roundToInt()}/${entry.diastolic}" to stringResource(R.string.profile_mmhg)
    }
    // The large digits measure a pixel over their line height.
    Row(modifier.height(ReadingHeight), verticalAlignment = Alignment.Bottom) {
        Text(number, style = type.displaySmall, color = colors.textPrimary)
        Spacer(Modifier.width(Spacing.xs))
        Text(unit, Modifier.padding(bottom = UnitLift), style = ParagraphStyle, color = colors.textSecondary)
    }
}

/** The line under the reading and whether it is good news; null when the period has nothing to compare. */
@Composable
private fun periodNote(current: HealthCurrent, state: HealthState): Pair<String, Boolean>? {
    val period = stringResource(
        when (state.period) {
            StatsTab.Week -> R.string.health_this_week
            StatsTab.Month -> R.string.health_this_month
            StatsTab.Year -> R.string.health_this_year
            StatsTab.All -> R.string.health_overall
        },
    )
    if (state.tab == HealthKind.MOOD) {
        val usual = Mood.of(current.usualMood ?: return null)
        val name = stringResource(usual.titleRes).lowercase(Locale.ENGLISH)
        return stringResource(R.string.health_mostly, name, period) to (usual >= Mood.Good)
    }
    val change = current.change ?: return null
    val size = abs(change)
    val (amount, none) = when (state.tab) {
        HealthKind.WEIGHT -> (size * state.units.weight.perMetric).let {
            "${oneDecimal(it)} ${state.units.weight.symbol}" to (oneDecimal(it) == oneDecimal(0.0))
        }
        HealthKind.GLUCOSE -> glucoseNumber(size, state.units.glucose).let {
            it to (it == glucoseNumber(0.0, state.units.glucose))
        }
        else -> stringResource(R.string.health_systolic_change, size.roundToInt()) to (size.roundToInt() == 0)
    }
    if (none) return stringResource(R.string.health_no_change, period) to false
    return "${if (change < 0) "↓" else "↑"} $amount $period" to current.towardTarget
}

/** The remark under the chart, see [HealthInsight]. */
@Composable
internal fun InsightCard(insight: HealthInsight, state: HealthState, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    AppCard(modifier) {
        Row(
            modifier = Modifier.padding(Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(R.drawable.ic_leaf, colors.positiveTint, colors.positive)
            Text(
                text = insightText(insight, state),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun insightText(insight: HealthInsight, state: HealthState): String {
    // "in January", "in January 2025", "on Monday", "on Sep 1": the start of what the chart shows.
    val since = { day: LocalDate ->
        when (state.period) {
            StatsTab.Week -> R.string.health_since_day to WeekdayName
            StatsTab.Month -> R.string.health_since_day to MonthDay
            StatsTab.Year -> R.string.health_since_month to MonthName
            StatsTab.All -> R.string.health_since_month to MonthAndYear
        }.let { (res, format) -> res to format.format(day) }
    }
    return when (insight) {
        HealthInsight.BetterMoodOnActiveDays ->
            stringResource(R.string.health_insight_mood, grouped(ACTIVE_DAY_STEPS))
        is HealthInsight.WalkMore -> since(insight.since).let { (res, day) ->
            stringResource(R.string.health_insight_steps, grouped(insight.steps), stringResource(res, day))
        }
        is HealthInsight.Changed -> {
            val size = abs(insight.change)
            val (subject, amount) = when (state.tab) {
                HealthKind.WEIGHT -> R.string.health_weight to weightText(size, state.units.weight)
                HealthKind.GLUCOSE -> R.string.health_glucose to glucoseNumber(size, state.units.glucose)
                else -> R.string.health_systolic_pressure to size.roundToInt().toString()
            }
            val (res, day) = since(insight.since)
            stringResource(
                if (insight.change < 0) R.string.health_insight_lower else R.string.health_insight_higher,
                stringResource(subject),
                amount,
                stringResource(res, day),
            )
        }
    }
}

@Composable
internal fun momentText(at: Instant, today: LocalDate): String {
    val moment = at.atZone(ZoneId.systemDefault())
    val time = ClockTime.format(moment)
    return when (at.localDay()) {
        today -> stringResource(R.string.health_today_at, time)
        today.minusDays(1) -> stringResource(R.string.health_yesterday_at, time)
        else -> DayAndTime.format(moment)
    }
}
