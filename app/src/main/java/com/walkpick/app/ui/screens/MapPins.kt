package com.walkpick.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.FindKind
import com.walkpick.app.data.places.Place
import com.walkpick.app.ui.components.ClusterPin
import com.walkpick.app.ui.components.FindPin
import com.walkpick.app.ui.components.MeDot
import com.walkpick.app.ui.components.UnvisitedPlacePin
import com.walkpick.app.ui.find.look
import com.walkpick.app.ui.map.ClusterMark
import com.walkpick.app.ui.map.FindMark
import com.walkpick.app.ui.map.MapFilter
import com.walkpick.app.ui.map.MapMark
import com.walkpick.app.ui.map.MapPick
import com.walkpick.app.ui.map.MapState
import kotlinx.coroutines.launch

private val PinSize = 36.dp
private val PickedPinSize = 48.dp

// A tap on a cluster comes this much closer; the cluster falls apart on the way.
private const val ZOOM_STEP = 2f
private val Centre = Offset(0.5f, 0.5f)

/** The pins of the map: finds alone and in clusters, the places of Wikipedia, the walker. */
@Composable
@GoogleMapComposable
internal fun MapPins(
    state: MapState,
    marks: List<MapMark>,
    camera: CameraPositionState,
    onPick: (MapPick?) -> Unit,
) {
    val scope = rememberCoroutineScope()
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
                        onPick(mark.find.pick())
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
                        val closer = camera.position.zoom + ZOOM_STEP
                        val before = camera.position.zoom
                        scope.launch {
                            camera.animate(CameraUpdateFactory.newLatLngZoom(at, closer))
                            if (camera.position.zoom == before) onPick(mark.finds.first().pick())
                        }
                        true
                    },
                ) { ClusterPin(mark.finds.size) }
            }
        }
    }
    // The places nobody has photographed yet are pale; they are seen under All and Places.
    if (state.filter == MapFilter.All || state.filter == MapFilter.Places) {
        state.places.forEach { place ->
            key(place.id) {
                val picked = state.pick?.place?.id == place.id
                MarkerComposable(
                    keys = arrayOf(picked),
                    state = rememberUpdatedMarkerState(LatLng(place.latitude, place.longitude)),
                    anchor = Centre,
                    onClick = {
                        onPick(place.pick())
                        true
                    },
                ) { UnvisitedPlacePin(if (picked) PickedPinSize else PinSize) }
            }
        }
    }
    state.me?.let { me ->
        val at = LatLng(me.latitude, me.longitude)
        MarkerComposable(state = rememberUpdatedMarkerState(at), anchor = Centre) { MeDot() }
    }
}

private fun FindEntity.pick() = MapPick(
    kind = kind,
    title = name,
    latitude = latitude,
    longitude = longitude,
    find = this,
    text = description?.ifBlank { null } ?: latinName,
)

private fun Place.pick() =
    MapPick(kind = FindKind.PLACE, title = title, latitude = latitude, longitude = longitude, place = this)
