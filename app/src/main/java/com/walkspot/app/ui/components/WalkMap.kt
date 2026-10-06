package com.walkspot.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.walkspot.app.R

// The screens draw their own buttons over the map.
private val BareMap = MapUiSettings(
    compassEnabled = false,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    zoomControlsEnabled = false,
)

/** Google Map in the colours of the design; routes and pins go into [content]. */
@Composable
fun WalkMap(
    camera: CameraPositionState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val style = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style) }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        properties = MapProperties(mapStyleOptions = style),
        uiSettings = BareMap,
        content = content,
    )
}
