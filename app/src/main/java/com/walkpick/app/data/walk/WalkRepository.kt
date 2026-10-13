package com.walkpick.app.data.walk

import androidx.room.withTransaction
import com.walkpick.app.data.find.PhotoStore
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.HealthEntryEntity
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.local.PendingDeleteEntity
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.local.WalkPickDatabase
import com.walkpick.app.data.steps.StepsRepository
import com.walkpick.app.data.sync.PHOTO_BUCKET
import com.walkpick.app.data.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

private const val WALKS_TABLE = "walks"

/**
 * The finished walk as the diary keeps it, or null when it never left its spot: a walk is a way
 * walked, and photos alone do not make one.
 */
fun walkEntity(
    track: WalkTrack,
    number: Int,
    endedAt: Instant,
    weightKg: Double?,
    strideM: Double,
): WalkEntity? {
    val route = routeText(track.points) ?: return null
    val seconds = track.movingSeconds(endedAt).coerceAtLeast(0)
    return WalkEntity(
        id = track.id,
        number = number,
        startedAt = track.startedAt,
        endedAt = endedAt,
        durationS = seconds.toInt(),
        steps = track.stepsWith(strideM),
        distanceM = track.metres.roundToInt(),
        calories = walkCalories(track.metres, seconds, weightKg),
        route = route,
        breaks = breaksText(track.legs),
        moodBefore = track.moodBefore,
        moodAfter = track.moodAfter,
        updatedAt = endedAt,
    )
}

/** The moods said at the two ends of [walk] as entries of Health, at the times they were said. */
internal fun moodEntries(walk: WalkEntity): List<HealthEntryEntity> =
    listOf(walk.moodBefore to walk.startedAt, walk.moodAfter to walk.endedAt).mapNotNull { (mood, at) ->
        val id = UUID.randomUUID().toString()
        mood?.let { HealthEntryEntity(id, HealthKind.MOOD, at, it.toDouble(), null) }
    }

class WalkRepository @Inject constructor(
    private val database: WalkPickDatabase,
    private val photos: PhotoStore,
    private val sync: SyncScheduler,
    private val namer: WalkNamer,
    private val steps: StepsRepository,
) {
    private val walks = database.walks()
    private val finds = database.finds()
    private val deletes = database.pendingDeletes()

    fun watch(id: String): Flow<WalkEntity?> = walks.watch(id)

    /** The photos of a walk in the order they were taken. */
    fun watchFinds(id: String): Flow<List<FindEntity>> = finds.watchOf(id)

    /** When every walk began. */
    fun watchStarts(): Flow<List<Instant>> = walks.watchStarts()

    /** The latest walk; null before the first one. */
    fun watchLatest(): Flow<WalkEntity?> = walks.watchLatest()

    /** Every walk, the latest first. */
    fun watchAll(): Flow<List<WalkEntity>> = walks.watchAll()

    /** Where the latest walk with a route began, as (latitude, longitude); null before the first one. */
    suspend fun lastStart(): Pair<Double, Double>? = routePlaces(walks.latestRoute()).firstOrNull()

    /** A note of blanks only is no note. */
    suspend fun setNote(id: String, note: String) {
        walks.setNote(id, note.trim().ifEmpty { null }, Instant.now())
        sync.pushSoon()
    }

    /** A blank name is not a name: the walk keeps the one it has. */
    suspend fun setName(id: String, name: String) {
        if (name.isBlank()) return
        walks.setName(id, name.trim(), Instant.now())
        sync.pushSoon()
    }

    /** Removes the walk from the phone at once and from the server with the next sync. */
    suspend fun delete(id: String) {
        val walk = walks.watch(id).first() ?: return
        // The finds leave with the walk by themselves; their photos are files and have to be named.
        val taken = finds.watchOf(id).first().map { it.photoPath }
        database.withTransaction {
            walks.delete(id)
            steps.recount(walk)
            deletes.add(PendingDeleteEntity(WALKS_TABLE, id))
            taken.forEach { deletes.add(PendingDeleteEntity(PHOTO_BUCKET, it)) }
        }
        taken.forEach(photos::delete)
        sync.pushSoon()
    }

    /** Keeps the walk under the next number and sends it to the server; null when [walkEntity] refused it. */
    suspend fun save(track: WalkTrack, weightKg: Double?, strideM: Double): WalkEntity? {
        val kept = walkEntity(track, walks.lastNumber() + 1, Instant.now(), weightKg, strideM)
        if (kept == null) {
            // The photos of a walk that is not kept go with it; some are on the server already.
            track.finds.forEach {
                photos.delete(it.photoPath)
                deletes.add(PendingDeleteEntity(PHOTO_BUCKET, it.photoPath))
            }
            if (track.finds.isNotEmpty()) sync.pushSoon()
            return null
        }
        val walk = kept.copy(name = namer.name(track))
        // All of it or none: a walk without its finds or outside its day cannot be told from a whole one.
        database.withTransaction {
            walks.save(listOf(walk))
            finds.save(track.finds)
            steps.recount(walk)
            // The moods of a walk are entries of Health like any other: its chart shows them too.
            database.healthEntries().save(moodEntries(walk))
        }
        sync.pushSoon()
        return walk
    }
}
