package com.walkpick.app.data

import com.walkpick.app.data.local.DailyStepsEntity
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.steps.dayOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class StepsTest {

    private val day = LocalDate.of(2026, 9, 29)
    private val now = Instant.parse("2026-09-29T18:00:00Z")
    private val start = Instant.parse("2026-09-29T15:40:00Z")

    private val walk = WalkEntity(
        id = "walk",
        number = 312,
        startedAt = start,
        endedAt = start.plusSeconds(2880),
        durationS = 2880,
        steps = 5230,
        distanceM = 3900,
        updatedAt = start,
    )

    @Test
    fun aDayIsTheSumOfItsWalks() {
        val evening = walk.copy(id = "evening", steps = 770, distanceM = 600, durationS = 420)

        val one = dayOf(day, listOf(walk), 8000, now)
        val two = dayOf(day, listOf(walk, evening), 8000, now)

        assertEquals(DailyStepsEntity(day, 5230, 3900, 48, 8000, now), one)
        assertEquals(DailyStepsEntity(day, 6000, 4500, 55, 8000, now), two)
    }

    @Test
    fun aDayWithoutWalksIsEmptyAndIsSentAgain() {
        val empty = dayOf(day, emptyList(), 8000, now)

        assertEquals(DailyStepsEntity(day, 0, 0, 0, 8000, now, synced = false), empty)
    }
}
