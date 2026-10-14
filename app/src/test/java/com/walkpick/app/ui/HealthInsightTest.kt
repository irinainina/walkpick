package com.walkpick.app.ui

import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.ui.health.HealthInsight
import com.walkpick.app.ui.health.moodInsight
import com.walkpick.app.ui.stats.StatsTab
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HealthInsightTest {

    private val today = LocalDate.of(2026, 9, 30)
    private val zone = ZoneOffset.UTC

    private fun walk(day: String, before: Int?, after: Int?): WalkEntity {
        val at = LocalDate.parse(day).atTime(18, 0).toInstant(zone)
        return WalkEntity(day, 0, at, at, 0, moodBefore = before, moodAfter = after, updatedAt = at)
    }

    @Test
    fun walksWithBothMoodsAreCountedAndThoseThatEndedBetter() {
        val walks = listOf(
            walk("2026-09-02", 3, 4),
            walk("2026-09-05", 4, 4),
            walk("2026-09-09", 2, 5),
            walk("2026-09-12", null, 5),
            walk("2026-08-20", 1, 5),
        )
        assertEquals(HealthInsight(up = 2, walks = 3), moodInsight(walks, StatsTab.Month, today, zone))
        assertEquals(HealthInsight(up = 3, walks = 4), moodInsight(walks, StatsTab.Year, today, zone))
    }

    @Test
    fun nothingIsSaidWithoutAWalkThatHasBothMoods() {
        assertNull(moodInsight(listOf(walk("2026-09-12", null, 5)), StatsTab.Month, today, zone))
        assertNull(moodInsight(emptyList(), StatsTab.Month, today, zone))
    }
}
