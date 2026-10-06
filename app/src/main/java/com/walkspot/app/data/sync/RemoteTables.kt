package com.walkspot.app.data.sync

import com.walkspot.app.data.remote.SupabaseProvider
import com.walkspot.app.data.remote.signedInSession
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

// The server as the sync sees it: rows of a table by name. A test puts a fake here.
interface RemoteTables {
    suspend fun isSignedIn(): Boolean

    suspend fun upsert(table: String, rows: List<JsonObject>, key: String, insertOnly: Boolean)

    suspend fun page(table: String, key: String, from: Long, to: Long): List<JsonObject>

    suspend fun delete(table: String, id: String)
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
}
