package com.walkpick.app.data.find

import com.walkpick.app.data.local.FindDao
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.PendingDeleteDao
import com.walkpick.app.data.local.PendingDeleteEntity
import com.walkpick.app.data.places.NearPlace
import com.walkpick.app.data.sync.PHOTO_BUCKET
import com.walkpick.app.data.sync.RemoteTables
import com.walkpick.app.data.sync.SyncScheduler
import com.walkpick.app.data.walk.WalkRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.Instant
import javax.inject.Inject

private const val FINDS_TABLE = "finds"

/**
 * The finds wherever they are kept: those of the walk going on lie in its draft, the others in the
 * database. The screens need not know which.
 */
class FindStore @Inject constructor(
    private val recorder: WalkRecorder,
    private val finds: FindDao,
    private val deletes: PendingDeleteDao,
    private val sync: SyncScheduler,
    private val photos: PhotoStore,
    private val remote: RemoteTables,
    private val identifier: FindIdentifier,
) {
    private fun drafted(id: String) = recorder.track.value?.finds?.firstOrNull { it.id == id }

    fun watch(id: String): Flow<FindEntity?> = combine(recorder.track, finds.watch(id)) { track, saved ->
        track?.finds?.firstOrNull { it.id == id } ?: saved
    }

    /** Every kept find, the latest first; those of the walk going on are not among them yet. */
    fun watchAll(): Flow<List<FindEntity>> = finds.watchAll()

    fun watchCount(): Flow<Int> = finds.watchCount()

    /** The find as it is now; null when there is none. */
    suspend fun watchNow(id: String): FindEntity? = drafted(id) ?: finds.get(id)

    /** The photo of a find on this phone, fetched from the server when need be; null when there is none. */
    suspend fun photo(find: FindEntity): File? = photos.local(find.photoPath, remote)

    suspend fun change(id: String, transform: (FindEntity) -> FindEntity) {
        if (drafted(id) != null) {
            recorder.changeFind(id, transform)
            return
        }
        val saved = finds.get(id) ?: return
        finds.save(listOf(transform(saved).copy(updatedAt = Instant.now(), synced = false)))
        sync.pushSoon()
    }

    /** Asks the services for the name and the description the find still lacks. */
    suspend fun identify(id: String) {
        val find = drafted(id) ?: finds.get(id) ?: return
        val better = identifier.identified(find)
        if (better != find) change(id) { better }
    }

    /** The places to offer for a photo of a place: the walker's own names nearby, then those around. */
    suspend fun placesFor(id: String): List<NearPlace> {
        val find = drafted(id) ?: finds.get(id) ?: return emptyList()
        val earlier = finds.watchAll().first() + recorder.track.value?.finds.orEmpty()
        return identifier.placesFor(find, earlier)
    }

    /** Names the find after the place the walker chose; the other [offered] ones go under "Not this?". */
    suspend fun place(id: String, chosen: NearPlace, offered: List<NearPlace>) {
        val find = drafted(id) ?: finds.get(id) ?: return
        val placed = identifier.placed(find, chosen, offered)
        change(id) { placed }
    }

    /** Removes the find and its photo from the phone at once, from the server with the next sync. */
    suspend fun delete(id: String) {
        val find = drafted(id) ?: finds.get(id) ?: return
        if (drafted(id) != null) {
            recorder.removeFind(id)
        } else {
            finds.delete(id)
            deletes.add(PendingDeleteEntity(FINDS_TABLE, id))
        }
        photos.delete(find.photoPath)
        // The photo of a find still in the draft may be on the server already: it was sent to be named.
        deletes.add(PendingDeleteEntity(PHOTO_BUCKET, find.photoPath))
        sync.pushSoon()
    }
}
