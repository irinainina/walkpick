package com.walkpick.app.ui

import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.ui.components.BarKind
import com.walkpick.app.ui.stats.StatsChoice
import com.walkpick.app.ui.stats.StatsTab
import com.walkpick.app.ui.stats.statsState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class StatsStateTest {

    // Wednesday, as in the designs
    private val today = LocalDate.of(2026, 9, 30)

    private fun walk(date: String, steps: Int, number: Int): WalkEntity {
        val start = LocalDate.parse(date).atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant()
        return WalkEntity(
            id = "w$number",
            number = number,
            startedAt = start,
            endedAt = start.plusSeconds(1800),
            durationS = 1800,
            steps = steps,
            distanceM = steps * 3 / 4,
            updatedAt = start,
        )
    }

    private val walks = listOf(
        walk("2025-12-31", 3100, 1),
        walk("2026-08-31", 3100, 2),
        walk("2026-09-28", 6000, 3),
        walk("2026-09-28", 4240, 4),
        walk("2026-09-29", 6200, 5),
        walk("2026-09-30", 6420, 6),
    )

    private fun state(tab: StatsTab, anchor: LocalDate = today, bar: Int? = null) =
        statsState(walks, StatsChoice(tab, anchor, bar), today)

    @Test
    fun theWeekAddsUpItsWalksAndPicksToday() {
        val week = state(StatsTab.Week)

        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), week.bars.map { it.label })
        assertEquals(listOf(BarKind.Walked, BarKind.Walked, BarKind.Today), week.bars.take(3).map { it.kind })
        assertEquals(1f, week.bars[0].fraction)
        assertEquals(2 to "6\u00A0420", week.selectedBar to week.bars[2].tip)
        assertEquals(4, week.totals.walks)
        assertEquals(22860 to 4 * 1800, week.totals.steps to week.totals.durationS)
        assertEquals(LocalDate.of(2026, 9, 28) to 10240, week.totals.bestDay to week.totals.bestDaySteps)
        assertEquals(3, week.streak)
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
        assertEquals(0, week.totals.walks)
        assertTrue(week.canGoBack && week.canGoForward)
    }

    @Test
    fun theMonthMarksItsDaysWithAWalk() {
        val month = state(StatsTab.Month)

        assertEquals(30, month.calendar.size)
        assertEquals(listOf(28, 29, 30), month.calendar.filter { it.walked }.map { it.day.dayOfMonth })
        assertEquals(4, month.totals.walks)
        assertTrue(month.bars.isEmpty())
    }

    @Test
    fun theYearAddsUpItsMonthsAndNamesTheBestOne() {
        val year = state(StatsTab.Year)

        assertEquals(12, year.bars.size)
        assertEquals("JFMAMJJASOND", year.bars.joinToString("") { it.label })
        assertEquals(null, year.selectedBar)
        assertEquals("22\u00A0860", year.bars[8].tip)
        assertEquals(YearMonth.of(2026, 9), year.bestMonth)
    }

    @Test
    fun allStartsAtTheFirstMonthAndLabelsTheYears() {
        val all = state(StatsTab.All)

        assertEquals(LocalDate.of(2025, 12, 1), all.range.from)
        assertEquals(listOf("2025", "2026", ""), all.bars.take(3).map { it.label })
        assertEquals(10, all.bars.size)
        assertEquals(null, all.selectedBar)
        assertEquals(6 to 3, all.totals.walks to all.longestStreak)
        assertFalse(all.canGoBack || all.canGoForward)
    }
}
