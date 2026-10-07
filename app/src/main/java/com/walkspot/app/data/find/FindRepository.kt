package com.walkspot.app.data.find

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.walk.LocationFinder
import com.walkspot.app.data.walk.TrackPoint
import com.walkspot.app.data.walk.WalkRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

// Enough for a full-screen photo on a phone; the camera gives four times as many pixels.
private const val LONGEST_SIDE = 1600
private const val JPEG_QUALITY = 85

/** How much a photo is shrunk so that its longest side is [longest]; a small photo is never enlarged. */
internal fun keptScale(width: Int, height: Int, longest: Int = LONGEST_SIDE): Float =
    (longest.toFloat() / maxOf(width, height)).coerceAtMost(1f)

/** A photo just taken on the walk [walkId]: a find nobody has named yet. */
internal fun newFind(id: String, walkId: String, place: TrackPoint, takenAt: Instant) = FindEntity(
    id = id,
    walkId = walkId,
    takenAt = takenAt,
    latitude = place.latitude,
    longitude = place.longitude,
    photoPath = "$id.jpg",
    updatedAt = takenAt,
)

class FindRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val recorder: WalkRecorder,
    private val locations: LocationFinder,
) {
    private val folder = File(context.filesDir, "photos")

    fun file(find: FindEntity) = File(folder, find.photoPath)

    /**
     * Keeps [photo], which the camera gives [turned] degrees off, with the walk going on, as a find
     * at the place the phone is; null without a walk or without a place.
     */
    suspend fun add(photo: Bitmap, turned: Int): FindEntity? {
        val track = recorder.track.value ?: return null
        val place = track.points.lastOrNull() ?: locations.find() ?: return null
        val find = newFind(UUID.randomUUID().toString(), track.id, place, Instant.now())
        withContext(Dispatchers.IO) {
            val scale = keptScale(photo.width, photo.height)
            val upright = Matrix().apply {
                postScale(scale, scale)
                postRotate(turned.toFloat())
            }
            val kept = Bitmap.createBitmap(photo, 0, 0, photo.width, photo.height, upright, true)
            folder.mkdirs()
            file(find).outputStream().use { kept.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        }
        recorder.add(find)
        return find
    }
}
