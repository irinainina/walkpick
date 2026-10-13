package com.walkpick.app.ui

import com.walkpick.app.data.local.DailyStepsEntity
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.ui.components.BarKind
import com.walkpick.app.ui.today.todayState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class TodayStateTest {

    // Wednesday, as in the designs
    private val now = LocalDateTime.of(2026, 9, 30, 9, 41)

    private fun day(date: String, steps: Int, goal: Int = 10000) =
        DailyStepsEntity(LocalDate.parse(date), steps, steps * 3 / 4, steps / 40, goal, Instant.EPOCH)

    @Test
    fun todayComesFromItsRowAndTheWeekFromMonday() {
        val days = listOf(
            day("2026-09-27", 9000),
            day("2026-09-28", 10000),
            day("2026-09-29", 5000),
            day("2026-09-30", 6420, goal = 8000),
        )
        val walked = setOf(LocalDate.parse("2026-09-28"), LocalDate.parse("2026-09-29"))
        val state = todayState(days, walked, now, StepsAccess.COUNTING, "Ira", dailySteps = 10_000)

        assertEquals(6420, state.steps)
        assertEquals(4815 to 160, state.distanceM to state.activeMinutes)
        assertEquals(2, state.streak)
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), state.week.map { it.label })
        assertEquals(listOf(1f, 0.5f, 0.642f, 0f), state.week.take(4).map { it.fraction })
        assertEquals(
            listOf(BarKind.Past, BarKind.Past, BarKind.Today, BarKind.Past),
            state.week.take(4).map { it.kind },
        )
    }

    @Test
    fun aDayWithoutStepsShowsZeros() {
        val state = todayState(emptyList(), emptySet(), now, StepsAccess.NOT_ALLOWED, null, 5_000)

        assertEquals(0, state.steps)
        assertEquals(0, state.distanceM + state.activeMinutes + state.streak)
        assertEquals(7, state.week.size)
    }
}
