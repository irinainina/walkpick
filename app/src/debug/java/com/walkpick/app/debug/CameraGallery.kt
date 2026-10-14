package com.walkpick.app.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.walkpick.app.data.places.NearPlace
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.ui.camera.CameraState
import com.walkpick.app.ui.camera.Shot
import com.walkpick.app.ui.screens.CameraScreen
import com.walkpick.app.ui.screens.PlaceSheet

// The green of the viewfinder in the Figma frame, in place of the live picture.
private val Scene = Brush.linearGradient(listOf(Color(0xFFC3F0B8), Color(0xFF5AA469)))

/** The camera over a still picture, for the comparison with the design; [asking] is the taken photo. */
@Composable
fun CameraGallery(identifying: Boolean, asking: Boolean = false, placing: Boolean = false) {
    val shot = Shot("", null).takeIf { asking || identifying || placing }
    val state = CameraState(identifying = identifying, shot = shot)
    CameraScreen(state, onClose = {}, onKind = {}, onFlash = {}, onZoom = {}, onFlip = {}, onShutter = {}) {
        Box(Modifier.fillMaxSize().background(Scene))
    }
    if (placing) PlaceSheet(places, DistanceUnit.KM, onSave = { _, _ -> }, onDismiss = {})
}

// The three places of the frame "07 Camera · Which place".
private val places = listOf(
    NearPlace("St. Nicholas Church", "Church", 40),
    NearPlace("Taras Shevchenko monument", "Monument", 120),
    NearPlace("Lebedyn Art Museum", "Museum", 180),
)
