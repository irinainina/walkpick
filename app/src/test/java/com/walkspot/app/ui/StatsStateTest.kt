package com.walkspot.app.ui

import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.ui.components.BarKind
import com.walkspot.app.ui.stats.StatsChoice
import com.walkspot.app.ui.stats.StatsTab
import com.walkspot.app.ui.stats.statsState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class StatsStateTest {

    // Wednesday, as in the designs
    private val today = LocalDate.of(2026, 9, 30)

    private fun day(date: String, steps: Int) =
        DailyStepsEntity(LocalDate.parse(date), steps, steps * 3 / 4, steps / 40, 10000, Instant.EPOCH)

    private val days = listOf(
        day("2025-12-31", 31000),
        day("2026-08-31", 3100),
        day("2026-09-28", 10240),
        day("2026-09-29", 6200),
        day("2026-09-30", 6420),
    )

    private fun state(tab: StatsTab, anchor: LocalDate = today, bar: Int? = null) =
        statsState(days, listOf(today.minusDays(1), today), StatsChoice(tab, anchor, bar), today)

    @Test
    fun theWeekShowsItsDaysAndTodayPicked() {
        val week = state(StatsTab.Week)

        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), week.bars.map { it.label })
        val kinds = listOf(BarKind.Past, BarKind.Past, BarKind.Today)
        assertEquals(kinds, week.bars.take(3).map { it.kind })
        assertEquals(6420 / 10240f, week.bars[2].fraction)
        assertEquals(2, week.walkDays)
        assertEquals(2 to "6,420", week.selectedBar to week.bars[2].tip)
        assertEquals(22860 to 7620, week.stats.steps to week.stats.averageSteps)
        assertTrue(week.canGoBack)
        assertFalse(week.canGoForward)
    }

    @Test
    fun aBarPickedByHandStaysPicked() {
        assertEquals(0, state(StatsTab.Week, bar = 0).selectedBar)
    }

    @Test
    fun anEarlierWeekCanBeLeftBothWays() {
        val week = state(StatsTab.Week, anchor = today.minusWeeks(1))

        assertEquals(LocalDate.of(2026, 9, 21), week.range.from)
        assertEquals(null, week.selectedBar)
        assertTrue(week.canGoBack && week.canGoForward)
    }

    @Test
    fun theMonthMarksItsDaysWithAWalk() {
        val month = state(StatsTab.Month)

        assertEquals(30, month.calendar.size)
        assertEquals(listOf(29, 30), month.calendar.filter { it.walked }.map { it.day.dayOfMonth })
        assertEquals(2, month.walkDays)
        assertTrue(month.bars.isEmpty())
    }

    @Test
    fun theYearAveragesItsMonthsAndNamesTheBestOne() {
        val year = state(StatsTab.Year)

        assertEquals(12, year.bars.size)
        assertEquals("JFMAMJJASOND", year.bars.joinToString("") { it.label })
        assertEquals(8, year.selectedBar)
        assertEquals("762", year.bars[8].tip)
        assertEquals(YearMonth.of(2026, 9), year.bestMonth)
    }

    @Test
    fun allStartsAtTheFirstDayAndLabelsTheYears() {
        val all = state(StatsTab.All)

        assertEquals(LocalDate.of(2025, 12, 31), all.range.from)
        assertEquals(listOf("2025", "2026", ""), all.bars.take(3).map { it.label })
        assertEquals(10, all.bars.size)
        assertEquals(null, all.selectedBar)
        assertEquals(2 to 2, all.walks to all.longestStreak)
        assertFalse(all.canGoBack || all.canGoForward)
    }
}
