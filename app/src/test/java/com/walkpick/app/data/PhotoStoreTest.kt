package com.walkpick.app.data

import com.walkpick.app.data.find.Guess
import com.walkpick.app.data.find.PhotoStore
import com.walkpick.app.data.sync.RemoteTables
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PhotoStoreTest {

    private class Bucket : RemoteTables {
        val uploads = mutableListOf<String>()
        var failing = false
        var fetched: ByteArray? = null

        override suspend fun isSignedIn() = true

        override suspend fun upsert(table: String, rows: List<JsonObject>, key: String, insertOnly: Boolean) {
            uploads += "upsert $table"
        }

        override suspend fun page(table: String, key: String, from: Long, to: Long) = emptyList<JsonObject>()

        override suspend fun delete(table: String, id: String) = Unit

        override suspend fun upload(bucket: String, path: String, jpeg: ByteArray) {
            check(!failing) { "no network" }
            uploads += "$bucket $path ${jpeg.size}"
        }

        override suspend fun remove(bucket: String, path: String) = Unit

        override suspend fun download(bucket: String, path: String): ByteArray? = fetched

        override suspend fun identify(photoPath: String): List<Guess>? = null
    }

    @Test
    fun aKeptPhotoIsSentOnceUnderItsPath(@TempDir folder: File) = runTest {
        val photos = PhotoStore(folder)
        val bucket = Bucket()
        photos.keep("user-1/find-1.jpg") { it.write(ByteArray(3)) }

        assertEquals(listOf("user-1/find-1.jpg"), photos.unsent())
        photos.push(bucket)
        photos.push(bucket)

        assertEquals(listOf("photos user-1/find-1.jpg 3"), bucket.uploads)
        assertTrue(photos.file("user-1/find-1.jpg").exists())
    }

    @Test
    fun aPhotoThisPhoneDoesNotHaveIsFetchedOnceAndADeletedOneIsGone(@TempDir folder: File) = runTest {
        val photos = PhotoStore(folder)
        val bucket = Bucket()

        assertEquals(null, photos.local("user-1/find-2.jpg", bucket))
        bucket.fetched = ByteArray(5)
        assertEquals(5L, photos.local("user-1/find-2.jpg", bucket)?.length())
        bucket.fetched = null
        assertEquals(5L, photos.local("user-1/find-2.jpg", bucket)?.length())

        photos.delete("user-1/find-2.jpg")
        assertEquals(null, photos.local("user-1/find-2.jpg", bucket))
    }

    @Test
    fun aPhotoThatCouldNotBeSentWaitsForTheNextSync(@TempDir folder: File) = runTest {
        val photos = PhotoStore(folder)
        val bucket = Bucket().apply { failing = true }
        photos.keep("user-1/find-1.jpg") { it.write(ByteArray(3)) }

        assertThrows(IllegalStateException::class.java) { runBlocking { photos.push(bucket) } }
        assertEquals(listOf("user-1/find-1.jpg"), photos.unsent())

        bucket.failing = false
        photos.push(bucket)
        assertEquals(emptyList<String>(), photos.unsent())
    }

    @Test
    fun aDeletedAccountLeavesNoPhotosOnThePhone(@TempDir folder: File) {
        val photos = PhotoStore(File(folder, "photos"))
        photos.keep("user-1/find-1.jpg") { it.write(ByteArray(3)) }

        photos.clear()
        assertEquals(emptyList<String>(), folder.list()?.toList())
    }
}
