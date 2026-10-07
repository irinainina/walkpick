package com.walkspot.app.data.sync

import com.walkspot.app.data.find.Guess
import com.walkspot.app.data.local.SyncJson
import com.walkspot.app.data.remote.SupabaseProvider
import com.walkspot.app.data.remote.signedInSession
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

    /** What is on the photo of a find; null when the service gave no answer. */
    suspend fun identify(findId: String): List<Guess>?
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

    override suspend fun upload(bucket: String, path: String, jpeg: ByteArray) {
        val files = supabase.client.storage.from(bucket)
        if (!files.exists(path)) files.upload(path, jpeg) { contentType = ContentType.Image.JPEG }
    }

    override suspend fun identify(findId: String): List<Guess>? = try {
        val asked = JsonObject(mapOf("find_id" to JsonPrimitive(findId)))
        val reply = supabase.client.functions.invoke(IDENTIFY, asked)
        SyncJson.decodeFromString<Identified>(reply.bodyAsText()).guesses
    } catch (_: RestException) {
        null
    }
}
