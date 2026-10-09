package com.walkspot.app.data.sync

import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.HealthEntryEntity
import com.walkspot.app.data.local.SyncJson
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.local.WalkSpotDatabase
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.jsonObject

// One request per row would be a request storm.
private const val CHUNK = 100

// PostgREST answers with at most a thousand rows.
private const val PAGE = 1000L

/** Something kept on the phone and copied to the server and back. */
interface SyncedPart {
    suspend fun push(remote: RemoteTables)

    suspend fun pull(remote: RemoteTables)
}

// One table kept on the phone and copied to the server under the same name.
class SyncedTable<T : Any>(
    val name: String,
    // the columns that tell one row from another, comma-separated
    private val key: String,
    private val serializer: KSerializer<T>,
    private val insertOnly: Boolean = false,
    private val unsynced: suspend () -> List<T>,
    private val markSynced: suspend (T) -> Unit,
    // takes rows as the server sent them and stores them as synced
    private val restore: suspend (List<T>) -> Unit,
) : SyncedPart {
    override suspend fun push(remote: RemoteTables) {
        unsynced().chunked(CHUNK).forEach { send(it, remote) }
    }

    // A row the server will never take stays unsent and must not hold back the rows beside it.
    private suspend fun send(rows: List<T>, remote: RemoteTables) {
        try {
            val json = rows.map { SyncJson.encodeToJsonElement(serializer, it).jsonObject }
            remote.upsert(name, json, key, insertOnly)
            rows.forEach { markSynced(it) }
        } catch (_: RowsRefused) {
            if (rows.size > 1) rows.forEach { send(listOf(it), remote) }
        }
    }

    override suspend fun pull(remote: RemoteTables) {
        var from = 0L
        do {
            val page = remote.page(name, key, from, from + PAGE - 1)
            restore(page.map { SyncJson.decodeFromJsonElement(serializer, it) })
            from += PAGE
        } while (page.size >= PAGE)
    }
}

// Walks stand before finds: a find points at its walk.
fun syncedTables(database: WalkSpotDatabase): List<SyncedTable<*>> = listOf(
    database.dailySteps().let { dao ->
        SyncedTable(
            name = "daily_steps",
            key = "user_id,day",
            serializer = DailyStepsEntity.serializer(),
            unsynced = dao::unsynced,
            markSynced = { dao.markSynced(it.day, it.updatedAt) },
            restore = { rows -> dao.restore(rows.map { it.copy(synced = true) }) },
        )
    },
    database.walks().let { dao ->
        SyncedTable(
            name = "walks",
            key = "id",
            serializer = WalkEntity.serializer(),
            unsynced = dao::unsynced,
            markSynced = { dao.markSynced(it.id, it.updatedAt) },
            restore = { rows -> dao.restore(rows.map { it.copy(synced = true) }) },
        )
    },
    database.finds().let { dao ->
        SyncedTable(
            name = "finds",
            key = "id",
            serializer = FindEntity.serializer(),
            unsynced = dao::unsynced,
            markSynced = { dao.markSynced(it.id, it.updatedAt) },
            restore = { rows -> dao.restore(rows.map { it.copy(synced = true) }) },
        )
    },
    database.healthEntries().let { dao ->
        SyncedTable(
            name = "health_entries",
            key = "id",
            serializer = HealthEntryEntity.serializer(),
            insertOnly = true,
            unsynced = dao::unsynced,
            markSynced = { dao.markSynced(it.id) },
            restore = { rows -> dao.restore(rows.map { it.copy(synced = true) }) },
        )
    },
)
