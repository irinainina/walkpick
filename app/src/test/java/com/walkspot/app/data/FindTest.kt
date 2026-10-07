package com.walkspot.app.data

import com.walkspot.app.data.find.keptScale
import com.walkspot.app.data.find.newFind
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.data.walk.TrackPoint
import com.walkspot.app.data.walk.WalkDraft
import com.walkspot.app.data.walk.WalkTrack
import com.walkspot.app.data.walk.walkEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant

class FindTest {

    private val noon = Instant.parse("2026-10-07T12:00:00Z")

    @Test
    fun aLargePhotoIsShrunkToItsLongestSideAndASmallOneIsLeftAlone() {
        assertEquals(0.4f, keptScale(4000, 3000))
        assertEquals(0.4f, keptScale(3000, 4000))
        assertEquals(1f, keptScale(1200, 900))
    }

    @Test
    fun aNewFindIsUnknownAndStandsWhereThePhoneWas() {
        val find = newFind("find-1", "walk-1", TrackPoint(50.45, 30.52, noon, 8f), noon)

        assertEquals("walk-1", find.walkId)
        assertEquals(50.45, find.latitude)
        assertEquals(30.52, find.longitude)
        assertEquals("find-1.jpg", find.photoPath)
        assertEquals(FindKind.UNKNOWN, find.kind)
        assertFalse(find.synced)
    }

    @Test
    fun aPhotoOfAWalkGoingOnSurvivesTheEndOfTheApp(@TempDir folder: File) {
        val draft = WalkDraft(File(folder, "walk.json"))
        val track = WalkTrack(noon)
        val find = newFind("find-1", track.id, TrackPoint(50.45, 30.52, noon, 8f), noon)

        draft.write(track.copy(finds = listOf(find)))

        assertEquals(track.id, draft.read()?.id)
        assertEquals(listOf(find), draft.read()?.finds)
    }

    @Test
    fun aWalkThatNeverLeftItsSpotIsKeptForItsPhotos() {
        val standing = WalkTrack(noon).with(TrackPoint(50.45, 30.52, noon, 8f))
        val find = newFind("find-1", standing.id, TrackPoint(50.45, 30.52, noon, 8f), noon)

        val kept = walkEntity(standing.copy(finds = listOf(find)), 1, noon.plusSeconds(120), null)

        assertEquals(standing.id, kept?.id)
        assertNull(kept?.route)
        assertEquals(0, kept?.distanceM)
    }

    @Test
    fun theSavedWalkKeepsTheIdItsPhotosPointAt() {
        val track = WalkTrack(noon)
            .with(TrackPoint(50.4500, 30.5200, noon, 8f))
            .with(TrackPoint(50.4510, 30.5200, noon.plusSeconds(90), 8f))

        assertEquals(track.id, walkEntity(track, 1, noon.plusSeconds(120), null)?.id)
    }
}
