package com.walkspot.app.data

import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.steps.CounterReading
import com.walkspot.app.data.steps.addSteps
import com.walkspot.app.data.steps.splitByDay
import com.walkspot.app.data.steps.stepsBetween
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class StepsTest {

    private val day = LocalDate.of(2026, 9, 29)
    private val now = Instant.parse("2026-09-29T18:00:00Z")

    private fun reading(boot: Int, steps: Long) = CounterReading(boot, steps, now)

    private fun at(time: String) = Instant.parse("2026-09-$time:00Z")

    private fun split(from: String, to: String, steps: Int) =
        splitByDay(at(from), at(to), steps, ZoneOffset.UTC)

    @Test
    fun theFirstReadingOnlySetsTheStartingPoint() {
        assertEquals(0, stepsBetween(null, reading(7, 5230)))
    }

    @Test
    fun stepsAreTheGrowthOfTheCounter() {
        assertEquals(110, stepsBetween(reading(7, 5230), reading(7, 5340)))
    }

    @Test
    fun afterARestartTheCounterIsCountedFromZero() {
        assertEquals(300, stepsBetween(reading(7, 5230), reading(8, 300)))
    }

    @Test
    fun stepsOfOneDayStayInIt() {
        assertEquals(mapOf(day to 110), split("29T08:00", "29T09:00", 110))
    }

    @Test
    fun stepsOverMidnightAreSharedByTime() {
        assertEquals(mapOf(day.minusDays(1) to 250, day to 750), split("28T23:00", "29T03:00", 1000))
    }

    @Test
    fun aLongSleepSpreadsStepsOverItsDays() {
        val shares = split("27T12:00", "29T12:00", 1000)
        assertEquals(mapOf(day.minusDays(2) to 250, day.minusDays(1) to 500, day to 250), shares)
    }

    @Test
    fun aNewDayTakesTodaysGoalAndItsEstimates() {
        val rows = addSteps(emptyList(), mapOf(day to 5230), 10000, 0.7, now)
        assertEquals(listOf(DailyStepsEntity(day, 5230, 3661, 130, 10000, now)), rows)
    }

    @Test
    fun aGrownDayKeepsItsGoalAndIsSentAgain() {
        val stored = DailyStepsEntity(day, 5230, 3661, 130, 8000, now.minusSeconds(3600), synced = true)
        val rows = addSteps(listOf(stored), mapOf(day to 770), 10000, 0.7, now)
        assertEquals(listOf(DailyStepsEntity(day, 6000, 4200, 150, 8000, now)), rows)
    }
}
