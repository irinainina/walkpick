package com.walkspot.app.data.places

import android.content.Context
import com.walkspot.app.BuildConfig
import com.walkspot.app.data.local.SyncJson
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
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

private const val SEARCH_URL = "https://en.wikipedia.org/w/api.php"

// Wikipedia refuses requests that do not say who is asking.
private const val ASKED_BY = "WalkSpot/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID})"
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

private val client by lazy { HttpClient(OkHttp) }

private suspend fun search(cell: PlaceCell): String {
    val answer = client.get(SEARCH_URL) {
        header(HttpHeaders.UserAgent, ASKED_BY)
        parameter("action", "query")
        parameter("list", "geosearch")
        parameter("gscoord", "${cell.latitude}|${cell.longitude}")
        parameter("gsradius", RADIUS_M)
        parameter("gslimit", PLACES_IN_CELL)
        parameter("format", "json")
    }
    if (!answer.status.isSuccess()) throw IOException("Wikipedia answered ${answer.status}")
    return answer.bodyAsText()
}

/** Places from Wikipedia around a point; every square is asked once and kept in a file for a month. */
@Singleton
class PlacesRepository(
    private val folder: File,
    private val search: suspend (PlaceCell) -> String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.cacheDir, "places"), ::search)

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
