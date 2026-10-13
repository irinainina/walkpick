package com.walkpick.app.data.find

import android.content.Context
import com.walkpick.app.data.sync.PHOTO_BUCKET
import com.walkpick.app.data.sync.RemoteTables
import com.walkpick.app.data.sync.SyncedPart
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

private const val UNSENT = ".unsent"

/**
 * The photos of finds on the phone, under the paths they have in the bucket of the server. A photo not
 * sent yet has an empty file beside it; the sync sends such photos and removes the marks.
 */
class PhotoStore(private val folder: File) : SyncedPart {

    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, "photos"))

    fun file(path: String) = File(folder, path)

    private fun mark(path: String) = File(folder, path + UNSENT)

    fun keep(path: String, write: (OutputStream) -> Unit) {
        val photo = file(path)
        photo.parentFile?.mkdirs()
        photo.outputStream().use(write)
        mark(path).createNewFile()
    }

    fun delete(path: String) {
        file(path).delete()
        mark(path).delete()
    }

    /** Every photo goes: the phone has changed hands or the account is gone. */
    fun clear() {
        folder.deleteRecursively()
    }

    fun unsent(): List<String> = folder.walkTopDown()
        .filter { it.name.endsWith(UNSENT) }
        .map { it.relativeTo(folder).invariantSeparatorsPath.removeSuffix(UNSENT) }
        .toList()

    /** Sends the photo unless it was sent already. */
    suspend fun send(path: String, remote: RemoteTables) {
        if (!mark(path).exists()) return
        remote.upload(PHOTO_BUCKET, path, withContext(Dispatchers.IO) { file(path).readBytes() })
        mark(path).delete()
    }

    /**
     * The photo on the phone. A phone that never took it, as after signing in on a new one, fetches it
     * from the server once; null when it is in neither place or the server cannot be asked.
     */
    suspend fun local(path: String, remote: RemoteTables): File? {
        val photo = file(path)
        if (photo.exists()) return photo
        val bytes = remote.download(PHOTO_BUCKET, path) ?: return null
        return withContext(Dispatchers.IO) {
            photo.parentFile?.mkdirs()
            photo.writeBytes(bytes)
            photo
        }
    }

    override suspend fun push(remote: RemoteTables) = unsent().forEach { send(it, remote) }

    // A photo is fetched when a screen shows it, not all of them in advance.
    override suspend fun pull(remote: RemoteTables) = Unit
}
