package com.walkspot.app.data

import com.walkspot.app.data.walk.TrackPoint
import com.walkspot.app.data.walk.WalkRecorder
import com.walkspot.app.data.walk.WalkTrack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class WalkTrackTest {

    private val start = Instant.parse("2026-09-29T15:40:00Z")

    // A thousandth of a degree of latitude is 111.2 m.
    private fun point(north: Double, seconds: Long) =
        TrackPoint(50.45 + north, 30.52, start.plusSeconds(seconds), 8f)

    private fun at(seconds: Long) = start.plusSeconds(seconds)

    @Test
    fun pointsThatContinueTheRouteAreKeptAndNoiseIsDropped() {
        val track = WalkTrack(start).with(point(0.0, 0)).with(point(0.00001, 5)).with(point(0.001, 60))
        assertEquals(2, track.points.size)
        assertEquals(111.2, track.metres, 0.1)
    }

    @Test
    fun aPauseStopsTheClockAndTheRoute() {
        val paused = WalkTrack(start).with(point(0.0, 0)).with(point(0.001, 60)).pausedAt(at(60))
        assertTrue(paused.paused)
        assertEquals(60, paused.movingSeconds(at(300)))
        assertEquals(2, paused.with(point(0.002, 120)).points.size)
    }

    @Test
    fun theWayCoveredDuringAPauseIsNotWalkedDistance() {
        val track = WalkTrack(start)
            .with(point(0.0, 0))
            .with(point(0.001, 60))
            .pausedAt(at(60))
            .resumedAt(at(360))
            // Paused at one place, went on five hundred metres away.
            .with(point(0.006, 370))
            .with(point(0.007, 430))
        assertEquals(2, track.legs.size)
        assertEquals(4, track.points.size)
        assertEquals(222.4, track.metres, 0.2)
        assertEquals(130, track.movingSeconds(at(430)))
        assertEquals(300, track.pausedSeconds)
    }

    @Test
    fun stepsAreCountedFromTheFirstReadingAndNotDuringAPause() {
        val walking = WalkTrack(start).withCounter(18_400).withCounter(18_520)
        assertEquals(120, walking.steps)
        val resumed = walking.pausedAt(at(60)).withCounter(18_600).resumedAt(at(120)).withCounter(18_650)
        assertEquals(170, resumed.steps)
        // The phone was restarted: its counter began again.
        assertEquals(170, resumed.withCounter(12).steps)
        assertEquals(200, resumed.withCounter(12).withCounter(42).steps)
    }

    @Test
    fun theRecorderHoldsOneWalkAndHandsItOverAtTheEnd() {
        val recorder = WalkRecorder()
        recorder.report(point(0.0, 0))
        assertNull(recorder.track.value)
        recorder.start(start)
        recorder.start(at(30))
        recorder.report(point(0.0, 0))
        assertEquals(start, recorder.track.value?.startedAt)
        assertEquals(1, recorder.finish()?.points?.size)
        assertNull(recorder.track.value)
    }
}
