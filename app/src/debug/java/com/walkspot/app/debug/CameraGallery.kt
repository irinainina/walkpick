package com.walkspot.app.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.walkspot.app.ui.camera.CameraState
import com.walkspot.app.ui.screens.CameraScreen

// The green of the viewfinder in the Figma frame, in place of the live picture.
private val Scene = Brush.linearGradient(listOf(Color(0xFFC3F0B8), Color(0xFF5AA469)))

/** The camera over a still picture, for the comparison with the design. */
@Composable
fun CameraGallery(identifying: Boolean) {
    val state = CameraState(identifying = identifying)
    CameraScreen(state, onClose = {}, onKind = {}, onFlash = {}, onFlip = {}, onShutter = {}) {
        Box(Modifier.fillMaxSize().background(Scene))
    }
}
