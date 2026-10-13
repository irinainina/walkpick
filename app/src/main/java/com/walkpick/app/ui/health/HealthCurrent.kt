package com.walkpick.app.ui.health

import com.walkpick.app.data.local.HealthEntryEntity
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.profile.Profile
import com.walkpick.app.data.steps.DayRange
import com.walkpick.app.data.steps.monthOf
import com.walkpick.app.data.steps.weekOf
import com.walkpick.app.data.steps.yearOf
import com.walkpick.app.ui.localDay
import com.walkpick.app.ui.stats.StatsTab
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/** What the card at the top of a tab says: the latest entry and how the period went. */
data class HealthCurrent(
    val latest: HealthEntryEntity,
    /** The last entry of the period minus its first one; null when the period holds fewer than two. */
    val change: Double? = null,
    /** The change leads to the target set in Profile. */
    val towardTarget: Boolean = false,
    /** Mood only: the mood met most often in the period, the better one of two equally frequent. */
    val usualMood: Int? = null,
)

/** A tab of Health on the Today card: its latest entry and the change this month, if it has any. */
data class HealthTile(val kind: HealthKind, val current: HealthCurrent?)

/** The target lines of a chart, the upper one first; the card and the tile compare with the first. */
internal fun Profile.healthTargets(kind: HealthKind): List<Double> = when (kind) {
    HealthKind.MOOD -> emptyList()
    HealthKind.WEIGHT -> listOfNotNull(targetWeightKg)
    HealthKind.PRESSURE -> listOf(targetSystolic.toDouble(), targetDiastolic.toDouble())
    HealthKind.GLUCOSE -> listOf(targetGlucose)
}

/** Health shows the period that holds today; there is nothing to page through. */
internal fun healthRange(period: StatsTab, today: LocalDate, first: LocalDate): DayRange = when (period) {
    StatsTab.Week -> weekOf(today)
    StatsTab.Month -> monthOf(YearMonth.from(today))
    StatsTab.Year -> yearOf(today)
    StatsTab.All -> DayRange(first, today)
}

/** [entries] are of one kind, oldest first; [target] is null where Profile sets none. */
internal fun healthCurrent(
    entries: List<HealthEntryEntity>,
    period: StatsTab,
    today: LocalDate,
    target: Double?,
    zone: ZoneId,
): HealthCurrent? {
    val latest = entries.lastOrNull() ?: return null
    val range = healthRange(period, today, entries.first().measuredAt.localDay(zone))
    val inPeriod = entries.filter { it.measuredAt.localDay(zone) in range }
    // Null unless the period holds two entries to compare.
    val ends = inPeriod.takeIf { it.size > 1 }?.let { it.first().value to it.last().value }
    return HealthCurrent(
        latest = latest,
        change = ends?.let { (first, last) -> last - first },
        towardTarget = target != null && ends != null && abs(ends.second - target) < abs(ends.first - target),
        usualMood = inPeriod
            .groupingBy { it.value.roundToInt() }
            .eachCount()
            .maxWithOrNull(compareBy({ it.value }, { it.key }))
            ?.key,
    )
}
