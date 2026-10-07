package com.walkspot.app.ui

import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.local.WalkPhotos
import com.walkspot.app.ui.map.diaryDays
import com.walkspot.app.ui.map.routeSketch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DiaryTest {

    private fun walk(number: Int, startedAt: String) = WalkEntity(
        id = "walk$number",
        number = number,
        startedAt = Instant.parse(startedAt),
        endedAt = Instant.parse(startedAt).plusSeconds(1800),
        durationS = 1800,
        route = "30.52 50.45,30.52 50.451",
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun walksAreGroupedByDayWithTheLatestFirst() {
        val walks = listOf(
            walk(310, "2026-09-27T09:15:00Z"),
            walk(312, "2026-09-29T18:40:00Z"),
            walk(311, "2026-09-27T17:30:00Z"),
        )
        val days = diaryDays(walks, listOf(WalkPhotos("walk312", 7)), ZoneOffset.UTC)
        assertEquals(listOf(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 27)), days.map { it.day })
        assertEquals(listOf(311, 310), days[1].walks.map { it.walk.number })
        assertEquals(7, days[0].walks.single().photos)
        assertEquals(0, days[1].walks.first().photos)
    }

    @Test
    fun aWalkAfterMidnightBelongsToTheDayOfThePhone() {
        val late = walk(1, "2026-09-29T22:30:00Z")
        val days = diaryDays(listOf(late), emptyList(), ZoneOffset.ofHours(3))
        assertEquals(LocalDate.of(2026, 9, 30), days.single().day)
    }

    @Test
    fun theSketchFillsItsSquareAndKeepsTheShapeOfTheRoute() {
        // Twice as far to the north as to the east: a tall route, centred across.
        val tall = routeSketch(listOf(0.0 to 0.0, 0.002 to 0.0, 0.002 to 0.001))
        assertEquals(listOf(0.25f to 1f, 0.25f to 0f, 0.75f to 0f), tall)
        assertTrue(routeSketch(listOf(50.45 to 30.52)).isEmpty())
        assertTrue(routeSketch(listOf(50.45 to 30.52, 50.45 to 30.52)).isEmpty())
    }
}
