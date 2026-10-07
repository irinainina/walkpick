package com.walkspot.app.data.find

import com.walkspot.app.data.local.FindAlternative
import com.walkspot.app.data.local.FindDao
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.data.places.Place
import com.walkspot.app.data.places.WikiSummary
import com.walkspot.app.data.places.Wikipedia
import com.walkspot.app.data.places.parsePlaces
import com.walkspot.app.data.sync.RemoteTables
import com.walkspot.app.data.sync.SyncedPart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import javax.inject.Inject

// A photo of a place is taken from close by; the best article and three more for "Not this?".
private const val PLACE_RADIUS_M = 300
private const val PLACES = 4

// Old finds without a description are caught up with a few at a sync, not all at once.
private const val DESCRIBED_AT_ONCE = 20

/** What a service makes of a photo; the likeliest guess comes first. */
@Serializable
data class Guess(
    val name: String,
    @SerialName("latin_name") val latinName: String = "",
    val match: Float,
)

/**
 * The find with what the service said: the best guess is its name, the others wait under "Not this?".
 * With no guess at all it becomes an unknown find.
 */
internal fun FindEntity.named(guesses: List<Guess>, at: Instant): FindEntity {
    val best = guesses.firstOrNull() ?: return copy(kind = FindKind.UNKNOWN, updatedAt = at, synced = false)
    return copy(
        name = best.name,
        latinName = best.latinName.ifEmpty { null },
        match = best.match,
        alternatives = guesses.drop(1).map {
            FindAlternative(it.name, it.latinName.ifEmpty { null }, it.match)
        },
        updatedAt = at,
        synced = false,
    )
}

/**
 * The find as the nearest article of Wikipedia about a place; the articles further off wait under
 * "Not this?". With no place around it becomes an unknown find.
 */
internal fun FindEntity.placed(places: List<Place>, at: Instant): FindEntity =
    named(places.map { Guess(name = it.title, match = 0f) }, at).copy(match = null)

/**
 * The find with what Wikipedia says about it. A bird is known by its Latin name until now and takes
 * the title of its article; an empty description marks a find Wikipedia has no article for.
 */
internal fun FindEntity.described(summary: WikiSummary?, at: Instant): FindEntity = copy(
    name = if (kind == FindKind.BIRD) summary?.title ?: name else name,
    description = summary?.extract.orEmpty(),
    wikipediaUrl = summary?.url,
    updatedAt = at,
    synced = false,
)

/**
 * Names the finds that have no name yet: plants by the server, birds by the model in the app, places
 * by the nearest article of Wikipedia; then adds the description from Wikipedia. It is a part of the
 * sync and stands after the photos: the server looks at the photo it already holds. A find that got
 * no answer stays as it is and is asked about with the next sync.
 */
class FindIdentifier @Inject constructor(
    private val finds: FindDao,
    private val photos: PhotoStore,
    private val birds: BirdClassifier,
    private val wikipedia: Wikipedia,
) : SyncedPart {

    override suspend fun push(remote: RemoteTables) {
        val plants = finds.unnamed(FindKind.PLANT).mapNotNull { find ->
            remote.identify(find.id)?.let { find.named(it, Instant.now()) }
        }
        val winged = finds.unnamed(FindKind.BIRD).mapNotNull { find ->
            birds.guesses(photos.file(find.photoPath))?.let { find.named(it, Instant.now()) }
        }
        val places = finds.unnamed(FindKind.PLACE).map { find ->
            val near = wikipedia.near(find.latitude, find.longitude, PLACE_RADIUS_M, PLACES)
            find.placed(parsePlaces(near), Instant.now())
        }
        finds.save(plants + winged + places)
        finds.save(
            finds.undescribed(DESCRIBED_AT_ONCE).map { find ->
                find.described(wikipedia.summary(find.latinName ?: find.name.orEmpty()), Instant.now())
            },
        )
    }

    override suspend fun pull(remote: RemoteTables) = Unit
}
