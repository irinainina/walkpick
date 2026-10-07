package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.walkspot.app.R
import com.walkspot.app.data.places.Place
import com.walkspot.app.ui.components.CircleButton
import com.walkspot.app.ui.components.MeDot
import com.walkspot.app.ui.components.PaleRouteLine
import com.walkspot.app.ui.components.RouteLine
import com.walkspot.app.ui.components.SegmentedControl
import com.walkspot.app.ui.components.UnvisitedPlacePin
import com.walkspot.app.ui.components.WalkMap
import com.walkspot.app.ui.components.routeView
import com.walkspot.app.ui.map.MapState
import com.walkspot.app.ui.map.MapTab
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing

private val SwitchHeight = 40.dp
private val RouteEdge = 40.dp
private val LatestWidth = 6.dp
private val OlderWidth = 4.dp
private const val PLACES_FROM_ZOOM = 13f

// The second, third, fourth and fifth route back, as the design fades them.
private val OlderAlpha = listOf(0.6f, 0.45f, 0.32f, 0.22f)

@Composable
fun MapScreen(
    state: MapState,
    onTab: (MapTab) -> Unit,
    onLook: (Double, Double) -> Unit,
    onPlace: (Place) -> Unit,
    onLocate: () -> Unit,
    onWalk: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val underSwitch = statusBar + Spacing.sm + SwitchHeight
    Box(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas),
    ) {
        if (state.tab == MapTab.Map) {
            RoutesMap(state, onLook, onPlace, onLocate, PaddingValues(top = underSwitch))
        } else {
            val content = PaddingValues(Spacing.ml, underSwitch + Spacing.ml, Spacing.ml, Spacing.l)
            DiaryList(state, onWalk, content)
        }
        SegmentedControl(
            options = listOf(stringResource(R.string.tab_map), stringResource(R.string.map_diary)),
            selectedIndex = state.tab.ordinal,
            onSelect = { onTab(MapTab.entries[it]) },
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = Spacing.ml, top = Spacing.sm, end = Spacing.ml),
            ground = if (state.tab == MapTab.Map) AppTheme.colors.surface else AppTheme.colors.surfaceTint,
        )
    }
}

// The latest walk in the gradient, the four before it paler and paler.
@Composable
private fun RoutesMap(
    state: MapState,
    onLook: (Double, Double) -> Unit,
    onPlace: (Place) -> Unit,
    onLocate: () -> Unit,
    covered: PaddingValues,
) {
    val camera = rememberCameraPositionState()
    val routes = remember(state.routes) {
        state.routes.map { route -> route.map { (lat, lon) -> LatLng(lat, lon) } }
    }
    val edge = with(LocalDensity.current) { RouteEdge.roundToPx() }
    var loaded by remember { mutableStateOf(false) }
    // The map opens on the latest walk; the locate button moves it to the phone, each time it is pressed.
    LaunchedEffect(loaded, routes.firstOrNull()) {
        if (loaded) routes.firstOrNull()?.let { routeView(it, edge) }?.let { camera.move(it) }
    }
    val me = state.me?.let { LatLng(it.latitude, it.longitude) }
    LaunchedEffect(loaded, state.located) {
        if (loaded && me != null) routeView(listOf(me), edge)?.let { camera.animate(it) }
    }
    // Places are looked up where the map has stopped, and only close enough to tell them apart.
    LaunchedEffect(loaded, camera.isMoving) {
        val at = camera.position
        val near = at.zoom >= PLACES_FROM_ZOOM
        if (loaded && !camera.isMoving && near) onLook(at.target.latitude, at.target.longitude)
    }
    Box(Modifier.fillMaxSize()) {
        WalkMap(camera, Modifier.fillMaxSize(), covered, onLoaded = { loaded = true }) {
            routes.drop(1).forEachIndexed { index, route ->
                PaleRouteLine(route, OlderWidth, OlderAlpha.getOrElse(index) { OlderAlpha.last() })
            }
            routes.firstOrNull()?.let { RouteLine(it, LatestWidth) }
            val centre = Offset(0.5f, 0.5f)
            state.places.forEach { place ->
                key(place.id) {
                    MarkerComposable(
                        state = rememberUpdatedMarkerState(LatLng(place.latitude, place.longitude)),
                        anchor = centre,
                        onClick = {
                            onPlace(place)
                            true
                        },
                    ) { UnvisitedPlacePin() }
                }
            }
            if (me != null) {
                MarkerComposable(state = rememberUpdatedMarkerState(me), anchor = centre) { MeDot() }
            }
        }
        CircleButton(
            iconRes = R.drawable.ic_locate,
            contentDescription = stringResource(R.string.walk_locate),
            onClick = onLocate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.ml),
            tint = AppTheme.colors.accent,
            raised = true,
        )
    }
}
