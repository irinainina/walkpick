package com.walkspot.app.data.sync

import com.walkspot.app.data.find.Guess
import com.walkspot.app.data.local.SyncJson
import com.walkspot.app.data.remote.SupabaseProvider
import com.walkspot.app.data.remote.signedInSession
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
        supabase.client.from(table).upsert(rows) {
            onConflict = key
            ignoreDuplicates = insertOnly
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
