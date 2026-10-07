package com.walkspot.app.data.walk

import android.content.Context
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.InstantIso
import com.walkspot.app.data.local.SyncJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Some phones switch the step sensor off behind a dark screen. A count below this share of the steps
// the distance takes is such a cut, not a long stride.
private const val TRUSTED_SHARE = 0.7

/**
 * A walk being recorded. A new leg starts after every pause: the way from where the walk was paused to
 * where it went on is not walked distance.
 */
@Serializable
data class WalkTrack(
    @Serializable(InstantIso::class) val startedAt: Instant,
    val legs: List<List<TrackPoint>> = listOf(emptyList()),
    @Serializable(InstantIso::class) val pausedAt: Instant? = null,
    val pausedSeconds: Long = 0,
    val steps: Int = 0,
    /** The last reading of the phone's step counter, which counts from the start of the phone. */
    val lastCounter: Long? = null,
    /** The id the saved walk gets: photos taken on the way point at it. */
    val id: String = UUID.randomUUID().toString(),
    /** The photos taken on the way; they are kept with the walk: a find cannot be without one. */
    val finds: List<FindEntity> = emptyList(),
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

    /** The counted steps, or the steps the distance takes when the counter fell far behind it. */
    fun stepsWith(strideM: Double): Int {
        val needed = (metres / strideM).toInt()
        return if (steps < needed * TRUSTED_SHARE) needed else steps
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

/** The walk going on, kept in a file: Android may end the app in the middle of a walk. */
class WalkDraft(private val file: File) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, "walk.json"))

    /** The unfinished walk, or null when there is none or the file cannot be read. */
    fun read(): WalkTrack? = try {
        if (file.exists()) SyncJson.decodeFromString(WalkTrack.serializer(), file.readText()) else null
    } catch (_: IOException) {
        null
    } catch (_: SerializationException) {
        null
    }

    fun write(track: WalkTrack?) {
        if (track == null) {
            file.delete()
        } else {
            file.writeText(SyncJson.encodeToString(WalkTrack.serializer(), track))
        }
    }
}

/** The walk going on now, shared by the service that listens to GPS and the screens that show it. */
@Singleton
class WalkRecorder @Inject constructor(draft: WalkDraft) {
    // A walk the app was ended in the middle of goes on from where it was.
    private val current = MutableStateFlow(draft.read())

    val track: StateFlow<WalkTrack?> = current

    init {
        // Written off the main thread; of several changes in a row only the last one matters.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            current.drop(1).collectLatest(draft::write)
        }
    }

    /** Starts a walk unless one is going on. */
    fun start(at: Instant) = current.update { it ?: WalkTrack(at) }

    fun report(point: TrackPoint) = current.update { it?.with(point) }

    fun add(find: FindEntity) = current.update { it?.copy(finds = it.finds + find) }

    fun changeFind(id: String, transform: (FindEntity) -> FindEntity) = current.update { track ->
        track?.copy(finds = track.finds.map { if (it.id == id) transform(it) else it })
    }

    fun removeFind(id: String) = current.update { track ->
        track?.copy(finds = track.finds.filterNot { it.id == id })
    }

    fun reportSteps(counter: Long) = current.update { it?.withCounter(counter) }

    fun pause(at: Instant) = current.update { it?.pausedAt(at) }

    fun resume(at: Instant) = current.update { it?.resumedAt(at) }

    /** Ends the walk and hands over what was recorded. */
    fun finish(): WalkTrack? = current.getAndUpdate { null }
}
