package com.walkspot.app.data

import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.steps.dayWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
    fun theFirstWalkOfADayMakesTheDay() {
        val made = dayWith(null, walk, sign = 1, goal = 10000, now = now, zone = ZoneOffset.UTC)

        assertEquals(DailyStepsEntity(day, 5230, 3900, 48, 10000, now), made)
    }

    @Test
    fun aSecondWalkIsAddedAndTheDayKeepsItsGoalAndIsSentAgain() {
        val stored = DailyStepsEntity(day, 5230, 3900, 48, 8000, now.minusSeconds(3600), synced = true)
        val evening = walk.copy(steps = 770, distanceM = 600, durationS = 420)

        val grown = dayWith(stored, evening, 1, 10000, now)

        assertEquals(DailyStepsEntity(day, 6000, 4500, 55, 8000, now), grown)
    }

    @Test
    fun aDeletedWalkLeavesItsDayAndTheDayNeverGoesBelowNothing() {
        val stored = DailyStepsEntity(day, 6000, 4500, 55, 8000, now.minusSeconds(3600), synced = true)

        assertEquals(DailyStepsEntity(day, 770, 600, 7, 8000, now), dayWith(stored, walk, -1, 10000, now))
        val empty = DailyStepsEntity(day, 100, 50, 1, 8000, now.minusSeconds(3600))
        assertEquals(DailyStepsEntity(day, 0, 0, 0, 8000, now), dayWith(empty, walk, -1, 10000, now))
    }

    @Test
    fun aWalkBelongsToTheDayItBeganOnInTheZoneOfThePhone() {
        val late = walk.copy(startedAt = Instant.parse("2026-09-29T22:30:00Z"))

        assertEquals(day.plusDays(1), dayWith(null, late, 1, 10000, now, ZoneOffset.ofHours(3)).day)
    }
}
