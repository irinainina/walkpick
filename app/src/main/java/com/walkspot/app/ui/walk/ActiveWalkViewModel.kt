package com.walkspot.app.ui.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.health.HealthRepository
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.settings.DistanceUnit
import com.walkspot.app.data.settings.SettingsStore
import com.walkspot.app.data.walk.TrackPoint
import com.walkspot.app.data.walk.WalkControl
import com.walkspot.app.data.walk.WalkRecorder
import com.walkspot.app.data.walk.WalkRepository
import com.walkspot.app.data.walk.WalkTrack
import com.walkspot.app.data.walk.distanceM
import com.walkspot.app.data.walk.paceSecondsPerKm
import com.walkspot.app.data.walk.walkCalories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    /** Straight back to the first point; null until the walk has left it. */
    val toStartM: Double? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
)

@HiltViewModel
class ActiveWalkViewModel @Inject constructor(
    private val recorder: WalkRecorder,
    private val control: WalkControl,
    private val walks: WalkRepository,
    health: HealthRepository,
    settings: SettingsStore,
) : ViewModel() {

    private val clock = flow {
        while (true) {
            emit(Instant.now())
            delay(TICK_MS)
        }
    }

    private val weight = health.watchLatest(HealthKind.WEIGHT)

    val state: StateFlow<ActiveWalkState> =
        combine(recorder.track, clock, weight, settings.value) { track, now, kg, units ->
            activeWalkState(track, now, kg).copy(distanceUnit = units.distance)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), ActiveWalkState())

    /** The location permission must be granted already. */
    fun start() = control.start()

    fun togglePause() {
        val now = Instant.now()
        if (recorder.track.value?.paused == true) recorder.resume(now) else recorder.pause(now)
    }

    /** Ends the recording and keeps the walk; [onDone] runs once it is saved. */
    fun stop(onDone: () -> Unit) {
        val track = control.stop()
        viewModelScope.launch {
            if (track != null) walks.save(track, weight.first())
            onDone()
        }
    }
}

internal fun activeWalkState(track: WalkTrack?, now: Instant, weightKg: Double?): ActiveWalkState {
    if (track == null) return ActiveWalkState()
    val seconds = track.movingSeconds(now).coerceAtLeast(0)
    val points = track.points
    return ActiveWalkState(
        recording = true,
        paused = track.paused,
        seconds = seconds,
        steps = track.steps,
        metres = track.metres,
        paceSecondsPerKm = paceSecondsPerKm(track.metres, seconds),
        calories = walkCalories(track.metres, seconds, weightKg),
        route = points,
        toStartM = if (points.size > 1) distanceM(points.last(), points.first()) else null,
    )
}
