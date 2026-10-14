package com.walkpick.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.walkpick.app.R
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.ui.components.AppChip
import com.walkpick.app.ui.components.CircleButton
import com.walkpick.app.ui.components.ClusterPin
import com.walkpick.app.ui.components.FindPin
import com.walkpick.app.ui.components.MeDot
import com.walkpick.app.ui.components.WalkMap
import com.walkpick.app.ui.components.routeView
import com.walkpick.app.ui.find.look
import com.walkpick.app.ui.map.ClusterMark
import com.walkpick.app.ui.map.FindMark
import com.walkpick.app.ui.map.MapFilter
import com.walkpick.app.ui.map.PhotoMapState
import com.walkpick.app.ui.map.mapMarks
import com.walkpick.app.ui.map.shownFinds
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing
import kotlinx.coroutines.launch

private val ChipsHeight = 36.dp
private val FindsEdge = 40.dp
private val PinSize = 36.dp
private val PickedPinSize = 48.dp

// As drawn: the card stands 80 above the tabs, the logo of the map stays in sight under it.
private val CardBottom = 80.dp

// A tap on a cluster comes this much closer; the cluster falls apart on the way.
private const val ZOOM_STEP = 2f
private val Centre = Offset(0.5f, 0.5f)

/** What the map of photos asks of its owner. */
class PhotoMapActions(
    val onFilter: (MapFilter) -> Unit,
    /** A pin is tapped, or the map beside every pin: null takes the card away. */
    val onPick: (FindEntity?) -> Unit,
    val onOpen: (FindEntity) -> Unit,
    val onLocate: () -> Unit,
)

/** Every photo where it was taken, by kind; [top] is where the controls over the map end. */
@Composable
fun PhotoMap(state: PhotoMapState, actions: PhotoMapActions, top: Dp) {
    val camera = rememberCameraPositionState()
    val edge = with(LocalDensity.current) { FindsEdge.roundToPx() }
    var loaded by remember { mutableStateOf(false) }
    // The map opens on the photos of the latest walk; the locate button moves it to the phone.
    val latest = remember(state.latest) { state.latest.map { LatLng(it.latitude, it.longitude) } }
    LaunchedEffect(loaded, latest) {
        if (loaded) routeView(latest, edge)?.let { camera.move(it) }
    }
    LaunchedEffect(loaded, state.located) {
        val me = state.me?.let { LatLng(it.latitude, it.longitude) }
        if (loaded && me != null) routeView(listOf(me), edge)?.let { camera.animate(it) }
    }
    // What is in sight once the map has stopped: pins are made for that part only.
    var sight by remember { mutableStateOf<Pair<LatLngBounds, Float>?>(null) }
    LaunchedEffect(loaded, camera.isMoving) {
        if (!loaded || camera.isMoving) return@LaunchedEffect
        camera.projection?.visibleRegion?.latLngBounds?.let { sight = it to camera.position.zoom }
    }
    val chipsEnd = top + Spacing.sm + ChipsHeight
    Box(Modifier.fillMaxSize()) {
        WalkMap(
            camera = camera,
            modifier = Modifier.fillMaxSize(),
            covered = PaddingValues(top = chipsEnd),
            onLoaded = { loaded = true },
            onTap = { actions.onPick(null) },
        ) {
            Pins(state, sight, camera, actions.onPick)
        }
        Row(
            modifier = Modifier.padding(start = Spacing.ml, top = top + Spacing.sm, end = Spacing.ml),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            MapFilter.entries.forEach { filter ->
                AppChip(stringResource(filter.label), filter == state.filter, { actions.onFilter(filter) })
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = Spacing.ml, end = Spacing.ml, bottom = CardBottom),
            horizontalAlignment = Alignment.End,
        ) {
            CircleButton(
                iconRes = R.drawable.ic_locate,
                contentDescription = stringResource(R.string.walk_locate),
                onClick = actions.onLocate,
                tint = AppTheme.colors.accent,
                raised = true,
            )
            state.pick?.let { pick ->
                Spacer(Modifier.height(Spacing.m))
                MapPickCard(pick, state.today, onOpen = { actions.onOpen(pick.find) })
            }
        }
    }
}

private val MapFilter.label: Int
    get() = when (this) {
        MapFilter.All -> R.string.gallery_all
        MapFilter.Plants -> R.string.map_plants
        MapFilter.Birds -> R.string.map_birds
        MapFilter.Places -> R.string.map_places
    }

// The finds alone and in clusters, and the walker.
@Composable
@GoogleMapComposable
private fun Pins(
    state: PhotoMapState,
    sight: Pair<LatLngBounds, Float>?,
    camera: CameraPositionState,
    onPick: (FindEntity?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val shown = remember(state.finds, state.filter) { shownFinds(state.finds, state.filter) }
    val marks = remember(shown, sight) {
        val (bounds, zoom) = sight ?: return@remember emptyList()
        mapMarks(shown.filter { bounds.contains(LatLng(it.latitude, it.longitude)) }, zoom)
    }
    marks.forEach { mark ->
        val at = LatLng(mark.latitude, mark.longitude)
        when (mark) {
            is FindMark -> key(mark.find.id) {
                val picked = state.pick?.find?.id == mark.find.id
                MarkerComposable(
                    keys = arrayOf<Any>(mark.find.kind, picked),
                    state = rememberUpdatedMarkerState(at),
                    anchor = Centre,
                    onClick = {
                        onPick(mark.find)
                        true
                    },
                ) { FindPin(mark.find.kind.look(), if (picked) PickedPinSize else PinSize) }
            }
            is ClusterMark -> key(mark.finds.first().id) {
                MarkerComposable(
                    keys = arrayOf(mark.finds.size),
                    state = rememberUpdatedMarkerState(at),
                    anchor = Centre,
                    onClick = {
                        // At the closest zoom a cluster cannot fall apart: its first find is shown.
                        val before = camera.position.zoom
                        scope.launch {
                            camera.animate(CameraUpdateFactory.newLatLngZoom(at, before + ZOOM_STEP))
                            if (camera.position.zoom == before) onPick(mark.finds.first())
                        }
                        true
                    },
                ) { ClusterPin(mark.finds.size) }
            }
        }
    }
    state.me?.let { me ->
        val at = rememberUpdatedMarkerState(LatLng(me.latitude, me.longitude))
        MarkerComposable(state = at, anchor = Centre) { MeDot() }
    }
}
