package com.walkpick.app.ui

import com.walkpick.app.data.local.DailyStepsEntity
import com.walkpick.app.data.local.HealthEntryEntity
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.ui.health.HealthInsight
import com.walkpick.app.ui.health.healthInsight
import com.walkpick.app.ui.stats.StatsTab
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class HealthInsightTest {

    // Wednesday, as in the designs
    private val today = LocalDate.of(2026, 9, 30)
    private val zone = ZoneOffset.UTC
    private val january = LocalDate.of(2026, 1, 1)

    private fun entry(kind: HealthKind, day: String, value: Double): HealthEntryEntity {
        val at = LocalDate.parse(day).atTime(8, 10).toInstant(zone)
        return HealthEntryEntity(day + value, kind, at, value)
    }

    private fun day(date: String, steps: Int) =
        DailyStepsEntity(LocalDate.parse(date), steps, 0, 0, 10000, Instant.EPOCH)

    private val weights = listOf(
        entry(HealthKind.WEIGHT, "2026-01-10", 66.3),
        entry(HealthKind.WEIGHT, "2026-09-30", 64.2),
    )

    // Every day of January and of September, so that the month averages are these numbers.
    private fun month(month: Int, days: Int, steps: Int) =
        List(days) { day("2026-%02d-%02d".format(month, it + 1), steps) }

    private val walkingMore = month(1, 31, 4800) + month(9, 30, 7100)

    private fun insight(
        entries: List<HealthEntryEntity> = weights,
        days: List<DailyStepsEntity> = walkingMore,
        target: Double? = 62.0,
        kind: HealthKind = HealthKind.WEIGHT,
        period: StatsTab = StatsTab.Year,
    ) = healthInsight(kind, entries, days, period, today, target, zone)

    @Test
    fun aValueThatNearsItsTargetWhileTheStepsGrowIsRemarked() {
        val changed = insight() as HealthInsight.Changed
        assertEquals(-2.1, changed.change, 1e-9)
        assertEquals(january, changed.since)
    }

    @Test
    fun withoutProgressToTheTargetOnlyTheStepsAreRemarked() {
        assertEquals(HealthInsight.WalkMore(2300, january), insight(target = 70.0))
        assertEquals(HealthInsight.WalkMore(2300, january), insight(target = null))
    }

    @Test
    fun nothingIsSaidWhenTheStepsDidNotGrowOrThereIsNothingToCompare() {
        assertNull(insight(days = month(1, 31, 7100) + month(9, 30, 4800)))
        assertNull(insight(days = emptyList()))
        assertNull(insight(entries = weights.takeLast(1)))
        assertNull(insight(entries = emptyList()))
    }

    @Test
    fun moodIsComparedBetweenActiveAndQuietDays() {
        val active = listOf("01", "02", "03").map { "2026-09-$it" }
        val quiet = listOf("04", "05", "06").map { "2026-09-$it" }
        val days = active.map { day(it, 9000) } + quiet.map { day(it, 2000) }
        val good = { day: String -> entry(HealthKind.MOOD, day, 4.0) }
        val better = active.map(good) + quiet.map { entry(HealthKind.MOOD, it, 3.0) }
        val same = (active + quiet).map(good)
        val mood = { entries: List<HealthEntryEntity> ->
            insight(entries, days, target = null, kind = HealthKind.MOOD, period = StatsTab.Month)
        }
        assertEquals(HealthInsight.BetterMoodOnActiveDays, mood(better))
        assertNull(mood(same))
        assertNull(mood(better.dropLast(1)))
    }
}
