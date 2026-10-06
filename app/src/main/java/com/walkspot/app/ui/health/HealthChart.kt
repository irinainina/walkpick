package com.walkspot.app.ui.health

import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.HealthEntryEntity
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.steps.DayRange
import com.walkspot.app.data.steps.monthOf
import com.walkspot.app.data.steps.stepsStats
import com.walkspot.app.ui.localDay
import com.walkspot.app.ui.stats.StatsTab
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// The values keep clear of the edges of the chart; the tallest bar stops at four fifths of it.
private const val EDGE_SHARE = 0.15
private const val BARS_TOP = 1.25f

// The five moods stand 31 apart on a chart 140 high, awful 8 above its bottom.
private const val MOOD_FLOOR = 8f / 140
private const val MOOD_STEP = 31f / 140
private const val MID_MONTH = 15

private val MonthDay = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

// Five letters ask for the narrow form: "M" for Monday, "S" for September.
private val WeekdayLetter = DateTimeFormatter.ofPattern("EEEEE", Locale.ENGLISH)
private val MonthLetter = DateTimeFormatter.ofPattern("MMMMM", Locale.ENGLISH)

/** A target of Profile on the chart: where its dashed line runs and the number its label names. */
data class ChartGoal(val at: Float, val value: Double)

data class HealthChart(
    /** Days in Week and Month, months in Year and All. */
    val columns: Int,
    /** Spread evenly under the chart: one for a column, or the start, middle and end of a month. */
    val labels: List<String>,
    /** The label of today or of this month, where there is one. */
    val currentLabel: Int? = null,
    /** Average steps a day in each column, 0..1 of the height. */
    val bars: List<Float>,
    /** One line, or two for pressure; a value 0..1 or a gap for each column. */
    val lines: List<List<Float?>>,
    val goals: List<ChartGoal> = emptyList(),
    /** Mood only: the mood of each column, 1..5; its point stands at [moodLevel]. */
    val moods: List<Int?> = emptyList(),
)

/** How high the point of a mood stands, 0..1 of the chart. */
fun moodLevel(mood: Int): Float = MOOD_FLOOR + (mood - 1) * MOOD_STEP

internal fun chartColumns(period: StatsTab, today: LocalDate, first: LocalDate): List<DayRange> {
    val range = healthRange(period, today, first)
    if (period == StatsTab.Week || period == StatsTab.Month) {
        return generateSequence(range.from) { it.plusDays(1) }
            .takeWhile { it <= range.to }
            .map { DayRange(it, it) }
            .toList()
    }
    return generateSequence(YearMonth.from(range.from)) { it.plusMonths(1) }
        .takeWhile { it <= YearMonth.from(range.to) }
        .map { monthOf(it) }
        .toList()
}

/** The labels under the columns and which of them is the current one. */
internal fun chartLabels(
    period: StatsTab,
    today: LocalDate,
    columns: List<DayRange>,
): Pair<List<String>, Int?> {
    val starts = columns.map { it.from }
    return when (period) {
        StatsTab.Week -> starts.map(WeekdayLetter::format) to starts.indexOf(today)
        StatsTab.Month -> {
            val shown = listOf(starts.first(), starts.first().withDayOfMonth(MID_MONTH), starts.last())
            shown.map(MonthDay::format) to null
        }
        StatsTab.Year -> starts.map(MonthLetter::format) to today.monthValue - 1
        StatsTab.All -> starts.mapIndexed { index, month ->
            if (index == 0 || month.monthValue == 1) month.year.toString() else ""
        } to null
    }
}

/** [entries] are of [kind], oldest first; [targets] are the lines of Profile, the upper one first. */
internal fun healthChart(
    kind: HealthKind,
    entries: List<HealthEntryEntity>,
    days: List<DailyStepsEntity>,
    period: StatsTab,
    today: LocalDate,
    targets: List<Double>,
    zone: ZoneId,
): HealthChart {
    val first = entries.firstOrNull()?.measuredAt?.localDay(zone) ?: today
    val columns = chartColumns(period, today, first)
    val (labels, current) = chartLabels(period, today, columns)
    val inColumns = columns.map { column -> entries.filter { it.measuredAt.localDay(zone) in column } }
    val steps = columns.map { stepsStats(days, it, today).averageSteps }
    val top = (steps.maxOrNull() ?: 0) * BARS_TOP
    val bars = steps.map { if (top > 0) it / top else 0f }
    if (kind == HealthKind.MOOD) {
        // The last mood of the day or month speaks for it.
        val moods = inColumns.map { column -> column.lastOrNull()?.value?.toInt() }
        return HealthChart(columns.size, labels, current, bars, lines = emptyList(), moods = moods)
    }
    val upper = inColumns.map { column -> column.map { it.value }.averageOrNull() }
    val lower = inColumns.map { column -> column.mapNotNull { it.diastolic?.toDouble() }.averageOrNull() }
    val series = listOf(upper, lower).filter { line -> line.any { it != null } }
    val shown = series.flatten().filterNotNull() + targets
    val low = shown.minOrNull() ?: 0.0
    val span = ((shown.maxOrNull() ?: 0.0) - low).takeIf { it > 0 } ?: 1.0
    val edge = span * EDGE_SHARE
    val place = { value: Double -> ((value - low + edge) / (span + 2 * edge)).toFloat() }
    return HealthChart(
        columns = columns.size,
        labels = labels,
        currentLabel = current,
        bars = bars,
        lines = series.map { line -> line.map { value -> value?.let(place) } },
        goals = targets.map { ChartGoal(place(it), it) },
    )
}

private fun List<Double>.averageOrNull() = if (isEmpty()) null else average()
