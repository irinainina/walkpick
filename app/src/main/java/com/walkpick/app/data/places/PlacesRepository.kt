package com.walkpick.app.data.places

import android.content.Context
import com.walkpick.app.data.local.SyncJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor

private const val CELLS_IN_DEGREE = 50
private const val RADIUS_M = 2_000
private const val PLACES_IN_CELL = 30
private val KeepFor = Duration.ofDays(30)

/** A place with an article in Wikipedia. */
@Serializable
data class Place(
    @SerialName("pageid") val id: Long,
    val title: String,
    @SerialName("lat") val latitude: Double,
    @SerialName("lon") val longitude: Double,
)

@Serializable
private data class SearchAnswer(val query: Found = Found()) {
    @Serializable
    data class Found(val geosearch: List<Place> = emptyList())
}

/** The square of the map one search covers, about two kilometres across: places are asked by squares. */
data class PlaceCell(val row: Int, val column: Int) {
    val latitude: Double get() = (row + HALF) / CELLS_IN_DEGREE
    val longitude: Double get() = (column + HALF) / CELLS_IN_DEGREE

    private companion object {
        const val HALF = 0.5
    }
}

fun placeCell(latitude: Double, longitude: Double) =
    PlaceCell(floor(latitude * CELLS_IN_DEGREE).toInt(), floor(longitude * CELLS_IN_DEGREE).toInt())

/** The places of an answer of Wikipedia; none when the answer is not one. */
internal fun parsePlaces(answer: String): List<Place> = try {
    SyncJson.decodeFromString<SearchAnswer>(answer).query.geosearch
} catch (_: SerializationException) {
    emptyList()
}

/** Places from Wikipedia around a point; every square is asked once and kept in a file for a month. */
@Singleton
class PlacesRepository(
    private val folder: File,
    private val search: suspend (PlaceCell) -> String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    @Inject
    constructor(@ApplicationContext context: Context, wikipedia: Wikipedia) : this(
        folder = File(context.cacheDir, "places"),
        search = { wikipedia.near(it.latitude, it.longitude, RADIUS_M, PLACES_IN_CELL) },
    )

    /** Without the network: what was kept, however old, or nothing. */
    suspend fun near(cell: PlaceCell): List<Place> = withContext(Dispatchers.IO) {
        val kept = File(folder, "${cell.row}_${cell.column}.json")
        val fresh = kept.exists() && now() - kept.lastModified() < KeepFor.toMillis()
        val answer = try {
            if (fresh) {
                kept.readText()
            } else {
                search(cell).also {
                    folder.mkdirs()
                    kept.writeText(it)
                }
            }
        } catch (_: IOException) {
            if (kept.exists()) kept.readText() else ""
        }
        parsePlaces(answer)
    }
}
