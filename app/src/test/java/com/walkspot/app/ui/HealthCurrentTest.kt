package com.walkspot.app.ui

import com.walkspot.app.data.local.HealthEntryEntity
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.ui.health.healthCurrent
import com.walkspot.app.ui.stats.StatsTab
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HealthCurrentTest {

    // Wednesday, as in the designs
    private val today = LocalDate.of(2026, 9, 30)
    private val zone = ZoneOffset.UTC

    private fun entry(day: String, value: Double, kind: HealthKind = HealthKind.WEIGHT) =
        HealthEntryEntity(day, kind, LocalDate.parse(day).atTime(8, 10).toInstant(zone), value)

    private val weights = listOf(
        entry("2025-12-20", 68.0),
        entry("2026-01-05", 66.3),
        entry("2026-09-02", 65.0),
        entry("2026-09-30", 64.2),
    )

    private fun current(period: StatsTab, target: Double? = 62.0) =
        healthCurrent(weights, period, today, target, zone)!!

    @Test
    fun thereIsNoCardDataWithoutEntries() {
        assertNull(healthCurrent(emptyList(), StatsTab.Month, today, 62.0, zone))
    }

    @Test
    fun theChangeIsTheLastEntryOfThePeriodMinusItsFirst() {
        assertEquals(64.2, current(StatsTab.Year).latest.value)
        assertEquals(-2.1, current(StatsTab.Year).change!!, 1e-9)
        assertEquals(-0.8, current(StatsTab.Month).change!!, 1e-9)
        assertEquals(-3.8, current(StatsTab.All).change!!, 1e-9)
    }

    @Test
    fun aPeriodWithOneEntryHasNothingToCompare() {
        assertNull(current(StatsTab.Week).change)
        assertFalse(current(StatsTab.Week).towardTarget)
    }

    @Test
    fun goingTowardsTheTargetIsGoodNewsWhicheverWayItLies() {
        assertTrue(current(StatsTab.Year, target = 62.0).towardTarget)
        assertFalse(current(StatsTab.Year, target = 70.0).towardTarget)
        assertFalse(current(StatsTab.Year, target = null).towardTarget)
    }

    @Test
    fun theUsualMoodIsTheMostFrequentAndTheBetterOfTwoEqual() {
        val moods = listOf(3.0, 4.0, 4.0, 2.0, 5.0, 5.0).mapIndexed { index, mood ->
            entry("2026-09-%02d".format(index + 1), mood, HealthKind.MOOD)
        }
        assertEquals(5, healthCurrent(moods, StatsTab.Month, today, null, zone)!!.usualMood)
        assertEquals(4, healthCurrent(moods.dropLast(1), StatsTab.Month, today, null, zone)!!.usualMood)
    }
}
