package com.walkpick.app.data.find

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

private const val MODEL = "birds.tflite"
private const val LABELS = "birds.csv"

// The model looks at a square of this many pixels, three bytes a pixel.
private const val SIDE = 224
private const val CHANNELS = 3
private const val BYTE = 0xFF
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8

// The class the model gives to a photo with no bird on it.
private const val NO_BIRD = "background"

// Below this the model is guessing: such a find is better left unknown than named wrongly.
private const val LEAST_MATCH = 0.2f
private const val GUESSES = 4

// A bird the model gives less than this is not worth offering under "Not this?".
private const val LEAST_OTHER = 0.01f

/** The names of the classes of the model by their numbers, from the "id,name" lines of its label file. */
internal fun birdLabels(csv: String): Map<Int, String> = csv.lineSequence().mapNotNull { line ->
    val id = line.substringBefore(',').trim().toIntOrNull() ?: return@mapNotNull null
    id to line.substringAfter(',').trim()
}.toMap()

/**
 * The likeliest birds for the [scores] of the model, the best first; none when the model sees no bird
 * or is not sure enough of its best guess. The model knows birds by their Latin names only.
 */
internal fun birdGuesses(scores: FloatArray, labels: Map<Int, String>): List<Guess> {
    val ranked = scores.indices.sortedByDescending { scores[it] }
    val best = ranked.firstOrNull() ?: return emptyList()
    if (labels[best] == NO_BIRD || scores[best] < LEAST_MATCH) return emptyList()
    return ranked
        .filter { labels[it] != null && labels[it] != NO_BIRD && scores[it] >= LEAST_OTHER }
        .take(GUESSES)
        .map { Guess(name = labels.getValue(it), latinName = labels.getValue(it), match = scores[it]) }
}

/** Names the bird on a photo with a model kept in the app: no server, no key, no network. */
@Singleton
class BirdClassifier @Inject constructor(@ApplicationContext private val context: Context) {

    private val labels by lazy { birdLabels(context.assets.open(LABELS).bufferedReader().readText()) }

    private val model by lazy {
        val bytes = context.assets.open(MODEL).readBytes()
        Interpreter(ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes))
    }

    /** Null when the photo cannot be read. */
    suspend fun guesses(photo: File): List<Guess>? = withContext(Dispatchers.Default) {
        val picture = BitmapFactory.decodeFile(photo.path) ?: return@withContext null
        val output = Array(1) { ByteArray(labels.size) }
        // One at a time: an interpreter is not safe to call from two threads.
        synchronized(model) { model.run(pixels(picture), output) }
        birdGuesses(FloatArray(labels.size) { (output[0][it].toInt() and BYTE) / BYTE.toFloat() }, labels)
    }

    // The middle square of the picture, shrunk to the size the model takes, as RGB bytes.
    private fun pixels(picture: Bitmap): ByteBuffer {
        val side = minOf(picture.width, picture.height)
        val left = (picture.width - side) / 2
        val top = (picture.height - side) / 2
        val square = Bitmap.createBitmap(picture, left, top, side, side)
        val small = square.scale(SIDE, SIDE)
        val colours = IntArray(SIDE * SIDE).also { small.getPixels(it, 0, SIDE, 0, 0, SIDE, SIDE) }
        val bytes = ByteBuffer.allocateDirect(SIDE * SIDE * CHANNELS).order(ByteOrder.nativeOrder())
        colours.forEach { colour ->
            bytes.put((colour shr RED_SHIFT and BYTE).toByte())
            bytes.put((colour shr GREEN_SHIFT and BYTE).toByte())
            bytes.put((colour and BYTE).toByte())
        }
        return bytes.apply { rewind() }
    }
}
