package com.walkpick.app.data.sync

import com.walkpick.app.data.find.Guess
import com.walkpick.app.data.local.SyncJson
import com.walkpick.app.data.remote.SupabaseProvider
import com.walkpick.app.data.remote.signedInSession
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException
import javax.inject.Inject

private const val IDENTIFY = "identify"

/** The bucket of the photos; a photo to remove waits among the pending deletes under this name. */
const val PHOTO_BUCKET = "photos"

// What PostgREST answers to rows that break a rule of the table: a check, a unique pair, a missing parent.
private val DataRefusals = setOf(400, 409, 422)

/** The server read the rows and will not take them, now or later; a lost network is not this. */
class RowsRefused(cause: Throwable) : Exception(cause)

@Serializable
private data class Identified(val guesses: List<Guess> = emptyList())

// The server as the sync sees it: rows of a table by name. A test puts a fake here.
interface RemoteTables {
    suspend fun isSignedIn(): Boolean

    suspend fun upsert(table: String, rows: List<JsonObject>, key: String, insertOnly: Boolean)

    suspend fun page(table: String, key: String, from: Long, to: Long): List<JsonObject>

    suspend fun delete(table: String, id: String)

    /** Puts a JPEG into a bucket; a file already there is left as it is. */
    suspend fun upload(bucket: String, path: String, jpeg: ByteArray)

    /** Takes a file out of a bucket; one that is not there is no error. */
    suspend fun remove(bucket: String, path: String)

    /** A file of a bucket; null when it is not there or the server cannot be asked. */
    suspend fun download(bucket: String, path: String): ByteArray?

    /** The plant on a photo already sent to the bucket; null when the service gave no answer. */
    suspend fun identify(photoPath: String): List<Guess>?
}

class SupabaseRemoteTables @Inject constructor(
    private val supabase: SupabaseProvider,
) : RemoteTables {

    override suspend fun isSignedIn(): Boolean =
        supabase.isConfigured && supabase.client.signedInSession() != null

    override suspend fun upsert(table: String, rows: List<JsonObject>, key: String, insertOnly: Boolean) {
        try {
            supabase.client.from(table).upsert(rows) {
                onConflict = key
                ignoreDuplicates = insertOnly
            }
        } catch (refused: RestException) {
            throw if (refused.statusCode in DataRefusals) RowsRefused(refused) else refused
        }
    }

    override suspend fun page(table: String, key: String, from: Long, to: Long): List<JsonObject> =
        supabase.client.from(table).select {
            key.split(',').forEach { order(it, Order.ASCENDING) }
            range(from, to)
        }.decodeList()

    override suspend fun delete(table: String, id: String) {
        supabase.client.from(table).delete { filter { eq("id", id) } }
    }

    // The library has an error of its own for a lost network; the callers know IOException.
    override suspend fun upload(bucket: String, path: String, jpeg: ByteArray) {
        val files = supabase.client.storage.from(bucket)
        try {
            if (!files.exists(path)) files.upload(path, jpeg) { contentType = ContentType.Image.JPEG }
        } catch (lost: HttpRequestException) {
            throw IOException(lost)
        }
    }

    override suspend fun remove(bucket: String, path: String) {
        supabase.client.storage.from(bucket).delete(listOf(path))
    }

    override suspend fun download(bucket: String, path: String): ByteArray? = try {
        supabase.client.storage.from(bucket).downloadAuthenticated(path)
    } catch (_: RestException) {
        null
    } catch (_: HttpRequestException) {
        null
    }

    override suspend fun identify(photoPath: String): List<Guess>? = try {
        val asked = JsonObject(mapOf("photo_path" to JsonPrimitive(photoPath)))
        val reply = supabase.client.functions.invoke(IDENTIFY, asked)
        SyncJson.decodeFromString<Identified>(reply.bodyAsText()).guesses
    } catch (_: RestException) {
        null
    } catch (lost: HttpRequestException) {
        throw IOException(lost)
    }
}
