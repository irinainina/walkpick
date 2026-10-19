package com.walkpick.app.data

import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.walk.TrackPoint
import com.walkpick.app.data.walk.WalkDraft
import com.walkpick.app.data.walk.WalkRecorder
import com.walkpick.app.data.walk.WalkTrack
import com.walkpick.app.data.walk.moodEntries
import com.walkpick.app.data.walk.walkEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
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
    fun aWalkTheAppWasEndedInGoesOnFromItsFile(@TempDir folder: File) {
        val draft = WalkDraft(File(folder, "walk.json"))
        assertNull(WalkRecorder(draft).track.value)
        val walked = WalkTrack(start).with(point(0.0, 0)).with(point(0.001, 60))
        val track = walked.withCounter(100).pausedAt(at(90))
        draft.write(track)
        assertEquals(track, WalkRecorder(draft).track.value)
        draft.write(walked)
        assertEquals(listOf("walk.json"), folder.list()?.toList())
        draft.write(null)
        assertNull(draft.read())
    }

    @Test
    fun aDraftWaitsForTheWayAndTheStepsButNotForAPause() {
        val walked = WalkTrack(start).with(point(0.0, 0))
        assertTrue(walked.with(point(0.001, 60)).withCounter(100).onlyMovedSince(walked))
        assertFalse(walked.pausedAt(at(90)).onlyMovedSince(walked))
        assertFalse(walked.onlyMovedSince(null))
    }

    @Test
    fun aWalkThatNeverLeftItsSpotIsNotKept() {
        val standing = WalkTrack(start).with(point(0.0, 0))
        assertNull(walkEntity(standing, 313, at(600), 64.2, 0.7))
        val walked = standing.with(point(0.001, 60)).withCounter(10).withCounter(160)
        val entry = walkEntity(walked, 313, at(600), 64.2, 0.7)!!
        assertEquals(313, entry.number)
        assertEquals(111, entry.distanceM)
        assertEquals(600, entry.durationS)
        assertEquals(150, entry.steps)
        // The phone cut the counter off on the way: the steps are the ones 111 m take.
        assertEquals(158, standing.with(point(0.001, 60)).withCounter(10).withCounter(40).stepsWith(0.7))
        // GPS lost the fix on the way: the distance is the one 400 steps take.
        val lost = standing.with(point(0.001, 60)).withCounter(10).withCounter(410)
        assertEquals(280, walkEntity(lost, 313, at(600), 64.2, 0.7)!!.distanceM)
        assertEquals("30.520000 50.450000,30.520000 50.451000", entry.route)
        assertEquals(false, entry.synced)
    }

    @Test
    fun theMoodsOfAWalkBecomeEntriesOfHealthAtItsTwoEnds() {
        val moody = WalkTrack(start, moodBefore = 2, moodAfter = 4)
        val walked = moody.with(point(0.0, 0)).with(point(0.001, 60))
        val entry = walkEntity(walked, 313, at(600), 64.2, 0.7)!!
        assertEquals(2 to 4, entry.moodBefore to entry.moodAfter)
        assertNull(entry.breaks)
        val moods = moodEntries(entry)
        assertEquals(listOf(2.0 to start, 4.0 to at(600)), moods.map { it.value to it.measuredAt })
        assertTrue(moods.all { it.kind == HealthKind.MOOD })
        assertTrue(moodEntries(entry.copy(moodBefore = null, moodAfter = null)).isEmpty())
    }

    @Test
    fun theRecorderHoldsOneWalkAndHandsItOverAtTheEnd(@TempDir folder: File) {
        val recorder = WalkRecorder(WalkDraft(File(folder, "walk.json")))
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
