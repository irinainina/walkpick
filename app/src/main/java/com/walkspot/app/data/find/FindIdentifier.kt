package com.walkspot.app.data.find

import com.walkspot.app.data.local.FindAlternative
import com.walkspot.app.data.local.FindDao
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.data.sync.RemoteTables
import com.walkspot.app.data.sync.SyncedPart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import javax.inject.Inject

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
 * Asks the server to name the plants that have no name yet. It is a part of the sync and stands after
 * the photos: the server looks at the photo it already holds. A find the service gave no answer for
 * stays as it is and is asked about with the next sync.
 */
class FindIdentifier @Inject constructor(private val finds: FindDao) : SyncedPart {

    override suspend fun push(remote: RemoteTables) {
        val named = finds.unnamed(FindKind.PLANT).mapNotNull { find ->
            remote.identify(find.id)?.let { find.named(it, Instant.now()) }
        }
        if (named.isNotEmpty()) finds.save(named)
    }

    override suspend fun pull(remote: RemoteTables) = Unit
}
