package com.walkpick.app.ui.map

import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.FindKind
import java.io.File
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow

// The whole world is this many dp wide at zoom 0 and twice as wide with every level.
private const val WORLD_DP = 256.0
private const val WORLD_DEGREES = 360.0

// Pins nearer than this would lie on each other: such finds are shown as one circle with their number.
private const val CELL_DP = 56

/** What the chips over the map let through. */
enum class MapFilter { All, Plants, Birds, Places }

/** A pin of the map: one find, or several that stand too close to tell apart at this zoom. */
sealed interface MapMark {
    val latitude: Double
    val longitude: Double
}

data class FindMark(val find: FindEntity) : MapMark {
    override val latitude get() = find.latitude
    override val longitude get() = find.longitude
}

data class ClusterMark(val finds: List<FindEntity>) : MapMark {
    override val latitude get() = finds.sumOf { it.latitude } / finds.size
    override val longitude get() = finds.sumOf { it.longitude } / finds.size
}

/** The find the walker tapped on the map, with what the card under the map shows. */
data class MapPick(
    val kind: FindKind,
    /** Null for a find that has no name yet. */
    val title: String?,
    val latitude: Double,
    val longitude: Double,
    val find: FindEntity? = null,
    val text: String? = null,
    val photo: File? = null,
)

/** The finds a filter shows; a find nobody could name is seen under All only. */
internal fun shownFinds(finds: List<FindEntity>, filter: MapFilter): List<FindEntity> = when (filter) {
    MapFilter.All -> finds
    MapFilter.Plants -> finds.filter { it.kind == FindKind.PLANT }
    MapFilter.Birds -> finds.filter { it.kind == FindKind.BIRD }
    MapFilter.Places -> finds.filter { it.kind == FindKind.PLACE }
}

/**
 * The pins for [finds] at [zoom]: the map is cut into cells the size of a pin and a bit, and the
 * finds of one cell become one cluster. Zooming in halves the cells, so a cluster falls apart.
 */
internal fun mapMarks(finds: List<FindEntity>, zoom: Float): List<MapMark> {
    val cell = WORLD_DEGREES * CELL_DP / (WORLD_DP * 2.0.pow(zoom.toDouble()))
    return finds
        .groupBy { find ->
            // A degree of latitude is longer on the screen than a degree of longitude away from the equator.
            val tall = cell * cos(Math.toRadians(find.latitude))
            floor(find.longitude / cell).toLong() to floor(find.latitude / tall).toLong()
        }
        .values
        .map { together -> if (together.size == 1) FindMark(together.first()) else ClusterMark(together) }
}
