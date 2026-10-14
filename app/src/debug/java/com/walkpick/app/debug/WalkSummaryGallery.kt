package com.walkpick.app.debug

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.createBitmap
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.FindKind
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.ui.screens.WalkSummaryScreen
import com.walkpick.app.ui.walk.WalkSummaryState
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

private fun evening(hour: Int, minute: Int) =
    LocalDateTime.of(2026, 9, 29, hour, minute).atZone(ZoneId.systemDefault()).toInstant()

// A ring through the centre of Kyiv shaped like the route of the Figma frame.
private const val PHOTO_PX = 300
private const val JPEG_QUALITY = 90

private const val RING = "30.5180 50.4476,30.5186 50.4500,30.5226 50.4506,30.5232 50.4528,30.5276 50.4532," +
    "30.5288 50.4510,30.5318 50.4504,30.5300 50.4480,30.5236 50.4474,30.5180 50.4476"

// The finds of the frame on the ring, each with a photo of its gradient.
private val finds = listOf(
    Triple(FindKind.PLANT, "Common yarrow", "Achillea millefolium") to (50.4500 to 30.5186),
    Triple(FindKind.BIRD, "Great tit", "Parus major") to (50.4528 to 30.5232),
    Triple(FindKind.PLACE, "St. Nicholas Church", null) to (50.4510 to 30.5288),
).mapIndexed { index, (names, place) ->
    FindEntity(
        id = "find$index",
        walkId = "walk",
        takenAt = evening(18, 50 + index),
        latitude = place.first,
        longitude = place.second,
        photoPath = "",
        kind = names.first,
        name = names.second,
        latinName = names.third,
        match = 0.94f,
        updatedAt = evening(19, 28),
    )
}
private val gradients = listOf(0xFF9BE15D to 0xFF00B4A6, 0xFFFFD26F to 0xFFFF7E5F, 0xFFA1C4FD to 0xFF3F6FD8)

internal fun gradientPhoto(dir: File, index: Int): File = File(dir, "summary-find$index.jpg").also { file ->
    val (from, to) = gradients[index]
    val bitmap = createBitmap(PHOTO_PX, PHOTO_PX)
    val side = PHOTO_PX.toFloat()
    val paint = Paint().apply {
        shader = LinearGradient(0f, 0f, side, side, from.toInt(), to.toInt(), Shader.TileMode.CLAMP)
    }
    Canvas(bitmap).drawRect(0f, 0f, side, side, paint)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
}

/** Walk summary with the numbers of the Figma frame, for the comparison with the design. */
@Composable
fun WalkSummaryGallery() {
    val dir = LocalContext.current.cacheDir
    val photos = remember { finds.indices.associate { "find$it" to gradientPhoto(dir, it) } }
    WalkSummaryScreen(
        state = WalkSummaryState(
            WalkEntity(
                id = "walk",
                number = 312,
                name = "Misty Evening",
                startedAt = evening(18, 40),
                endedAt = evening(19, 28),
                durationS = 48 * 60,
                steps = 5230,
                distanceM = 3900,
                route = RING,
                moodBefore = 3,
                moodAfter = 5,
                updatedAt = evening(19, 28),
            ),
            finds = finds,
            photos = photos,
        ),
        onBack = {},
        onNote = {},
        onRename = {},
        onDelete = {},
        onFind = {},
    )
}
