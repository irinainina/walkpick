package com.walkpick.app.data.places

import com.walkpick.app.data.local.SyncJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

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

/** The places of an answer of Wikipedia; none when the answer is not one. */
internal fun parsePlaces(answer: String): List<Place> = try {
    SyncJson.decodeFromString<SearchAnswer>(answer).query.geosearch
} catch (_: SerializationException) {
    emptyList()
}
