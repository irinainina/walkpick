package com.walkspot.app.data.steps

import com.walkspot.app.data.local.DailyStepsDao
import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.WalkDao
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.profile.ProfileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
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
    private val walks: WalkDao,
    private val profiles: ProfileStore,
) {
    fun access(): StepsAccess = counter.access()

    fun watchDays(): Flow<List<DailyStepsEntity>> = days.watchAll()

    /** Counts the day [walk] began on anew from its walks; called once the walk is kept or deleted. */
    suspend fun recount(walk: WalkEntity) {
        val zone = ZoneId.systemDefault()
        val day = walk.startedAt.atZone(zone).toLocalDate()
        val next = day.plusDays(1)
        val began = walks.between(day.atStartOfDay(zone).toInstant(), next.atStartOfDay(zone).toInstant())
        val goal = days.since(day).firstOrNull { it.day == day }?.goal ?: profiles.value.first().dailyGoal
        days.save(listOf(dayOf(day, began, goal, Instant.now())))
    }
}

/** The day as its [walks] make it: a day holds nothing but what was walked between Start and Stop. */
internal fun dayOf(day: LocalDate, walks: List<WalkEntity>, goal: Int, now: Instant) = DailyStepsEntity(
    day = day,
    steps = walks.sumOf { it.steps },
    distanceM = walks.sumOf { it.distanceM },
    activeMinutes = walks.sumOf { (it.durationS / SECONDS_IN_MINUTE).roundToInt() },
    goal = goal,
    updatedAt = now,
)
