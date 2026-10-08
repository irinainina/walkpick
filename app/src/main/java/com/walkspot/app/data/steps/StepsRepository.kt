package com.walkspot.app.data.steps

import com.walkspot.app.data.local.DailyStepsDao
import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.profile.ProfileStore
import com.walkspot.app.data.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * The steps that fill the ring of a day and the track of a bar until the walker sets their own in
 * Settings. Steps are counted on walks only, so it is half of the usual ten thousand.
 */
const val DEFAULT_DAILY_GOAL = 5_000

private const val SECONDS_IN_MINUTE = 60.0

/**
 * The steps, the way and the time of every day. A day holds what its walks hold: steps are counted
 * between Start and Stop only, the phone is not listened to the rest of the day.
 */
class StepsRepository @Inject constructor(
    private val counter: StepCounter,
    private val days: DailyStepsDao,
    private val profiles: ProfileStore,
    private val sync: SyncScheduler,
) {
    fun access(): StepsAccess = counter.access()

    fun watchDays(): Flow<List<DailyStepsEntity>> = days.watchAll()

    /** Adds a finished walk to the day it began on. */
    suspend fun add(walk: WalkEntity) = change(walk, sign = 1)

    /** Takes a deleted walk back out of its day. */
    suspend fun remove(walk: WalkEntity) = change(walk, sign = -1)

    private suspend fun change(walk: WalkEntity, sign: Int) {
        val day = walk.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()
        val stored = days.since(day).firstOrNull { it.day == day }
        days.save(listOf(dayWith(stored, walk, sign, profiles.value.first().dailyGoal, Instant.now())))
        sync.pushSoon()
    }
}

/** The day [stored] (or an empty one) with [walk] added to it, or taken out of it when [sign] is -1. */
internal fun dayWith(
    stored: DailyStepsEntity?,
    walk: WalkEntity,
    sign: Int,
    goal: Int,
    now: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): DailyStepsEntity {
    val minutes = (walk.durationS / SECONDS_IN_MINUTE).roundToInt()
    return DailyStepsEntity(
        day = stored?.day ?: walk.startedAt.atZone(zone).toLocalDate(),
        steps = ((stored?.steps ?: 0) + sign * walk.steps).coerceAtLeast(0),
        distanceM = ((stored?.distanceM ?: 0) + sign * walk.distanceM).coerceAtLeast(0),
        activeMinutes = ((stored?.activeMinutes ?: 0) + sign * minutes).coerceAtLeast(0),
        goal = stored?.goal ?: goal,
        updatedAt = now,
    )
}
