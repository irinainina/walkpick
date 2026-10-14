package com.walkpick.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.walk.routePlaces

private val RouteWidth = 4.dp
private val StartPin = 28.dp

/**
 * The whole route of a walk as a picture on the map, its start flagged; the page around it scrolls,
 * the map does not. A walk kept for its photos alone has no line: it shows where the first photo was.
 */
@Composable
fun RoutePicture(
    walk: WalkEntity,
    finds: List<FindEntity>,
    edge: Dp,
    modifier: Modifier = Modifier,
    // What the screen lays over the map: the route keeps clear of it.
    covered: PaddingValues = PaddingValues(),
) {
    val route = remember(walk.route, finds) {
        val photo = finds.take(1).map { it.latitude to it.longitude }
        routePlaces(walk.route).ifEmpty { photo }.map { (lat, lon) -> LatLng(lat, lon) }
    }
    val camera = rememberCameraPositionState()
    val edgePx = with(LocalDensity.current) { edge.roundToPx() }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(route, loaded) {
        if (loaded) routeView(route, edgePx)?.let { camera.move(it) }
    }
    WalkMap(camera, modifier, covered, still = true, onLoaded = { loaded = true }) {
        RouteLine(route, RouteWidth, walk.look())
        route.firstOrNull()?.let { start ->
            MarkerComposable(state = rememberUpdatedMarkerState(start), anchor = Offset(0.5f, 0.5f)) {
                StartFlag(StartPin)
            }
        }
    }
}
