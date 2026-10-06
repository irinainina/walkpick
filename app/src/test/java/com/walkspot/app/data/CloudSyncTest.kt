package com.walkspot.app.data

import com.walkspot.app.data.local.HealthEntryEntity
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.local.PendingDeleteDao
import com.walkspot.app.data.local.PendingDeleteEntity
import com.walkspot.app.data.sync.CloudSync
import com.walkspot.app.data.sync.RemoteTables
import com.walkspot.app.data.sync.SyncedTable
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class CloudSyncTest {

    private class FakeRemote(var signedIn: Boolean = true) : RemoteTables {
        val calls = mutableListOf<String>()
        val rows = mutableMapOf<String, MutableList<JsonObject>>()
        var failing = false

        override suspend fun isSignedIn() = signedIn

        override suspend fun upsert(table: String, rows: List<JsonObject>, key: String, insertOnly: Boolean) {
            check(!failing) { "no network" }
            calls += "upsert $table ${rows.size}"
            this.rows.getOrPut(table) { mutableListOf() } += rows
        }

        override suspend fun page(table: String, key: String, from: Long, to: Long): List<JsonObject> {
            calls += "page $table $from"
            return rows[table].orEmpty().drop(from.toInt()).take((to - from + 1).toInt())
        }

        override suspend fun delete(table: String, id: String) {
            calls += "delete $table $id"
        }
    }

    private class FakeDeletes(vararg rows: PendingDeleteEntity) : PendingDeleteDao {
        val rows = rows.toMutableList()
        override suspend fun add(row: PendingDeleteEntity) { rows += row }
        override suspend fun all() = rows.toList()
        override suspend fun remove(row: PendingDeleteEntity) { rows -= row }
    }

    private class Phone(name: String, count: Int = 0) {
        val rows = MutableList(count) { entry("local-$it") }
        val table = SyncedTable(
            name = name,
            key = "id",
            serializer = HealthEntryEntity.serializer(),
            unsynced = { rows.filterNot { it.synced } },
            markSynced = { sent -> rows.replaceAll { if (it.id == sent.id) it.copy(synced = true) else it } },
            restore = { server -> rows += server.map { it.copy(synced = true) } },
        )
    }

    @Test
    fun nothingLeavesThePhoneWhenNobodyIsSignedIn() = runTest {
        val remote = FakeRemote(signedIn = false)
        val phone = Phone("walks", count = 2)

        CloudSync(remote, listOf(phone.table), FakeDeletes(PendingDeleteEntity("walks", "w1"))).push()

        assertTrue(remote.calls.isEmpty())
        assertTrue(phone.rows.none { it.synced })
    }

    @Test
    fun removalsGoFirstThenTablesInOrderAndRowsAreMarked() = runTest {
        val remote = FakeRemote()
        val walks = Phone("walks", count = 1)
        val finds = Phone("finds", count = 250)
        val deletes = FakeDeletes(PendingDeleteEntity("walks", "w1"))

        CloudSync(remote, listOf(walks.table, finds.table), deletes).push()

        assertEquals(
            listOf(
                "delete walks w1",
                "upsert walks 1",
                "upsert finds 100",
                "upsert finds 100",
                "upsert finds 50",
            ),
            remote.calls,
        )
        assertTrue(deletes.rows.isEmpty())
        assertTrue((walks.rows + finds.rows).all { it.synced })
    }

    @Test
    fun aSecondPushSendsNothing() = runTest {
        val remote = FakeRemote()
        val sync = CloudSync(remote, listOf(Phone("walks", count = 3).table), FakeDeletes())

        sync.push()
        remote.calls.clear()
        sync.push()

        assertTrue(remote.calls.isEmpty())
    }

    @Test
    fun aFailedPushLeavesRowsToBeSentAgain() = runTest {
        val remote = FakeRemote().apply { failing = true }
        val phone = Phone("walks", count = 2)
        val sync = CloudSync(remote, listOf(phone.table), FakeDeletes())

        assertTrue(runCatching { sync.push() }.isFailure)
        assertTrue(phone.rows.none { it.synced })

        remote.failing = false
        sync.push()
        assertTrue(phone.rows.all { it.synced })
    }

    @Test
    fun restoreReadsEveryPageAndStoresRowsAsSynced() = runTest {
        val remote = FakeRemote()
        remote.rows["walks"] = MutableList(1001) { serverRow("server-$it") }
        val phone = Phone("walks")

        CloudSync(remote, listOf(phone.table), FakeDeletes()).restore()

        assertEquals(listOf("page walks 0", "page walks 1000"), remote.calls)
        assertEquals(1001, phone.rows.size)
        assertTrue(phone.rows.all { it.synced })
    }

    @Test
    fun rowsGatheredBeforeSigningInAreSentRightAfterTheRestore() = runTest {
        val remote = FakeRemote()
        val phone = Phone("walks", count = 2)

        CloudSync(remote, listOf(phone.table), FakeDeletes()).restore()

        assertEquals(listOf("page walks 0", "upsert walks 2"), remote.calls)
        assertTrue(phone.rows.all { it.synced })
    }

    private companion object {
        fun entry(id: String) =
            HealthEntryEntity(id, HealthKind.WEIGHT, Instant.parse("2026-10-06T08:00:00Z"), 64.2)

        fun serverRow(id: String) = JsonObject(
            mapOf(
                "id" to JsonPrimitive(id),
                "user_id" to JsonPrimitive("someone"),
                "kind" to JsonPrimitive("weight"),
                "measured_at" to JsonPrimitive("2026-10-06T08:00:00+00:00"),
                "value" to JsonPrimitive(64.2),
                "diastolic" to JsonNull,
            ),
        )
    }
}
