package com.walkspot.app.debug

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import com.walkspot.app.ui.components.WalkMap

private const val STREET_ZOOM = 15f

// The centre of Kyiv: streets, a park and the river in one view.
private val Centre = LatLng(50.4501, 30.5234)

/** The styled map alone: shows that the key works and what the style looks like. */
@Composable
fun MapGallery() {
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(Centre, STREET_ZOOM) }
    WalkMap(camera, Modifier.fillMaxSize())
}
