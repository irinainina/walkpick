package com.walkspot.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.walkspot.app.data.local.PendingDeleteEntity
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

private const val TAG = "SyncCheck"

/**
 * Runs the sync on a phone without any screen:
 * adb shell am broadcast -n com.walkspot.app/.debug.SyncCheckReceiver --es cmd <command>
 * Commands: signin, seed (the made-up history of SeedData), push, remove, wipe, restore, dump.
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
                val walk = database.walks().unsynced().firstOrNull()?.id ?: seedData().walks.last().id
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

    // Rows already there are replaced: the made-up history can be written again after a change to it.
    private suspend fun seed(database: WalkSpotDatabase) {
        val data = seedData()
        database.dailySteps().save(data.days)
        database.walks().save(data.walks)
        database.finds().save(data.finds)
        database.healthEntries().restore(data.health)
        Log.i(TAG, "seeded ${data.days.size} days, ${data.walks.size} walks, ${data.finds.size} finds")
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
}
