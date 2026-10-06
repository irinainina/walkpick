package com.walkspot.app.data.walk

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A walk being recorded. A new leg starts after every pause: the way from where the walk was paused to
 * where it went on is not walked distance.
 */
data class WalkTrack(
    val startedAt: Instant,
    val legs: List<List<TrackPoint>> = listOf(emptyList()),
    val pausedAt: Instant? = null,
    val pausedSeconds: Long = 0,
    val steps: Int = 0,
    /** The last reading of the phone's step counter, which counts from the start of the phone. */
    val lastCounter: Long? = null,
) {
    val paused: Boolean get() = pausedAt != null

    val points: List<TrackPoint> get() = legs.flatten()

    val metres: Double get() = legs.sumOf(::routeLengthM)

    /** The time walked up to [now]; a pause stops it. */
    fun movingSeconds(now: Instant): Long =
        Duration.between(startedAt, pausedAt ?: now).seconds - pausedSeconds

    /** [point] is kept when the walk is not paused and the point passes [continuesRoute]. */
    fun with(point: TrackPoint): WalkTrack {
        val leg = legs.last()
        if (paused || !continuesRoute(leg.lastOrNull(), point)) return this
        return copy(legs = legs.dropLast(1).plusElement(leg + point))
    }

    /**
     * Adds the steps counted since the last reading. Steps made during a pause are skipped, and so is
     * a counter that went back: the phone was restarted.
     */
    fun withCounter(reading: Long): WalkTrack {
        val added = lastCounter?.let { reading - it }?.takeIf { it > 0 && !paused } ?: 0
        return copy(steps = steps + added.toInt(), lastCounter = reading)
    }

    fun pausedAt(at: Instant): WalkTrack = if (paused) this else copy(pausedAt = at)

    fun resumedAt(at: Instant): WalkTrack {
        val since = pausedAt ?: return this
        return copy(
            legs = if (legs.last().isEmpty()) legs else legs.plusElement(emptyList()),
            pausedAt = null,
            pausedSeconds = pausedSeconds + Duration.between(since, at).seconds,
        )
    }
}

/** The walk going on now, shared by the service that listens to GPS and the screens that show it. */
@Singleton
class WalkRecorder @Inject constructor() {
    private val current = MutableStateFlow<WalkTrack?>(null)

    val track: StateFlow<WalkTrack?> = current

    /** Starts a walk unless one is going on. */
    fun start(at: Instant) = current.update { it ?: WalkTrack(at) }

    fun report(point: TrackPoint) = current.update { it?.with(point) }

    fun reportSteps(counter: Long) = current.update { it?.withCounter(counter) }

    fun pause(at: Instant) = current.update { it?.pausedAt(at) }

    fun resume(at: Instant) = current.update { it?.resumedAt(at) }

    /** Ends the walk and hands over what was recorded. */
    fun finish(): WalkTrack? = current.getAndUpdate { null }
}
