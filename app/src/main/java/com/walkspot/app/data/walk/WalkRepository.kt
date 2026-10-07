package com.walkspot.app.data.walk

import com.walkspot.app.data.local.WalkDao
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.sync.SyncScheduler
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * The finished walk as the diary keeps it, or null when it never left its spot: a walk with no line
 * to draw is a slip of the finger, not an entry.
 */
fun walkEntity(track: WalkTrack, number: Int, endedAt: Instant, weightKg: Double?): WalkEntity? {
    val route = routeText(track.points) ?: return null
    val seconds = track.movingSeconds(endedAt).coerceAtLeast(0)
    return WalkEntity(
        id = UUID.randomUUID().toString(),
        number = number,
        startedAt = track.startedAt,
        endedAt = endedAt,
        durationS = seconds.toInt(),
        steps = track.steps,
        distanceM = track.metres.roundToInt(),
        calories = walkCalories(track.metres, seconds, weightKg),
        route = route,
        updatedAt = endedAt,
    )
}

class WalkRepository @Inject constructor(
    private val walks: WalkDao,
    private val sync: SyncScheduler,
) {
    /** Keeps the walk under the next number and sends it to the server; null when [walkEntity] refused it. */
    suspend fun save(track: WalkTrack, weightKg: Double?): WalkEntity? {
        val walk = walkEntity(track, walks.lastNumber() + 1, Instant.now(), weightKg) ?: return null
        walks.save(listOf(walk))
        sync.pushSoon()
        return walk
    }
}
