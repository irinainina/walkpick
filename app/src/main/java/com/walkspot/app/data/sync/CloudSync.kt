package com.walkspot.app.data.sync

import com.walkspot.app.data.local.PendingDeleteDao
import javax.inject.Inject

// Everything is written to the phone first; this carries it to the server and back.
class CloudSync @Inject constructor(
    private val remote: RemoteTables,
    private val tables: List<@JvmSuppressWildcards SyncedPart>,
    private val deletes: PendingDeleteDao,
) {
    suspend fun push() {
        if (!remote.isSignedIn()) return
        // Removals go first: a walk removed on the server takes its finds with it.
        deletes.all().forEach {
            when (it.remoteTable) {
                PHOTO_BUCKET -> remote.remove(PHOTO_BUCKET, it.id)
                else -> remote.delete(it.remoteTable, it.id)
            }
            deletes.remove(it)
        }
        tables.forEach { it.push(remote) }
    }

    // For a phone that has just been signed in on; what it gathered before that goes up at once.
    suspend fun restore() {
        if (!remote.isSignedIn()) return
        tables.forEach { it.pull(remote) }
        push()
    }
}
