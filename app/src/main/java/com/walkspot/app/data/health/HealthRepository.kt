package com.walkspot.app.data.health

import com.walkspot.app.data.local.HealthEntryDao
import com.walkspot.app.data.local.HealthEntryEntity
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** What a person can be: the limits only keep slips of the finger out. */
object HealthLimits {
    val mood = 1.0..5.0
    val weightKg = 20.0..300.0
    val systolic = 60.0..250.0
    val diastolic = 40.0..150.0
    val glucoseMmol = 2.0..30.0
}

/** The entry as it is stored, or null when the numbers or the moment cannot be true. */
fun healthEntry(
    kind: HealthKind,
    value: Double,
    diastolic: Int?,
    measuredAt: Instant,
    now: Instant,
): HealthEntryEntity? {
    val limits = when (kind) {
        HealthKind.MOOD -> HealthLimits.mood
        HealthKind.WEIGHT -> HealthLimits.weightKg
        HealthKind.PRESSURE -> HealthLimits.systolic
        HealthKind.GLUCOSE -> HealthLimits.glucoseMmol
    }
    val lowerFits = if (kind == HealthKind.PRESSURE) {
        diastolic != null && diastolic.toDouble() in HealthLimits.diastolic && diastolic < value
    } else {
        diastolic == null
    }
    val whole = kind == HealthKind.WEIGHT || kind == HealthKind.GLUCOSE || value % 1 == 0.0
    val numbersFit = value in limits && lowerFits && whole
    if (!numbersFit || measuredAt > now) return null
    return HealthEntryEntity(UUID.randomUUID().toString(), kind, measuredAt, value, diastolic)
}

class HealthRepository @Inject constructor(
    private val entries: HealthEntryDao,
    private val sync: SyncScheduler,
) {
    fun watch(kind: HealthKind): Flow<List<HealthEntryEntity>> = entries.watch(kind)

    fun watchLatest(kind: HealthKind): Flow<Double?> = entries.watchLatest(kind)

    /** False when the entry was refused, see [healthEntry]. */
    suspend fun add(kind: HealthKind, value: Double, diastolic: Int?, measuredAt: Instant): Boolean {
        val entry = healthEntry(kind, value, diastolic, measuredAt, Instant.now()) ?: return false
        entries.save(listOf(entry))
        sync.pushSoon()
        return true
    }
}
