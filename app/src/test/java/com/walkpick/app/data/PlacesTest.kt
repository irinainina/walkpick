package com.walkpick.app.data

import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.FindKind
import com.walkpick.app.data.places.ownPlaces
import com.walkpick.app.data.profile.oldTargets
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.ui.screens.nearText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class PlacesTest {

    private val noon = Instant.parse("2026-10-14T12:00:00Z")

    private fun find(id: String, latitude: Double, kind: FindKind, name: String?) =
        FindEntity(id, "walk", noon, latitude, 34.47494, "", kind = kind, name = name, updatedAt = noon)

    // A thousandth of a degree of latitude is about 111 m.
    private val here = find("here", 50.58089, FindKind.PLACE, null)

    @Test
    fun theWalkersOwnPlaceNamesWithin50mAreOfferedNearestFirstAndOnce() {
        val earlier = listOf(
            find("a", 50.58119, FindKind.PLACE, "Old oak"),
            find("b", 50.58099, FindKind.PLACE, "Swan monument"),
            find("c", 50.58109, FindKind.PLACE, "Swan monument"),
            find("d", 50.58189, FindKind.PLACE, "Far chapel"),
            find("e", 50.58090, FindKind.PLANT, "Common yarrow"),
            find("f", 50.58090, FindKind.PLACE, null),
        )
        val offered = ownPlaces(here, earlier)
        assertEquals(listOf("Swan monument", "Old oak"), offered.map { it.name })
        assertEquals(listOf(11, 33), offered.map { it.distanceM })
        assertNull(offered.first().kind)
    }

    @Test
    fun aPlaceAFewStepsAwayIsWrittenInMetresOrFeet() {
        assertEquals("40 m", nearText(40, DistanceUnit.KM))
        assertEquals("131 ft", nearText(40, DistanceUnit.MI))
        assertEquals("1.2 km", nearText(1200, DistanceUnit.KM))
    }

    @Test
    fun theDefaultsOfVersion01AreNoTargetsAndOwnOnesStay() {
        val defaults = oldTargets(120, 80, 5.5)
        assertNull(defaults.targetSystolic)
        assertNull(defaults.targetDiastolic)
        assertNull(defaults.targetGlucose)
        val own = oldTargets(130, 85, 5.2)
        assertEquals(130 to 85, own.targetSystolic to own.targetDiastolic)
        assertEquals(5.2, own.targetGlucose)
        assertNull(oldTargets(130, null, null).targetSystolic)
    }
}
