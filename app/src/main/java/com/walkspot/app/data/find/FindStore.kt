package com.walkspot.app.data.find

import com.walkspot.app.data.local.FindDao
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.PendingDeleteDao
import com.walkspot.app.data.local.PendingDeleteEntity
import com.walkspot.app.data.sync.RemoteTables
import com.walkspot.app.data.sync.SyncScheduler
import com.walkspot.app.data.walk.WalkRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    /** Removes the find and its photo from the phone at once, from the server with the next sync. */
    suspend fun delete(id: String) {
        val find = drafted(id) ?: finds.get(id) ?: return
        if (drafted(id) != null) {
            recorder.removeFind(id)
        } else {
            finds.delete(id)
            deletes.add(PendingDeleteEntity(FINDS_TABLE, id))
            sync.pushSoon()
        }
        photos.delete(find.photoPath)
    }
}
