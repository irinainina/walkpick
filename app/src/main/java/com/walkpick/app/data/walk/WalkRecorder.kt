package com.walkpick.app.data.walk

import android.content.Context
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.InstantIso
import com.walkpick.app.data.local.SyncJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

// Some phones switch the step sensor off behind a dark screen, and GPS loses the fix among houses.
// A count or a route below this share of what the other one gives is such a cut, not a long stride.
private const val TRUSTED_SHARE = 0.7

// An app ended in the middle of a walk loses the way walked since the last writing, no more than this.
private val DRAFT_EVERY = 10.seconds

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
    /** How the walker felt setting out and coming back, 1 (awful) to 5 (great); null when not said. */
    val moodBefore: Int? = null,
    val moodAfter: Int? = null,
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

    /** The length of the route, or the way the counted steps take when the route fell far behind it. */
    fun metresWith(strideM: Double): Double {
        val walked = steps * strideM
        return if (metres < walked * TRUSTED_SHARE) walked else metres
    }

    /** Whether nothing but the way and the steps changed since [earlier]: a draft may wait for that. */
    fun onlyMovedSince(earlier: WalkTrack?): Boolean = earlier != null &&
        copy(legs = earlier.legs, steps = earlier.steps, lastCounter = earlier.lastCounter) == earlier

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
            // Written aside and renamed: an app ended in the middle of writing leaves the old draft whole.
            val fresh = File(file.path + ".new")
            fresh.writeText(SyncJson.encodeToString(WalkTrack.serializer(), track))
            Files.move(fresh.toPath(), file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
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
        var written = current.value
        var writtenAt = TimeSource.Monotonic.markNow()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            current.collectLatest { track ->
                if (track == written) return@collectLatest
                // Points and steps come every second or two and the whole walk is written each time.
                if (track?.onlyMovedSince(written) == true) delay(DRAFT_EVERY - writtenAt.elapsedNow())
                draft.write(track)
                written = track
                writtenAt = TimeSource.Monotonic.markNow()
            }
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

    fun feel(mood: Int) = current.update { it?.copy(moodBefore = mood) }

    fun pause(at: Instant) = current.update { it?.pausedAt(at) }

    fun resume(at: Instant) = current.update { it?.resumedAt(at) }

    /** Ends the walk and hands over what was recorded. */
    fun finish(): WalkTrack? = current.getAndUpdate { null }
}
