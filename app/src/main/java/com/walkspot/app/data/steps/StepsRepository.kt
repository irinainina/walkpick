package com.walkspot.app.data.steps

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.walkspot.app.data.local.DailyStepsDao
import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

// Until Profile lets the user set a goal and a height.
const val DEFAULT_DAILY_GOAL = 10_000
const val DEFAULT_STRIDE_M = 0.7

// A slow walk, set by the owner; active time is an estimate, the sensor gives steps only.
private const val STEPS_PER_ACTIVE_MINUTE = 40

class StepsRepository @Inject constructor(
    private val counter: StepCounter,
    private val store: DataStore<Preferences>,
    private val days: DailyStepsDao,
    private val sync: SyncScheduler,
) {
    fun access(): StepsAccess = counter.access()

    fun watchDays(): Flow<List<DailyStepsEntity>> = days.watchAll()

    // Adds the steps walked since the last reading to their days; false when steps cannot be read.
    suspend fun refresh(): Boolean {
        if (counter.access() != StepsAccess.COUNTING) return false
        val now = counter.read() ?: return false
        val last = lastReading()
        val walked = stepsBetween(last, now)
        if (last != null && walked > 0) {
            val added = splitByDay(last.at, now.at, walked, ZoneId.systemDefault())
            val from = added.keys.minBy { it.toEpochDay() }
            days.save(addSteps(days.since(from), added, DEFAULT_DAILY_GOAL, DEFAULT_STRIDE_M, now.at))
            sync.pushSoon()
        }
        remember(now)
        return true
    }

    private suspend fun lastReading(): CounterReading? {
        val saved = store.data.first()
        val at = saved[READ_AT] ?: return null
        return CounterReading(saved[BOOT] ?: 0, saved[COUNTER] ?: 0, Instant.ofEpochMilli(at))
    }

    private suspend fun remember(reading: CounterReading) {
        store.edit {
            it[BOOT] = reading.boot
            it[COUNTER] = reading.steps
            it[READ_AT] = reading.at.toEpochMilli()
        }
    }

    private companion object {
        val BOOT = intPreferencesKey("steps_boot")
        val COUNTER = longPreferencesKey("steps_counter")
        val READ_AT = longPreferencesKey("steps_read_at")
    }
}

// The first reading only sets the starting point; after a restart the counter began again from zero.
internal fun stepsBetween(last: CounterReading?, now: CounterReading): Int = when {
    last == null -> 0L
    last.boot != now.boot -> now.steps
    else -> (now.steps - last.steps).coerceAtLeast(0)
}.toInt()

// When the app slept through midnight nobody knows on which side the steps were made:
// they are shared by time, which is less wrong than one day taking all of them.
internal fun splitByDay(from: Instant, to: Instant, steps: Int, zone: ZoneId): Map<LocalDate, Int> {
    val lastDay = to.atZone(zone).toLocalDate()
    val total = Duration.between(from, to).seconds
    if (total <= 0) return mapOf(lastDay to steps)
    val shares = mutableMapOf<LocalDate, Int>()
    var day = from.atZone(zone).toLocalDate()
    var start = from
    while (day < lastDay) {
        val midnight = day.plusDays(1).atStartOfDay(zone).toInstant()
        shares[day] = (steps * Duration.between(start, midnight).seconds / total).toInt()
        start = midnight
        day = day.plusDays(1)
    }
    shares[lastDay] = steps - shares.values.sum()
    return shares.filterValues { it > 0 }
}

internal fun addSteps(
    stored: List<DailyStepsEntity>,
    added: Map<LocalDate, Int>,
    goal: Int,
    strideM: Double,
    now: Instant,
): List<DailyStepsEntity> {
    val known = stored.associateBy { it.day }
    return added.map { (day, more) ->
        val old = known[day]
        val steps = (old?.steps ?: 0) + more
        DailyStepsEntity(
            day = day,
            steps = steps,
            distanceM = (steps * strideM).roundToInt(),
            activeMinutes = steps / STEPS_PER_ACTIVE_MINUTE,
            goal = old?.goal ?: goal,
            updatedAt = now,
        )
    }
}
