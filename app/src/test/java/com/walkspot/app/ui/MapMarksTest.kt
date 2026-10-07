package com.walkspot.app.ui

import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.ui.map.ClusterMark
import com.walkspot.app.ui.map.FindMark
import com.walkspot.app.ui.map.MapFilter
import com.walkspot.app.ui.map.mapMarks
import com.walkspot.app.ui.map.shownFinds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class MapMarksTest {

    private fun find(id: String, kind: FindKind, latitude: Double, longitude: Double) =
        FindEntity(id, "walk", Instant.EPOCH, latitude, longitude, "", kind, updatedAt = Instant.EPOCH)

    // Two finds some ten metres apart and one a kilometre to the north.
    private val finds = listOf(
        find("yarrow", FindKind.PLANT, 50.45000, 30.52000),
        find("tit", FindKind.BIRD, 50.45005, 30.52010),
        find("church", FindKind.PLACE, 50.45900, 30.52000),
        find("blur", FindKind.UNKNOWN, 50.47000, 30.52000),
    )

    @Test
    fun aFilterShowsItsKindAndAnUnknownFindIsSeenUnderAllOnly() {
        assertEquals(4, shownFinds(finds, MapFilter.All).size)
        assertEquals(listOf("yarrow"), shownFinds(finds, MapFilter.Plants).map { it.id })
        assertEquals(listOf("tit"), shownFinds(finds, MapFilter.Birds).map { it.id })
        assertEquals(listOf("church"), shownFinds(finds, MapFilter.Places).map { it.id })
    }

    @Test
    fun findsCloseTogetherBecomeOneClusterFromAfarAndFallApartUpClose() {
        val afar = mapMarks(finds, zoom = 14f)
        val cluster = afar.filterIsInstance<ClusterMark>().single()
        assertEquals(setOf("yarrow", "tit"), cluster.finds.map { it.id }.toSet())
        assertEquals(2, afar.filterIsInstance<FindMark>().size)
        assertEquals(50.450025, cluster.latitude, 1e-9)

        val close = mapMarks(finds, zoom = 21f)
        assertTrue(close.all { it is FindMark })
        assertEquals(4, close.size)
    }

    @Test
    fun fromVeryFarEverythingIsOneCluster() {
        assertEquals(listOf(4), mapMarks(finds, zoom = 8f).map { (it as ClusterMark).finds.size })
        assertEquals(emptyList<Any>(), mapMarks(emptyList(), zoom = 8f))
    }
}
