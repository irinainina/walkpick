package com.walkpick.app.ui

import com.walkpick.app.ui.walk.routeSketch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WalkSketchTest {

    @Test
    fun theSketchFillsItsSquareAndKeepsTheShapeOfTheRoute() {
        // Twice as far to the north as to the east: a tall route, centred across.
        val tall = routeSketch(listOf(0.0 to 0.0, 0.002 to 0.0, 0.002 to 0.001))
        assertEquals(listOf(0.25f to 1f, 0.25f to 0f, 0.75f to 0f), tall)
        assertTrue(routeSketch(listOf(50.45 to 30.52)).isEmpty())
        assertTrue(routeSketch(listOf(50.45 to 30.52, 50.45 to 30.52)).isEmpty())
    }
}
