package com.walkspot.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
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

    @Query("SELECT * FROM daily_steps WHERE day >= :from ORDER BY day")
    suspend fun since(from: LocalDate): List<DailyStepsEntity>

    @Query("SELECT * FROM daily_steps ORDER BY day")
    fun watchAll(): Flow<List<DailyStepsEntity>>

    @Query("UPDATE daily_steps SET goal = :goal, updatedAt = :now, synced = 0 WHERE day = :day")
    suspend fun setGoal(day: LocalDate, goal: Int, now: Instant)
}

@Dao
interface WalkDao : SyncedDao<WalkEntity> {
    @Query("SELECT * FROM walks WHERE synced = 0")
    suspend fun unsynced(): List<WalkEntity>

    @Query("UPDATE walks SET synced = 1 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Instant)

    @Query("SELECT startedAt FROM walks")
    fun watchStarts(): Flow<List<Instant>>
}

@Dao
interface FindDao : SyncedDao<FindEntity> {
    @Query("SELECT * FROM finds WHERE synced = 0")
    suspend fun unsynced(): List<FindEntity>

    @Query("UPDATE finds SET synced = 1 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Instant)

    @Query("SELECT COUNT(*) FROM finds")
    fun watchCount(): Flow<Int>
}

@Dao
interface HealthEntryDao : SyncedDao<HealthEntryEntity> {
    @Query("SELECT * FROM health_entries WHERE synced = 0")
    suspend fun unsynced(): List<HealthEntryEntity>

    @Query("UPDATE health_entries SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT value FROM health_entries WHERE kind = :kind ORDER BY measuredAt DESC LIMIT 1")
    fun watchLatest(kind: HealthKind): Flow<Double?>
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
