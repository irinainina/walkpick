package com.walkspot.app.ui.walk

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.health.HealthRepository
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.profile.ProfileStore
import com.walkspot.app.data.profile.strideM
import com.walkspot.app.data.settings.DistanceUnit
import com.walkspot.app.data.settings.SettingsStore
import com.walkspot.app.data.walk.BatteryLimits
import com.walkspot.app.data.walk.TrackPoint
import com.walkspot.app.data.walk.WalkControl
import com.walkspot.app.data.walk.WalkRecorder
import com.walkspot.app.data.walk.WalkRepository
import com.walkspot.app.data.walk.WalkTrack
import com.walkspot.app.data.walk.distanceM
import com.walkspot.app.data.walk.paceSecondsPerKm
import com.walkspot.app.data.walk.walkCalories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

private const val TICK_MS = 1_000L
private const val KEEP_AFTER_LEAVING_MS = 5_000L

data class ActiveWalkState(
    /** False before the walk is started and after it is stopped. */
    val recording: Boolean = false,
    val paused: Boolean = false,
    /** The time walked, without the pauses. */
    val seconds: Long = 0,
    val steps: Int = 0,
    val metres: Double = 0.0,
    val paceSecondsPerKm: Int? = null,
    val calories: Int = 0,
    val route: List<TrackPoint> = emptyList(),
    /** Whether Stop keeps the walk: one that has gone nowhere yet is dropped with its photos. */
    val kept: Boolean = false,
    /** Straight back to the first point; null until the walk has left it. */
    val toStartM: Double? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
)

@HiltViewModel
class ActiveWalkViewModel @Inject constructor(
    private val recorder: WalkRecorder,
    private val control: WalkControl,
    private val walks: WalkRepository,
    private val limits: BatteryLimits,
    health: HealthRepository,
    settings: SettingsStore,
    private val profiles: ProfileStore,
) : ViewModel() {

    private val clock = flow {
        while (true) {
            emit(Instant.now())
            delay(TICK_MS)
        }
    }

    private val weight = health.watchLatest(HealthKind.WEIGHT)

    val state: StateFlow<ActiveWalkState> =
        combine(recorder.track, clock, weight, settings.value, profiles.value) { track, now, kg, units, me ->
            val stride = strideM(me.sex, me.heightCm)
            activeWalkState(track, now, kg, stride).copy(distanceUnit = units.distance)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), ActiveWalkState())

    /** Where the map waits for the first signal: the start of the latest walk. */
    val lastStart: StateFlow<Pair<Double, Double>?> = flow { emit(walks.lastStart()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val battery = MutableStateFlow<Intent?>(null)

    /** The phone's battery page to offer; null when there is nothing to ask. */
    val batteryPage: StateFlow<Intent?> = battery

    /** The location permission must be granted already. */
    fun start() {
        control.start()
        viewModelScope.launch { battery.value = limits.pageToOffer() }
    }

    fun batteryAnswered() {
        battery.value = null
        viewModelScope.launch { limits.offered() }
    }

    fun togglePause() {
        val now = Instant.now()
        if (recorder.track.value?.paused == true) recorder.resume(now) else recorder.pause(now)
    }

    /** Ends the recording and keeps the walk; [onDone] takes its id, or null when it was not kept. */
    fun stop(onDone: (String?) -> Unit) {
        val track = control.stop()
        viewModelScope.launch {
            val kg = weight.first()
            val stride = profiles.value.first().let { strideM(it.sex, it.heightCm) }
            // Leaving the screen meanwhile must not leave the walk half kept.
            val saved = withContext(NonCancellable) {
                track?.let { walks.save(it, kg, stride) }.also { control.done() }
            }
            onDone(saved?.id)
        }
    }
}

internal fun activeWalkState(
    track: WalkTrack?,
    now: Instant,
    weightKg: Double?,
    strideM: Double,
): ActiveWalkState {
    if (track == null) return ActiveWalkState()
    val seconds = track.movingSeconds(now).coerceAtLeast(0)
    val points = track.points
    return ActiveWalkState(
        recording = true,
        paused = track.paused,
        seconds = seconds,
        steps = track.stepsWith(strideM),
        metres = track.metres,
        paceSecondsPerKm = paceSecondsPerKm(track.metres, seconds),
        calories = walkCalories(track.metres, seconds, weightKg),
        route = points,
        kept = points.size > 1,
        toStartM = if (points.size > 1) distanceM(points.last(), points.first()) else null,
    )
}
