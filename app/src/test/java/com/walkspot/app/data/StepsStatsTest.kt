package com.walkspot.app.data

import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.steps.DayRange
import com.walkspot.app.data.steps.MonthAverage
import com.walkspot.app.data.steps.currentStreak
import com.walkspot.app.data.steps.longestStreak
import com.walkspot.app.data.steps.monthOf
import com.walkspot.app.data.steps.monthlyAverages
import com.walkspot.app.data.steps.stepsStats
import com.walkspot.app.data.steps.weekOf
import com.walkspot.app.data.steps.yearOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class StepsStatsTest {

    // Wednesday, as in the designs
    private val today = LocalDate.of(2026, 9, 30)

    private fun day(date: String, steps: Int, distanceM: Int = 0) =
        DailyStepsEntity(LocalDate.parse(date), steps, distanceM, 0, 10000, Instant.EPOCH)

    @Test
    fun periodsStartOnMondayTheFirstAndNewYear() {
        assertEquals(DayRange(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4)), weekOf(today))
        assertEquals(DayRange(LocalDate.of(2026, 9, 1), today), monthOf(YearMonth.of(2026, 9)))
        assertEquals(DayRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)), yearOf(today))
    }

    @Test
    fun aWeekInProgressIsAveragedOverTheDaysThatHaveCome() {
        val days = listOf(
            day("2026-09-20", 4000),
            day("2026-09-28", 12000, 9000),
            day("2026-09-30", 6000, 4500),
        )
        val stats = stepsStats(days, weekOf(today), today)
        assertEquals(18000, stats.steps)
        assertEquals(6000, stats.averageSteps)
        assertEquals(13500, stats.distanceM)
        assertEquals(LocalDate.of(2026, 9, 28), stats.bestDay?.day)
    }

    @Test
    fun daysBeforeTheFirstRecordedOneAreNotAveraged() {
        val days = listOf(day("2026-09-29", 8000), day("2026-09-30", 4000))
        assertEquals(6000, stepsStats(days, yearOf(today), today).averageSteps)
    }

    @Test
    fun anEmptyPeriodIsAllZeros() {
        val stats = stepsStats(emptyList(), weekOf(today), today)
        assertEquals(0, stats.steps + stats.averageSteps + stats.distanceM)
        assertEquals(null, stats.bestDay)
    }

    @Test
    fun monthsAreAveragedPerDay() {
        val days = listOf(day("2026-08-01", 31000), day("2026-09-30", 60000))
        val range = DayRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 31))
        val expected = listOf(
            MonthAverage(YearMonth.of(2026, 8), 1000),
            MonthAverage(YearMonth.of(2026, 9), 2000),
            MonthAverage(YearMonth.of(2026, 10), 0),
        )
        assertEquals(expected, monthlyAverages(days, range, today))
    }

    private fun walked(vararg dates: String) = dates.map { LocalDate.parse("2026-09-$it") }.toSet()

    @Test
    fun todayDoesNotBreakAStreakUntilItIsOver() {
        assertEquals(2, currentStreak(walked("28", "29"), today))
        assertEquals(3, currentStreak(walked("28", "29", "30"), today))
    }

    @Test
    fun aDayWithoutAWalkEndsTheStreak() {
        assertEquals(1, currentStreak(walked("27", "29"), today))
        assertEquals(0, currentStreak(walked("27", "28"), today))
    }

    @Test
    fun theLongestStreakIsFoundAnywhere() {
        assertEquals(3, longestStreak(walked("01", "02", "03", "05", "06", "20")))
        assertEquals(0, longestStreak(emptySet()))
    }
}
