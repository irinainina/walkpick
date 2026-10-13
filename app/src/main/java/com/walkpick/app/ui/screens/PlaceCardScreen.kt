package com.walkpick.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.walkpick.app.R
import com.walkpick.app.data.local.FindKind
import com.walkpick.app.ui.components.CircleButton
import com.walkpick.app.ui.components.FindPin
import com.walkpick.app.ui.components.MeDot
import com.walkpick.app.ui.components.PhotoBox
import com.walkpick.app.ui.components.WalkMap
import com.walkpick.app.ui.components.routeView
import com.walkpick.app.ui.distanceText
import com.walkpick.app.ui.find.PlaceCardState
import com.walkpick.app.ui.find.look
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.ParagraphStyle
import com.walkpick.app.ui.theme.SheetShape
import com.walkpick.app.ui.theme.Spacing

private val PhotoHeight = 380.dp
private val SheetOverlap = 32.dp
private val MapHeight = 140.dp
private val MapEdge = 40.dp

// Further than this the walker is not shown on the little map: the place would shrink to a dot.
private const val ME_ON_MAP_M = 3_000

/** A place from Wikipedia near the walker: what it is, how far, where. Nothing here can be changed. */
@Composable
fun PlaceCardScreen(state: PlaceCardState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val type = MaterialTheme.typography
    Box(
        modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        PhotoBox(state.photo, Modifier.fillMaxWidth().height(PhotoHeight))
        CircleButton(
            iconRes = R.drawable.ic_back,
            contentDescription = stringResource(R.string.back),
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = Spacing.ml, vertical = Spacing.m),
            raised = true,
        )
        Column(
            Modifier
                .padding(top = PhotoHeight - SheetOverlap)
                .fillMaxWidth()
                .background(colors.surface, SheetShape)
                .navigationBarsPadding()
                .padding(start = Spacing.ml, top = Spacing.l, end = Spacing.ml, bottom = Spacing.l),
        ) {
            KindChip(FindKind.PLACE)
            Spacer(Modifier.height(Spacing.sm))
            Text(state.title, style = type.headlineLarge, color = colors.textPrimary)
            val away = state.away?.let { distanceText(it, state.distanceUnit) }
            val kind = stringResource(R.string.place_landmark)
            val under = if (away == null) kind else stringResource(R.string.place_away, kind, away)
            Text(under, style = ParagraphStyle, color = colors.textSecondary)
            Spacer(Modifier.height(Spacing.sm))
            val pictured = state.photo != null
            val note = if (pictured) R.string.place_not_visited_photo else R.string.place_not_visited
            Text(stringResource(note), style = type.bodyMedium, color = colors.textSecondary)
            if (!state.description.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(state.description, style = ParagraphStyle, color = colors.textPrimary)
            }
            // The link brings its own gap of 24; the design has 16 here.
            state.url?.let { WikipediaLink(it, Spacing.m) }
            Spacer(Modifier.height(Spacing.l))
            Text(stringResource(R.string.find_where), style = type.titleSmall, color = colors.textPrimary)
            Spacer(Modifier.height(Spacing.sm))
            PlaceMap(state)
        }
    }
}

// A still map with the place and, when it is near enough, the walker.
@Composable
private fun PlaceMap(state: PlaceCardState) {
    val place = LatLng(state.latitude, state.longitude)
    val near = (state.away ?: Int.MAX_VALUE) <= ME_ON_MAP_M
    val me = state.me?.takeIf { near }?.let { LatLng(it.latitude, it.longitude) }
    val camera = rememberCameraPositionState()
    val edge = with(LocalDensity.current) { MapEdge.roundToPx() }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(loaded, place, me) {
        if (loaded) routeView(listOfNotNull(place, me), edge)?.let { camera.move(it) }
    }
    WalkMap(
        camera = camera,
        modifier = Modifier
            .fillMaxWidth()
            .height(MapHeight)
            .clip(MaterialTheme.shapes.medium),
        still = true,
        onLoaded = { loaded = true },
    ) {
        val centre = Offset(0.5f, 0.5f)
        MarkerComposable(state = rememberUpdatedMarkerState(place), anchor = centre) {
            FindPin(FindKind.PLACE.look())
        }
        if (me != null) {
            MarkerComposable(state = rememberUpdatedMarkerState(me), anchor = centre) { MeDot() }
        }
    }
}
