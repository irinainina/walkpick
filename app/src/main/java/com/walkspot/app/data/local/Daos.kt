package com.walkspot.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import java.time.Instant
import java.time.LocalDate

interface SyncedDao<T> {
    @Upsert
    suspend fun save(rows: List<T>)

    // Rows from the server never replace what the phone already holds.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restore(rows: List<T>)
}

// markSynced names updatedAt too: a row changed while it was being sent stays unsynced.

@Dao
interface DailyStepsDao : SyncedDao<DailyStepsEntity> {
    @Query("SELECT * FROM daily_steps WHERE synced = 0")
    suspend fun unsynced(): List<DailyStepsEntity>

    @Query("UPDATE daily_steps SET synced = 1 WHERE day = :day AND updatedAt = :updatedAt")
    suspend fun markSynced(day: LocalDate, updatedAt: Instant)
}

@Dao
interface WalkDao : SyncedDao<WalkEntity> {
    @Query("SELECT * FROM walks WHERE synced = 0")
    suspend fun unsynced(): List<WalkEntity>

    @Query("UPDATE walks SET synced = 1 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Instant)
}

@Dao
interface FindDao : SyncedDao<FindEntity> {
    @Query("SELECT * FROM finds WHERE synced = 0")
    suspend fun unsynced(): List<FindEntity>

    @Query("UPDATE finds SET synced = 1 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Instant)
}

@Dao
interface HealthEntryDao : SyncedDao<HealthEntryEntity> {
    @Query("SELECT * FROM health_entries WHERE synced = 0")
    suspend fun unsynced(): List<HealthEntryEntity>

    @Query("UPDATE health_entries SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface PendingDeleteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(row: PendingDeleteEntity)

    @Query("SELECT * FROM pending_deletes")
    suspend fun all(): List<PendingDeleteEntity>

    @Delete
    suspend fun remove(row: PendingDeleteEntity)
}
