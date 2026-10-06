package com.walkspot.app.ui.health

import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.HealthEntryEntity
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.steps.stepsStats
import com.walkspot.app.ui.localDay
import com.walkspot.app.ui.stats.StatsTab
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/** A day counts as an active one from this many steps. */
const val ACTIVE_DAY_STEPS = 7_000
private const val GOOD_MOOD = 4

// Fewer days than this on either side say nothing about a mood.
private const val DAYS_TO_COMPARE = 3

/** A soft remark under the chart: what went along with more walking. It never names a cause. */
sealed interface HealthInsight {
    /** The value moved by [change] towards the target since [since], and the steps grew too. */
    data class Changed(val change: Double, val since: LocalDate) : HealthInsight

    /** [steps] more a day than at [since]. */
    data class WalkMore(val steps: Int, val since: LocalDate) : HealthInsight

    /** The mood is good more often on days with [ACTIVE_DAY_STEPS] and more. */
    data object BetterMoodOnActiveDays : HealthInsight
}

/** [entries] are of [kind], oldest first; null when the period shows nothing worth a remark. */
internal fun healthInsight(
    kind: HealthKind,
    entries: List<HealthEntryEntity>,
    days: List<DailyStepsEntity>,
    period: StatsTab,
    today: LocalDate,
    target: Double?,
    zone: ZoneId,
): HealthInsight? {
    val first = entries.firstOrNull()?.measuredAt?.localDay(zone) ?: return null
    val range = healthRange(period, today, first)
    if (kind == HealthKind.MOOD) {
        val stepsOf = days.associate { it.day to it.steps }
        val (active, quiet) = entries
            .filter { it.measuredAt.localDay(zone) in range }
            .groupBy({ it.measuredAt.localDay(zone) }, { it.value })
            .entries
            .partition { (stepsOf[it.key] ?: 0) >= ACTIVE_DAY_STEPS }
        val goodShare = { side: List<Map.Entry<LocalDate, List<Double>>> ->
            side.count { it.value.last() >= GOOD_MOOD }.toDouble() / side.size
        }
        val enough = active.size >= DAYS_TO_COMPARE && quiet.size >= DAYS_TO_COMPARE
        val better = enough && goodShare(active) > goodShare(quiet)
        return HealthInsight.BetterMoodOnActiveDays.takeIf { better }
    }
    // The first and the last column of the chart that have a value, with the steps of the same columns.
    val measured = chartColumns(period, today, first).mapNotNull { column ->
        val values = entries.filter { it.measuredAt.localDay(zone) in column }.map { it.value }
        val steps = stepsStats(days, column, today).averageSteps
        if (values.isEmpty()) null else Triple(column.from, values.average(), steps)
    }
    if (measured.size < 2) return null
    val (since, before, stepsBefore) = measured.first()
    val (_, now, stepsNow) = measured.last()
    val moreSteps = stepsNow - stepsBefore
    val nearer = target != null && abs(now - target) < abs(before - target)
    return when {
        stepsBefore == 0 || moreSteps <= 0 -> null
        nearer -> HealthInsight.Changed(now - before, since)
        else -> HealthInsight.WalkMore(moreSteps, since)
    }
}
