package com.walkpick.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.walkpick.app.data.account.AuthRepository
import com.walkpick.app.data.account.SupabaseAuthRepository
import com.walkpick.app.data.find.FindIdentifier
import com.walkpick.app.data.find.PhotoStore
import com.walkpick.app.data.local.DailyStepsDao
import com.walkpick.app.data.local.FindDao
import com.walkpick.app.data.local.HealthEntryDao
import com.walkpick.app.data.local.PendingDeleteDao
import com.walkpick.app.data.local.WalkDao
import com.walkpick.app.data.local.WalkPickDatabase
import com.walkpick.app.data.profile.ProfileSync
import com.walkpick.app.data.settings.SettingsSync
import com.walkpick.app.data.sync.RemoteTables
import com.walkpick.app.data.sync.SupabaseRemoteTables
import com.walkpick.app.data.sync.SyncedPart
import com.walkpick.app.data.sync.syncedTables
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val FINDS_TABLE = "finds"

// The one settings file: two instances over it would overwrite each other.
private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun settingsStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsStore

    // One instance: Room caches rows per instance.
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): WalkPickDatabase =
        Room.databaseBuilder(context, WalkPickDatabase::class.java, WalkPickDatabase.NAME).build()

    @Provides
    fun pendingDeletes(database: WalkPickDatabase): PendingDeleteDao = database.pendingDeletes()

    @Provides
    fun dailySteps(database: WalkPickDatabase): DailyStepsDao = database.dailySteps()

    @Provides
    fun walks(database: WalkPickDatabase): WalkDao = database.walks()

    @Provides
    fun finds(database: WalkPickDatabase): FindDao = database.finds()

    @Provides
    fun healthEntries(database: WalkPickDatabase): HealthEntryDao = database.healthEntries()

    @Provides
    fun synced(
        database: WalkPickDatabase,
        profile: ProfileSync,
        settings: SettingsSync,
        photos: PhotoStore,
        identifier: FindIdentifier,
    ): List<@JvmSuppressWildcards SyncedPart> {
        val tables = syncedTables(database)
        // The finds go up twice: before their photos, and again with the names the server gave them.
        val finds = tables.first { it.name == FINDS_TABLE }
        return tables + profile + settings + photos + identifier + finds
    }

    @Provides
    fun remoteTables(remote: SupabaseRemoteTables): RemoteTables = remote

    @Provides
    fun auth(repository: SupabaseAuthRepository): AuthRepository = repository
}
