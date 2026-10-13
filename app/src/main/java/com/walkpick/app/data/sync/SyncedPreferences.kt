package com.walkpick.app.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * One value kept in the settings file and copied to the server: the profile, the app settings.
 * Two flags stand beside it: changed on this phone at all, and sent since the last change.
 */
abstract class SyncedPreferences<T>(
    private val store: DataStore<Preferences>,
    name: String,
) {
    private val edited = booleanPreferencesKey("${name}_edited")
    private val synced = booleanPreferencesKey("${name}_synced")

    protected abstract fun read(saved: Preferences): T

    protected abstract fun write(saved: MutablePreferences, value: T)

    val value: Flow<T> = store.data.map { read(it) }

    suspend fun edited(): Boolean = store.data.first()[edited] ?: false

    suspend fun unsynced(): T? = store.data.first().let { saved ->
        read(saved).takeIf { saved[edited] == true && saved[synced] != true }
    }

    suspend fun change(transform: (T) -> T) {
        store.edit {
            write(it, transform(read(it)))
            it[edited] = true
            it[synced] = false
        }
    }

    /** What the server holds, taken for a phone that has just been signed in on. */
    suspend fun restore(transform: (T) -> T) {
        store.edit {
            write(it, transform(read(it)))
            it[synced] = true
        }
    }

    // A value changed while it was being sent stays unsynced.
    suspend fun markSynced(sent: T) {
        store.edit { if (read(it) == sent) it[synced] = true }
    }

    protected fun <V> MutablePreferences.put(key: Preferences.Key<V>, value: V?) {
        if (value == null) remove(key) else this[key] = value
    }
}
