package com.walkspot.app.data.steps

import com.walkspot.app.data.local.DailyStepsEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class DayRange(val from: LocalDate, val to: LocalDate) {
    operator fun contains(day: LocalDate) = day >= from && day <= to
}

fun weekOf(day: LocalDate): DayRange {
    val monday = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return DayRange(monday, monday.plusDays(DayOfWeek.SUNDAY.ordinal.toLong()))
}

fun monthOf(month: YearMonth) = DayRange(month.atDay(1), month.atEndOfMonth())

fun yearOf(day: LocalDate) = DayRange(day.withDayOfYear(1), day.with(TemporalAdjusters.lastDayOfYear()))

data class PeriodStats(
    val steps: Int,
    val averageSteps: Int,
    val distanceM: Int,
    val bestDay: DailyStepsEntity?,
)

data class MonthAverage(val month: YearMonth, val steps: Int)

// Takes every stored day: the average needs to know when the steps began.
fun stepsStats(days: List<DailyStepsEntity>, range: DayRange, today: LocalDate): PeriodStats {
    val inRange = days.filter { it.day in range }
    val steps = inRange.sumOf { it.steps }
    val counted = range.countedDays(days, today)
    return PeriodStats(
        steps = steps,
        averageSteps = if (counted > 0) steps / counted else 0,
        distanceM = inRange.sumOf { it.distanceM },
        bestDay = inRange.maxByOrNull { it.steps },
    )
}

// A day without a row is a day of no steps, but not the days before the first one or after today.
private fun DayRange.countedDays(days: List<DailyStepsEntity>, today: LocalDate): Int {
    val first = days.minOfOrNull { it.day.toEpochDay() } ?: return 0
    val start = maxOf(from.toEpochDay(), first)
    val end = minOf(to.toEpochDay(), today.toEpochDay())
    return (end - start + 1).coerceAtLeast(0).toInt()
}

fun monthlyAverages(days: List<DailyStepsEntity>, range: DayRange, today: LocalDate): List<MonthAverage> =
    generateSequence(YearMonth.from(range.from)) { it.plusMonths(1) }
        .takeWhile { it <= YearMonth.from(range.to) }
        .map { MonthAverage(it, stepsStats(days, monthOf(it), today).averageSteps) }
        .toList()

// A streak is days in a row with a recorded walk; today does not break it until it is over.
fun currentStreak(walkDays: Set<LocalDate>, today: LocalDate): Int {
    var day = if (today in walkDays) today else today.minusDays(1)
    var streak = 0
    while (day in walkDays) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

fun longestStreak(walkDays: Set<LocalDate>): Int {
    var longest = 0
    var run = 0
    var previous: LocalDate? = null
    walkDays.sortedBy { it.toEpochDay() }.forEach { day ->
        run = if (previous?.plusDays(1) == day) run + 1 else 1
        longest = maxOf(longest, run)
        previous = day
    }
    return longest
}
