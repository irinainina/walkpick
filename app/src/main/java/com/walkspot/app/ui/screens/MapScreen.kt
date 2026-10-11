package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.rememberCameraPositionState
import com.walkspot.app.R
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.ui.components.AppChip
import com.walkspot.app.ui.components.CircleButton
import com.walkspot.app.ui.components.PaleRouteLine
import com.walkspot.app.ui.components.RouteLine
import com.walkspot.app.ui.components.SegmentedControl
import com.walkspot.app.ui.components.WalkMap
import com.walkspot.app.ui.components.routeView
import com.walkspot.app.ui.map.MapFilter
import com.walkspot.app.ui.map.MapPick
import com.walkspot.app.ui.map.MapState
import com.walkspot.app.ui.map.MapTab
import com.walkspot.app.ui.map.mapMarks
import com.walkspot.app.ui.map.shownFinds
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing
import java.io.File

private val SwitchHeight = 40.dp
private val ChipsHeight = 36.dp
private val RouteEdge = 40.dp
private val LatestWidth = 6.dp
private val OlderWidth = 4.dp
private const val PLACES_FROM_ZOOM = 13f

// The second, third, fourth and fifth route back, as the design fades them.
private val OlderAlpha = listOf(0.6f, 0.45f, 0.32f, 0.22f)

/** What the map tab asks of its owner. */
class MapActions(
    val onTab: (MapTab) -> Unit,
    val onFilter: (MapFilter) -> Unit,
    /** The map has stopped at this point: the places around it are wanted. */
    val onLook: (Double, Double) -> Unit,
    /** A pin is tapped, or the map beside every pin: null takes the card away. */
    val onPick: (MapPick?) -> Unit,
    val onOpen: (MapPick) -> Unit,
    val onLocate: () -> Unit,
    val onWalk: (String) -> Unit,
)

@Composable
fun MapScreen(
    state: MapState,
    actions: MapActions,
    /** Finds the photo of a find; null when there is none. */
    photo: suspend (FindEntity) -> File?,
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
            FindsMap(state, actions, PaddingValues(top = underSwitch + Spacing.sm + ChipsHeight))
        } else {
            val content = PaddingValues(Spacing.ml, underSwitch + Spacing.ml, Spacing.ml, Spacing.l)
            DiaryList(state, actions.onWalk, photo, content)
        }
        Column(Modifier.statusBarsPadding()) {
            SegmentedControl(
                options = listOf(stringResource(R.string.tab_map), stringResource(R.string.map_diary)),
                selectedIndex = state.tab.ordinal,
                onSelect = { actions.onTab(MapTab.entries[it]) },
                modifier = Modifier.padding(start = Spacing.ml, top = Spacing.sm, end = Spacing.ml),
                ground = with(AppTheme.colors) { if (state.tab == MapTab.Map) surface else surfaceTint },
            )
            if (state.tab == MapTab.Map) {
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.ml),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    MapFilter.entries.forEach { filter ->
                        val picked = filter == state.filter
                        AppChip(stringResource(filter.label), picked, { actions.onFilter(filter) })
                    }
                }
            }
        }
    }
}

private val MapFilter.label: Int
    get() = when (this) {
        MapFilter.All -> R.string.map_all
        MapFilter.Plants -> R.string.map_plants
        MapFilter.Birds -> R.string.map_birds
        MapFilter.Places -> R.string.map_places
    }

// The routes of the latest walks, the finds of all time and the places of Wikipedia around.
@Composable
private fun FindsMap(state: MapState, actions: MapActions, covered: PaddingValues) {
    val camera = rememberCameraPositionState()
    val routes = remember(state.routes) {
        state.routes.map { route -> route.places.map { (lat, lon) -> LatLng(lat, lon) } }
    }
    val looks = state.routes.map { it.look }
    val edge = with(LocalDensity.current) { RouteEdge.roundToPx() }
    var loaded by remember { mutableStateOf(false) }
    // The map opens on the latest walk; the locate button moves it to the phone, each time it is pressed.
    LaunchedEffect(loaded, routes.firstOrNull()) {
        if (loaded) routes.firstOrNull()?.let { routeView(it, edge) }?.let { camera.move(it) }
    }
    LaunchedEffect(loaded, state.located) {
        val me = state.me?.let { LatLng(it.latitude, it.longitude) }
        if (loaded && me != null) routeView(listOf(me), edge)?.let { camera.animate(it) }
    }
    // What is in sight once the map has stopped: pins are made for that part only.
    var sight by remember { mutableStateOf<Pair<LatLngBounds, Float>?>(null) }
    LaunchedEffect(loaded, camera.isMoving) {
        if (!loaded || camera.isMoving) return@LaunchedEffect
        val at = camera.position
        camera.projection?.visibleRegion?.latLngBounds?.let { sight = it to at.zoom }
        // Places are looked up only close enough to tell them apart.
        if (at.zoom >= PLACES_FROM_ZOOM) actions.onLook(at.target.latitude, at.target.longitude)
    }
    val shown = remember(state.finds, state.filter) { shownFinds(state.finds, state.filter) }
    val marks = remember(shown, sight) {
        val (bounds, zoom) = sight ?: return@remember emptyList()
        mapMarks(shown.filter { bounds.contains(LatLng(it.latitude, it.longitude)) }, zoom)
    }
    Box(Modifier.fillMaxSize()) {
        WalkMap(
            camera = camera,
            modifier = Modifier.fillMaxSize(),
            covered = covered,
            onLoaded = { loaded = true },
            onTap = { actions.onPick(null) },
        ) {
            routes.drop(1).forEachIndexed { index, route ->
                val alpha = OlderAlpha.getOrElse(index) { OlderAlpha.last() }
                PaleRouteLine(route, OlderWidth, alpha, looks[index + 1])
            }
            routes.firstOrNull()?.let { RouteLine(it, LatestWidth, looks.first()) }
            MapPins(state, marks, camera, actions.onPick)
        }
        val bottom = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(Spacing.ml)
        Column(bottom, horizontalAlignment = Alignment.End) {
            CircleButton(
                iconRes = R.drawable.ic_locate,
                contentDescription = stringResource(R.string.walk_locate),
                onClick = actions.onLocate,
                tint = AppTheme.colors.accent,
                raised = true,
            )
            state.pick?.let { pick ->
                Spacer(Modifier.height(Spacing.m))
                MapPickCard(pick, state.me, state.distanceUnit, onOpen = { actions.onOpen(pick) })
            }
        }
    }
}
