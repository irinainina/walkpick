package com.walkspot.app.data.find

import android.content.Context
import com.walkspot.app.data.sync.RemoteTables
import com.walkspot.app.data.sync.SyncedPart
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

private const val BUCKET = "photos"
private const val UNSENT = ".unsent"

/**
 * The photos of finds on the phone, under the paths they have in the bucket of the server. A photo not
 * sent yet has an empty file beside it; the sync sends such photos and removes the marks.
 */
class PhotoStore(private val folder: File) : SyncedPart {

    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, "photos"))

    fun file(path: String) = File(folder, path)

    fun keep(path: String, write: (OutputStream) -> Unit) {
        val photo = file(path)
        photo.parentFile?.mkdirs()
        photo.outputStream().use(write)
        File(folder, path + UNSENT).createNewFile()
    }

    fun unsent(): List<String> = folder.walkTopDown()
        .filter { it.name.endsWith(UNSENT) }
        .map { it.relativeTo(folder).invariantSeparatorsPath.removeSuffix(UNSENT) }
        .toList()

    override suspend fun push(remote: RemoteTables) {
        unsent().forEach { path ->
            remote.upload(BUCKET, path, file(path).readBytes())
            File(folder, path + UNSENT).delete()
        }
    }

    // A photo is fetched when a screen shows it, not all of them in advance.
    override suspend fun pull(remote: RemoteTables) = Unit
}
