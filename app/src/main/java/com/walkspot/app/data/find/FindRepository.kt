package com.walkspot.app.data.find

import android.graphics.Bitmap
import android.graphics.Matrix
import com.walkspot.app.data.account.AccountStore
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.data.walk.LocationFinder
import com.walkspot.app.data.walk.TrackPoint
import com.walkspot.app.data.walk.WalkRecorder
import com.walkspot.app.data.walk.WalkTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

// Enough for a full-screen photo on a phone; the camera gives four times as many pixels.
private const val LONGEST_SIDE = 1600
private const val JPEG_QUALITY = 85

// GPS loses the walker for minutes under trees: an older point of the route is not where the photo is
// taken, so the phone is asked, but not for long.
private const val FRESH_POINT_S = 30L
private const val PLACE_WAIT_MS = 4_000L

/** How much a photo is shrunk so that its longest side is [longest]; a small photo is never enlarged. */
internal fun keptScale(width: Int, height: Int, longest: Int = LONGEST_SIDE): Float =
    (longest.toFloat() / maxOf(width, height)).coerceAtMost(1f)

/** A photo [owner] has just taken on the walk [walkId]: a find nobody has named yet. */
internal fun newFind(
    id: String,
    owner: String,
    walkId: String,
    place: TrackPoint,
    takenAt: Instant,
    kind: FindKind,
) = FindEntity(
    id = id,
    walkId = walkId,
    takenAt = takenAt,
    latitude = place.latitude,
    longitude = place.longitude,
    // The bucket lets a user into the folder named by the user's id only.
    photoPath = "$owner/$id.jpg",
    // What the walker said it is; the name comes from the service that knows this kind.
    kind = kind,
    updatedAt = takenAt,
)

class FindRepository @Inject constructor(
    private val photos: PhotoStore,
    private val account: AccountStore,
    private val recorder: WalkRecorder,
    private val locations: LocationFinder,
) {
    /**
     * Keeps [photo], which the camera gives [turned] degrees off, with the walk going on, as a find
     * at the place the phone is; null without a walk, a place or an owner. It is a plain photo until
     * the walker says what it shows.
     */
    suspend fun add(photo: Bitmap, turned: Int): FindEntity? {
        val track = recorder.track.value ?: return null
        val place = place(track) ?: return null
        val owner = account.lastUserId() ?: return null
        val id = UUID.randomUUID().toString()
        val find = newFind(id, owner, track.id, place, Instant.now(), FindKind.PHOTO)
        withContext(Dispatchers.IO) {
            val scale = keptScale(photo.width, photo.height)
            val upright = Matrix().apply {
                postScale(scale, scale)
                postRotate(turned.toFloat())
            }
            val kept = Bitmap.createBitmap(photo, 0, 0, photo.width, photo.height, upright, true)
            photos.keep(find.photoPath) { kept.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        }
        recorder.add(find)
        return find
    }

    /** The last point of the route while it is fresh, else where the phone says it is now. */
    private suspend fun place(track: WalkTrack): TrackPoint? {
        val last = track.points.lastOrNull() ?: return locations.find()
        if (Duration.between(last.at, Instant.now()).seconds <= FRESH_POINT_S) return last
        return withTimeoutOrNull(PLACE_WAIT_MS) { locations.find() } ?: last
    }
}
