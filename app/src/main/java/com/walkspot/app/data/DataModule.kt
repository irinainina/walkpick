package com.walkspot.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.walkspot.app.data.account.AuthRepository
import com.walkspot.app.data.account.SupabaseAuthRepository
import com.walkspot.app.data.local.DailyStepsDao
import com.walkspot.app.data.local.PendingDeleteDao
import com.walkspot.app.data.local.WalkSpotDatabase
import com.walkspot.app.data.sync.RemoteTables
import com.walkspot.app.data.sync.SupabaseRemoteTables
import com.walkspot.app.data.sync.SyncedTable
import com.walkspot.app.data.sync.syncedTables
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
    fun database(@ApplicationContext context: Context): WalkSpotDatabase =
        Room.databaseBuilder(context, WalkSpotDatabase::class.java, WalkSpotDatabase.NAME).build()

    @Provides
    fun pendingDeletes(database: WalkSpotDatabase): PendingDeleteDao = database.pendingDeletes()

    @Provides
    fun dailySteps(database: WalkSpotDatabase): DailyStepsDao = database.dailySteps()

    @Provides
    fun tables(database: WalkSpotDatabase): List<@JvmSuppressWildcards SyncedTable<*>> =
        syncedTables(database)

    @Provides
    fun remoteTables(remote: SupabaseRemoteTables): RemoteTables = remote

    @Provides
    fun auth(repository: SupabaseAuthRepository): AuthRepository = repository
}
