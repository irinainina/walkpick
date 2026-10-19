package com.walkpick.app.ui

import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.ui.components.BarKind
import com.walkpick.app.ui.today.todayState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TodayStateTest {

    // Wednesday, as in the designs
    private val now = LocalDateTime.of(2026, 9, 30, 9, 41)

    private fun walk(at: String, steps: Int): WalkEntity {
        val start = LocalDateTime.parse(at).atZone(ZoneId.systemDefault()).toInstant()
        return WalkEntity(at, 1, start, start.plusSeconds(600), 600, steps, steps * 3 / 4, updatedAt = start)
    }

    @Test
    fun theRingShowsTheLatestWalkAndTheWeekStartsOnMonday() {
        val walks = listOf(
            walk("2026-09-28T10:00", 4000),
            walk("2026-09-29T08:00", 3000),
            walk("2026-09-29T18:40", 5000),
            walk("2026-09-27T12:00", 9000),
        )
        val state = todayState(walks, now, StepsAccess.COUNTING, "Ira", dailySteps = 10_000)

        assertEquals(5000, state.lastWalk?.steps)
        assertEquals(3, state.streak)
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), state.week.map { it.label })
        assertEquals(listOf(0.5f, 1f, 0f), state.week.take(3).map { it.fraction })
        val kinds = listOf(BarKind.Walked, BarKind.Walked, BarKind.Today)
        assertEquals(kinds, state.week.take(3).map { it.kind })
        val ofDays = listOf(
            listOf("2026-09-28T10:00"),
            listOf("2026-09-29T08:00", "2026-09-29T18:40"),
            emptyList(),
        )
        assertEquals(ofDays, state.weekWalks.take(3))
    }

    @Test
    fun withoutWalksThereIsNoRing() {
        val state = todayState(emptyList(), now, StepsAccess.NOT_ALLOWED, null, 5_000)

        assertNull(state.lastWalk)
        assertTrue(state.newcomer)
        assertEquals(0, state.streak)
    }
}
