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
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

// A photo of a place is taken from close by; the best article and three more for "Not this?".
private const val PLACE_RADIUS_M = 300
private const val PLACES = 4

// Old finds without a description are caught up with a few at a sync, not all at once.
private const val DESCRIBED_AT_ONCE = 20

// A plant the service is less sure of than this is shown as unknown, not under a wrong name.
private const val LEAST_PLANT_MATCH = 0.2f

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

/** No guesses when even the best one is weak. */
internal fun List<Guess>.trusted(): List<Guess> =
    if (firstOrNull()?.let { it.match >= LEAST_PLANT_MATCH } == true) this else emptyList()

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
 * Names a find and describes it: a plant by the server, which looks at the photo sent to it, a bird
 * by the model in the app, a place by the nearest article of Wikipedia. It is asked right after the
 * shot and again with every sync for the finds that got no answer then.
 */
class FindIdentifier @Inject constructor(
    private val finds: FindDao,
    private val photos: PhotoStore,
    private val birds: BirdClassifier,
    private val wikipedia: Wikipedia,
    private val remote: RemoteTables,
) : SyncedPart {

    /** [find] with whatever could be found out now; the same find when nothing could, as offline. */
    suspend fun identified(find: FindEntity): FindEntity = try {
        val named = if (find.name == null) named(find) ?: find else find
        if (named.name == null || named.description != null) {
            named
        } else {
            named.described(wikipedia.summary(named.latinName ?: named.name), Instant.now())
        }
    } catch (_: IOException) {
        find
    }

    private suspend fun named(find: FindEntity): FindEntity? = when (find.kind) {
        FindKind.PLANT -> {
            photos.send(find.photoPath, remote)
            remote.identify(find.photoPath)?.let { find.named(it.trusted(), Instant.now()) }
        }
        FindKind.BIRD -> birds.guesses(photos.file(find.photoPath))?.let { find.named(it, Instant.now()) }
        FindKind.PLACE -> {
            val near = wikipedia.near(find.latitude, find.longitude, PLACE_RADIUS_M, PLACES)
            find.placed(parsePlaces(near), Instant.now())
        }
        FindKind.PHOTO, FindKind.UNKNOWN -> null
    }

    override suspend fun push(remote: RemoteTables) {
        val waiting = finds.unnamed() + finds.undescribed(DESCRIBED_AT_ONCE)
        finds.save(waiting.distinctBy { it.id }.mapNotNull { find -> identified(find).takeIf { it != find } })
    }

    override suspend fun pull(remote: RemoteTables) = Unit
}
