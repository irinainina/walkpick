package com.walkpick.app.ui

import com.walkpick.app.data.local.DailyStepsEntity
import com.walkpick.app.data.local.HealthEntryEntity
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.ui.health.HealthData
import com.walkpick.app.ui.health.MoodWay
import com.walkpick.app.ui.health.chartColumns
import com.walkpick.app.ui.health.chartLabels
import com.walkpick.app.ui.health.healthChart
import com.walkpick.app.ui.health.moodLevel
import com.walkpick.app.ui.stats.StatsTab
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class HealthChartTest {

    // Wednesday, as in the designs
    private val today = LocalDate.of(2026, 9, 30)
    private val zone = ZoneOffset.UTC

    private fun entry(kind: HealthKind, day: String, value: Double, lower: Int? = null): HealthEntryEntity {
        val at = LocalDate.parse(day).atTime(8, 10).toInstant(zone)
        return HealthEntryEntity(day + value, kind, at, value, lower)
    }

    private fun day(date: String, steps: Int) =
        DailyStepsEntity(LocalDate.parse(date), steps, 0, 0, 10000, Instant.EPOCH)

    private fun labels(period: StatsTab, first: LocalDate = today) =
        chartLabels(period, today, chartColumns(period, today, first))

    private fun chart(
        kind: HealthKind,
        period: StatsTab,
        entries: List<HealthEntryEntity> = emptyList(),
        days: List<DailyStepsEntity> = emptyList(),
        targets: List<Double> = emptyList(),
        walks: List<WalkEntity> = emptyList(),
    ) = healthChart(kind, HealthData(entries, days, walks), period, today, targets, zone)

    private fun walk(day: String, hour: Int, before: Int?, after: Int?): WalkEntity {
        val at = LocalDate.parse(day).atTime(hour, 0).toInstant(zone)
        return WalkEntity("$day$hour", 0, at, at, 0, moodBefore = before, moodAfter = after, updatedAt = at)
    }

    @Test
    fun theColumnsAreDaysOrMonthsOfThePeriodThatHoldsToday() {
        assertEquals("MTWTFSS".map { it.toString() } to 2, labels(StatsTab.Week))
        assertEquals(30, chartColumns(StatsTab.Month, today, today).size)
        assertEquals(listOf("Sep 1", "Sep 15", "Sep 30") to null, labels(StatsTab.Month))
        assertEquals("JFMAMJJASOND".map { it.toString() } to 8, labels(StatsTab.Year))
        val all = labels(StatsTab.All, LocalDate.of(2025, 11, 20)).first
        assertEquals(11, all.size)
        assertEquals(listOf("2025", "2026"), all.filter { it.isNotEmpty() })
    }

    @Test
    fun aMonthOfTheYearShowsTheAverageOfItsEntriesAndOtherMonthsAreGaps() {
        val entries = listOf(
            entry(HealthKind.WEIGHT, "2026-01-05", 66.0),
            entry(HealthKind.WEIGHT, "2026-09-02", 65.0),
            entry(HealthKind.WEIGHT, "2026-09-30", 63.0),
        )
        val chart = chart(HealthKind.WEIGHT, StatsTab.Year, entries, targets = listOf(62.0))
        val line = chart.lines.single()
        assertNull(line[1])
        // 62 (the target), 64 and 66 are shown: the span is 4, the edges take 0.6 each.
        assertEquals(0.6f / 5.2f, chart.goals.single().at, 1e-6f)
        assertEquals(2.6f / 5.2f, line[8]!!, 1e-6f)
        assertEquals(4.6f / 5.2f, line[0]!!, 1e-6f)
    }

    @Test
    fun pressureDrawsTwoLinesAndTwoTargets() {
        val entries = listOf(entry(HealthKind.PRESSURE, "2026-09-29", 124.0, 80))
        val chart = chart(HealthKind.PRESSURE, StatsTab.Week, entries, targets = listOf(120.0, 80.0))
        assertEquals(2, chart.lines.size)
        assertEquals(listOf(120.0, 80.0), chart.goals.map { it.value })
        assertTrue(chart.lines[0][1]!! > chart.goals[0].at)
        assertEquals(chart.goals[1].at, chart.lines[1][1]!!, 1e-6f)
    }

    @Test
    fun theBarsAreAverageStepsAndTheTallestStopsAtFourFifths() {
        val days = listOf(day("2026-09-28", 10000), day("2026-09-29", 5000))
        val chart = chart(HealthKind.WEIGHT, StatsTab.Week, days = days)
        assertEquals(listOf(0.8f, 0.4f), chart.bars.take(2))
        assertTrue(chart.lines.isEmpty())
    }

    @Test
    fun aMoodBarRunsFromTheMoodBeforeTheFirstWalkToTheMoodAfterTheLast() {
        val walks = listOf(
            walk("2026-09-28", 18, before = 4, after = 5),
            walk("2026-09-28", 8, before = 2, after = 3),
            walk("2026-09-30", 9, before = null, after = 1),
        )
        val chart = chart(HealthKind.MOOD, StatsTab.Week, walks = walks)
        assertEquals(listOf(MoodWay(2, 5), null, MoodWay(1, 1)), chart.moods.take(3))
        assertEquals(8f / 140, moodLevel(1), 1e-6f)
        assertEquals(132f / 140, moodLevel(5), 1e-6f)
    }
}
