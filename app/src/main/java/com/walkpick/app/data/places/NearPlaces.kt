package com.walkpick.app.data.places

import com.walkpick.app.BuildConfig
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.FindKind
import com.walkpick.app.data.local.SyncJson
import com.walkpick.app.data.remote.SupabaseProvider
import com.walkpick.app.data.remote.signedInSession
import com.walkpick.app.data.remote.webClient
import com.walkpick.app.data.walk.TrackPoint
import com.walkpick.app.data.walk.distanceM
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

// A name the walker gave a place before is offered again this close to it.
private const val OWN_NAME_M = 50

/** An article of Wikipedia in some language. */
@Serializable
data class Article(val language: String, val title: String)

/** A place near a photo, as the service finds it in Wikidata and Google Places. */
@Serializable
data class NearPlace(
    val name: String,
    /** What it is, in English: "Church", "Monument"; null for a name of the walker's own. */
    val kind: String? = null,
    @SerialName("distance_m") val distanceM: Int,
    val article: Article? = null,
)

@Serializable
private data class NearAnswer(val places: List<NearPlace> = emptyList())

/** Names of the walker's earlier places within 50 m of [find], the nearest first, as places to offer. */
internal fun ownPlaces(find: FindEntity, earlier: List<FindEntity>): List<NearPlace> {
    val here = find.point()
    return earlier
        .filter { it.id != find.id && it.kind == FindKind.PLACE && it.name != null }
        .map { NearPlace(checkNotNull(it.name), distanceM = distanceM(here, it.point()).toInt()) }
        .filter { it.distanceM <= OWN_NAME_M }
        .sortedBy { it.distanceM }
        .distinctBy { it.name }
}

private fun FindEntity.point() = TrackPoint(latitude, longitude, Instant.EPOCH, 0f)

/** Asks the service what places stand around a photo. */
class NearPlaces @Inject constructor(private val supabase: SupabaseProvider) {

    /** The nearest first; null when the service could not be asked. */
    suspend fun around(latitude: Double, longitude: Double): List<NearPlace>? = try {
        val token = if (supabase.isConfigured) supabase.client.signedInSession()?.accessToken else null
        val answer = token?.let {
            webClient.get("${BuildConfig.API_URL}/v1/places") {
                parameter("lat", latitude)
                parameter("lon", longitude)
                bearerAuth(it)
            }
        }
        answer?.takeIf { it.status.isSuccess() }
            ?.let { SyncJson.decodeFromString<NearAnswer>(it.bodyAsText()).places }
    } catch (_: IOException) {
        null
    } catch (_: SerializationException) {
        null
    }
}
