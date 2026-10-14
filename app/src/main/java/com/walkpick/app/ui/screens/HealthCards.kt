package com.walkpick.app.ui.screens

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
import com.walkpick.app.R
import com.walkpick.app.data.local.HealthEntryEntity
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.settings.AppSettings
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.IconBadge
import com.walkpick.app.ui.components.Mood
import com.walkpick.app.ui.components.MoodFace
import com.walkpick.app.ui.counted
import com.walkpick.app.ui.glucoseNumber
import com.walkpick.app.ui.health.HealthCurrent
import com.walkpick.app.ui.health.HealthInsight
import com.walkpick.app.ui.health.HealthState
import com.walkpick.app.ui.localDay
import com.walkpick.app.ui.oneDecimal
import com.walkpick.app.ui.stats.StatsTab
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.ParagraphStyle
import com.walkpick.app.ui.theme.Spacing
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
    val (number, unit) = readingText(entry, units)
    // The large digits measure a pixel over their line height.
    Row(modifier.height(ReadingHeight), verticalAlignment = Alignment.Bottom) {
        Text(number, style = type.displaySmall, color = colors.textPrimary)
        Spacer(Modifier.width(Spacing.xs))
        Text(unit, Modifier.padding(bottom = UnitLift), style = ParagraphStyle, color = colors.textSecondary)
    }
}

/** The number of a weight, pressure or glucose entry in the units of Settings, and the unit. */
@Composable
internal fun readingText(entry: HealthEntryEntity, units: AppSettings): Pair<String, String> =
    when (entry.kind) {
        HealthKind.WEIGHT -> oneDecimal(entry.value * units.weight.perMetric) to units.weight.symbol
        HealthKind.GLUCOSE -> glucoseNumber(entry.value, units.glucose) to units.glucose.symbol
        else -> "${entry.value.roundToInt()}/${entry.diastolic}" to stringResource(R.string.profile_mmhg)
    }

/** The size of a change, written as the reading is, without the unit. */
internal fun changeText(kind: HealthKind, size: Double, units: AppSettings): String = when (kind) {
    HealthKind.WEIGHT -> oneDecimal(size * units.weight.perMetric)
    HealthKind.GLUCOSE -> glucoseNumber(size, units.glucose)
    else -> size.roundToInt().toString()
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
    val number = changeText(state.tab, size, state.units)
    if (number == changeText(state.tab, 0.0, state.units)) {
        return stringResource(R.string.health_no_change, period) to false
    }
    val amount = when (state.tab) {
        HealthKind.WEIGHT -> "$number ${state.units.weight.symbol}"
        HealthKind.PRESSURE -> stringResource(R.string.health_systolic_change, size.roundToInt())
        else -> number
    }
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

// "Your mood went up on 14 of 20 walks this month."
@Composable
private fun insightText(insight: HealthInsight, state: HealthState): String {
    val period = when (state.period) {
        StatsTab.Week -> R.string.health_this_week
        StatsTab.Month -> R.string.health_this_month
        StatsTab.Year -> R.string.health_this_year
        StatsTab.All -> R.string.health_overall
    }
    val walks = counted(insight.walks, R.string.health_walk, R.string.health_walks)
    return stringResource(R.string.health_mood_up, insight.up, walks, stringResource(period))
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
