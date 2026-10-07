package com.walkspot.app.data

import com.walkspot.app.data.walk.TrackPoint
import com.walkspot.app.data.walk.continuesRoute
import com.walkspot.app.data.walk.distanceM
import com.walkspot.app.data.walk.paceSecondsPerKm
import com.walkspot.app.data.walk.routeLengthM
import com.walkspot.app.data.walk.routePlaces
import com.walkspot.app.data.walk.routeText
import com.walkspot.app.data.walk.walkCalories
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class TrackTest {

    private val start = Instant.parse("2026-09-29T15:40:00Z")

    // A thousandth of a degree of latitude is 111.2 m anywhere.
    private fun point(north: Double, seconds: Long, accuracy: Float = 8f, east: Double = 0.0) =
        TrackPoint(50.45 + north, 30.52 + east, start.plusSeconds(seconds), accuracy)

    private val origin = point(0.0, 0)

    @Test
    fun theDistanceIsMeasuredOverTheGlobe() {
        assertEquals(111.2, distanceM(origin, point(0.001, 60)), 0.1)
        assertEquals(70.8, distanceM(origin, point(0.0, 60, east = 0.001)), 0.1)
        assertEquals(222.4, routeLengthM(listOf(origin, point(0.001, 60), point(0.002, 120))), 0.2)
    }

    @Test
    fun aVagueReadingIsNotAPoint() {
        assertTrue(continuesRoute(null, origin))
        assertFalse(continuesRoute(null, point(0.0, 0, accuracy = 45f)))
        assertFalse(continuesRoute(origin, point(0.001, 60, accuracy = 45f)))
    }

    @Test
    fun tremblingOnTheSpotIsNotAMove() {
        // 3.3 m, then 11 m with an accuracy of 28 m: both inside what the phone cannot tell apart.
        assertFalse(continuesRoute(origin, point(0.00003, 10)))
        assertFalse(continuesRoute(origin, point(0.0001, 10, accuracy = 28f)))
        assertTrue(continuesRoute(origin, point(0.0001, 10)))
    }

    @Test
    fun aJumpOfTheSignalIsNotAStep() {
        // 111 m in 5 s is 22 m/s; in 60 s it is a brisk walk.
        assertFalse(continuesRoute(origin, point(0.001, 5)))
        assertTrue(continuesRoute(origin, point(0.001, 60)))
        assertFalse(continuesRoute(origin, point(0.001, 0)))
    }

    @Test
    fun thePaceAppearsOnceThereIsADistance() {
        assertNull(paceSecondsPerKm(30.0, 40))
        // 3.9 km in 48 minutes, as the walk of the designs: 12:18 a kilometre.
        assertEquals(738, paceSecondsPerKm(3900.0, 48 * 60))
    }

    @Test
    fun caloriesFollowTheSpeedAndTheWeight() {
        // 3.9 km in 48 min is 4.9 km/h: 4.3 MET; 64.2 kg for 0.8 h.
        assertEquals(221, walkCalories(3900.0, 48 * 60, 64.2))
        assertEquals(241, walkCalories(3900.0, 48 * 60, null))
        // 2 km/h: 2.8 MET.
        assertEquals(196, walkCalories(2000.0, 3600, 70.0))
        assertEquals(0, walkCalories(0.0, 0, 70.0))
        assertEquals(0, walkCalories(0.0, 600, 70.0))
    }

    @Test
    fun theRouteIsWrittenAsLongitudeThenLatitude() {
        assertNull(routeText(listOf(origin)))
        val text = routeText(listOf(origin, point(0.001, 60)))
        assertEquals("30.520000 50.450000,30.520000 50.451000", text)
        assertEquals(listOf(50.45 to 30.52, 50.451 to 30.52), routePlaces(text))
        assertEquals(emptyList<Pair<Double, Double>>(), routePlaces(null))
        assertEquals(listOf(50.45 to 30.52), routePlaces("30.52 50.45,broken"))
    }
}
