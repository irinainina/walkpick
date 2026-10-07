package com.walkspot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.gms.maps.model.StrokeStyle
import com.google.android.gms.maps.model.StyleSpan
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.walkspot.app.R
import com.walkspot.app.ui.find.KindLook
import com.walkspot.app.ui.theme.AppTheme

// Closer than this a street is a blank sheet; a route of a few metres would zoom that far.
private const val CLOSEST_ZOOM = 17f
private const val ONE_POINT_ZOOM = 16f
private val StartBorder = 3.dp
private val FlagSize = 16.dp
private val PlacePin = 36.dp
private val ClusterSize = 40.dp
private val PlaceIcon = 20.dp
private val MeHalo = 48.dp
private val MeSize = 20.dp
private const val HALO_ALPHA = 0.24f

// The screens draw their own buttons over the map.
private val BareMap = MapUiSettings(
    compassEnabled = false,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    zoomControlsEnabled = false,
)

// A map that is a picture: it lies inside a page that scrolls.
private val StillMap = BareMap.copy(
    rotationGesturesEnabled = false,
    scrollGesturesEnabled = false,
    tiltGesturesEnabled = false,
    zoomGesturesEnabled = false,
)

/** What is drawn on the map: lines and markers, not ordinary composables. */
typealias MapContent =
    @Composable
    @GoogleMapComposable
    () -> Unit

/** Google Map in the colours of the design; routes and pins go into [content]. */
@Composable
fun WalkMap(
    camera: CameraPositionState,
    modifier: Modifier = Modifier,
    // What the screen lays over the map: the camera keeps its subject clear of it.
    covered: PaddingValues = PaddingValues(),
    still: Boolean = false,
    onLoaded: () -> Unit = {},
    /** The map is tapped where there is no pin. */
    onTap: () -> Unit = {},
    content: MapContent = {},
) {
    val context = LocalContext.current
    val style = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style) }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        properties = MapProperties(mapStyleOptions = style, maxZoomPreference = CLOSEST_ZOOM),
        uiSettings = if (still) StillMap else BareMap,
        contentPadding = covered,
        onMapLoaded = onLoaded,
        onMapClick = { onTap() },
        content = content,
    )
}

/**
 * The camera move that shows the whole [route], [edge] pixels clear of the sides; null without a
 * route. Call it only after the map is loaded: before that Google Maps cannot build a move.
 */
fun routeView(route: List<LatLng>, edge: Int): CameraUpdate? = when {
    route.size > 1 -> {
        val bounds = LatLngBounds.builder().apply { route.forEach(::include) }.build()
        CameraUpdateFactory.newLatLngBounds(bounds, edge)
    }
    else -> route.firstOrNull()?.let { CameraUpdateFactory.newLatLngZoom(it, ONE_POINT_ZOOM) }
}

/** The line of a walk in the gradient of the app; goes into the content of [WalkMap]. */
@Composable
fun RouteLine(route: List<LatLng>, width: Dp) {
    if (route.size < 2) return
    val colors = AppTheme.colors
    val (from, to) = colors.accent.toArgb() to colors.accentLight.toArgb()
    Polyline(
        points = route,
        spans = listOf(StyleSpan(StrokeStyle.gradientBuilder(from, to).build(), (route.size - 1).toDouble())),
        width = with(LocalDensity.current) { width.toPx() },
        jointType = JointType.ROUND,
        startCap = RoundCap(),
        endCap = RoundCap(),
    )
}

/** An older route: one colour, paler the older it is. */
@Composable
fun PaleRouteLine(route: List<LatLng>, width: Dp, alpha: Float) {
    if (route.size < 2) return
    Polyline(
        points = route,
        color = AppTheme.colors.accent.copy(alpha = alpha),
        width = with(LocalDensity.current) { width.toPx() },
        jointType = JointType.ROUND,
    )
}

/** Where the phone is: a dot in a halo; the content of a map marker. */
@Composable
fun MeDot() {
    val colors = AppTheme.colors
    val halo = colors.accentLight.copy(alpha = HALO_ALPHA)
    Box(Modifier.size(MeHalo).background(halo, CircleShape), Alignment.Center) {
        Box(
            Modifier
                .size(MeSize)
                .background(colors.accent, CircleShape)
                .border(StartBorder, colors.surface, CircleShape),
        )
    }
}

/** A place from Wikipedia not visited yet: pale beside the finds; the content of a map marker. */
@Composable
fun UnvisitedPlacePin(size: Dp = PlacePin) {
    val colors = AppTheme.colors
    Box(
        Modifier
            .size(size)
            .background(colors.surface, CircleShape)
            .border(StartBorder, colors.unvisitedRing, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_globe), null, Modifier.size(PlaceIcon), colors.unvisited)
    }
}

/** A find on a map: a round pin in the colour of its kind with the icon of the kind. */
@Composable
fun FindPin(look: KindLook, size: Dp = PlacePin) {
    Box(
        Modifier
            .size(size)
            .background(look.color, CircleShape)
            .border(StartBorder, AppTheme.colors.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(look.icon), null, Modifier.size(PlaceIcon), AppTheme.colors.onAccent)
    }
}

/** Several finds too close to tell apart: a white circle with their number. */
@Composable
fun ClusterPin(count: Int) {
    val colors = AppTheme.colors
    Box(
        Modifier
            .size(ClusterSize)
            .background(colors.surface, CircleShape)
            .border(StartBorder, colors.accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(count.toString(), style = MaterialTheme.typography.labelLarge, color = colors.accent)
    }
}

/** The round flag that marks where a walk began; the content of a map marker. */
@Composable
fun StartFlag(size: Dp) {
    val colors = AppTheme.colors
    Box(
        Modifier
            .size(size)
            .background(colors.surface, CircleShape)
            .border(StartBorder, colors.accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_flag), null, Modifier.size(FlagSize), colors.accent)
    }
}
