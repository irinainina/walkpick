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

    @Query("SELECT COALESCE(MAX(number), 0) FROM walks")
    suspend fun lastNumber(): Int

    @Query("SELECT * FROM walks WHERE id = :id")
    fun watch(id: String): Flow<WalkEntity?>

    @Query("SELECT * FROM walks ORDER BY startedAt DESC")
    fun watchAll(): Flow<List<WalkEntity>>

    @Query("UPDATE walks SET note = :note, updatedAt = :now, synced = 0 WHERE id = :id")
    suspend fun setNote(id: String, note: String?, now: Instant)

    @Query("DELETE FROM walks WHERE id = :id")
    suspend fun delete(id: String)
}

/** How many photos a walk has; every find is a photo. */
data class WalkPhotos(val walkId: String, val photos: Int)

@Dao
interface FindDao : SyncedDao<FindEntity> {
    @Query("SELECT * FROM finds WHERE synced = 0")
    suspend fun unsynced(): List<FindEntity>

    @Query("UPDATE finds SET synced = 1 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Instant)

    @Query("SELECT * FROM finds WHERE walkId = :walkId ORDER BY takenAt")
    fun watchOf(walkId: String): Flow<List<FindEntity>>

    @Query("SELECT COUNT(*) FROM finds")
    fun watchCount(): Flow<Int>

    @Query("SELECT walkId, COUNT(*) AS photos FROM finds GROUP BY walkId")
    fun watchPhotoCounts(): Flow<List<WalkPhotos>>
}

@Dao
interface HealthEntryDao : SyncedDao<HealthEntryEntity> {
    @Query("SELECT * FROM health_entries WHERE synced = 0")
    suspend fun unsynced(): List<HealthEntryEntity>

    @Query("UPDATE health_entries SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT value FROM health_entries WHERE kind = :kind ORDER BY measuredAt DESC LIMIT 1")
    fun watchLatest(kind: HealthKind): Flow<Double?>

    @Query("SELECT * FROM health_entries WHERE kind = :kind ORDER BY measuredAt")
    fun watch(kind: HealthKind): Flow<List<HealthEntryEntity>>
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
