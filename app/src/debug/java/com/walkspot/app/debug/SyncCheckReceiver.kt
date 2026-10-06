package com.walkspot.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.HealthEntryEntity
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.local.PendingDeleteEntity
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.local.WalkSpotDatabase
import com.walkspot.app.data.remote.SupabaseProvider
import com.walkspot.app.data.sync.SyncScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private const val TAG = "SyncCheck"

/**
 * Runs the sync on a phone without any screen:
 * adb shell am broadcast -n com.walkspot.app/.debug.SyncCheckReceiver --es cmd <command>
 * Commands: signin, seed, push, remove, wipe, restore, dump.
 * signin also takes --es email and --es password of a test account.
 */
class SyncCheckReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Graph {
        fun supabase(): SupabaseProvider
        fun database(): WalkSpotDatabase
        fun scheduler(): SyncScheduler
    }

    // The point of the check is to print why a step did not work.
    @Suppress("TooGenericExceptionCaught")
    override fun onReceive(context: Context, intent: Intent) {
        val graph = EntryPointAccessors.fromApplication(context.applicationContext, Graph::class.java)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                run(graph, intent)
            } catch (e: Exception) {
                Log.e(TAG, "${intent.getStringExtra("cmd")} failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun run(graph: Graph, intent: Intent) {
        val database = graph.database()
        when (intent.getStringExtra("cmd")) {
            "signin" -> {
                val auth = graph.supabase().client.auth
                auth.signInWith(Email) {
                    email = intent.getStringExtra("email").orEmpty()
                    password = intent.getStringExtra("password").orEmpty()
                }
                Log.i(TAG, "signed in as ${auth.currentUserOrNull()?.id}")
            }
            "seed" -> {
                seed(database)
                graph.scheduler().pushSoon()
                Log.i(TAG, "seeded, push queued")
            }
            "push" -> {
                graph.scheduler().pushSoon()
                Log.i(TAG, "push queued")
            }
            "remove" -> {
                val walk = database.walks().unsynced().firstOrNull()?.id ?: SEEDED_WALK
                database.openHelper.writableDatabase.execSQL("DELETE FROM walks WHERE id = ?", arrayOf(walk))
                database.pendingDeletes().add(PendingDeleteEntity("walks", walk))
                graph.scheduler().pushSoon()
                Log.i(TAG, "walk $walk removed, push queued")
            }
            "wipe" -> {
                database.clearAllTables()
                Log.i(TAG, "wiped")
            }
            "restore" -> {
                graph.scheduler().restoreFromCloud()
                Log.i(TAG, "restore queued")
            }
            else -> dump(database)
        }
    }

    private suspend fun seed(database: WalkSpotDatabase) {
        val now = Instant.now()
        database.dailySteps().save(listOf(DailyStepsEntity(LocalDate.now(), 5230, 3900, 48, 10000, now)))
        database.walks().save(
            listOf(
                WalkEntity(
                    id = SEEDED_WALK,
                    number = 1,
                    startedAt = now.minusSeconds(2880),
                    endedAt = now,
                    durationS = 2880,
                    steps = 5230,
                    distanceM = 3900,
                    route = "27.56 53.9,27.57 53.91,27.56 53.9",
                    updatedAt = now,
                ),
            ),
        )
        database.finds().save(
            listOf(
                FindEntity(
                    id = UUID.randomUUID().toString(),
                    walkId = SEEDED_WALK,
                    takenAt = now,
                    latitude = 53.91,
                    longitude = 27.57,
                    photoPath = "check/none.jpg",
                    updatedAt = now,
                ),
            ),
        )
        database.healthEntries().save(
            listOf(
                HealthEntryEntity(UUID.randomUUID().toString(), HealthKind.PRESSURE, now, 122.0, 78),
                HealthEntryEntity(UUID.randomUUID().toString(), HealthKind.MOOD, now, 4.0),
            ),
        )
    }

    private fun dump(database: WalkSpotDatabase) {
        val reader = database.openHelper.readableDatabase
        listOf("daily_steps", "walks", "finds", "health_entries").forEach { table ->
            reader.query("SELECT COUNT(*), COALESCE(SUM(synced), 0) FROM $table").use {
                it.moveToFirst()
                Log.i(TAG, "$table: ${it.getInt(0)} rows, ${it.getInt(1)} synced")
            }
        }
        reader.query("SELECT COUNT(*) FROM pending_deletes").use {
            it.moveToFirst()
            Log.i(TAG, "pending_deletes: ${it.getInt(0)}")
        }
        reader.query("SELECT route FROM walks LIMIT 1").use {
            if (it.moveToFirst()) Log.i(TAG, "route: ${it.getString(0)}")
        }
    }

    private companion object {
        const val SEEDED_WALK = "00000000-0000-4000-8000-0000000000c1"
    }
}
