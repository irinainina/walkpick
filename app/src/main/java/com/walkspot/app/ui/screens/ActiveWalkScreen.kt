package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.walkspot.app.R
import com.walkspot.app.ui.components.AppButton
import com.walkspot.app.ui.components.AppButtonStyle
import com.walkspot.app.ui.components.AppSheet
import com.walkspot.app.ui.components.CircleButton
import com.walkspot.app.ui.components.MeDot
import com.walkspot.app.ui.components.RouteLine
import com.walkspot.app.ui.components.StartFlag
import com.walkspot.app.ui.components.WalkMap
import com.walkspot.app.ui.components.placeView
import com.walkspot.app.ui.components.routeView
import com.walkspot.app.ui.distanceText
import com.walkspot.app.ui.theme.AppShadows
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.CardShape
import com.walkspot.app.ui.theme.PillShape
import com.walkspot.app.ui.theme.Spacing
import com.walkspot.app.ui.walk.ActiveWalkState
import kotlinx.coroutines.launch

private val TimerWidth = 200.dp
private val TimerHeight = 72.dp
private val RouteWidth = 6.dp

// The timer with the plate under it and the bottom panel cover this much of the map; the route keeps
// this far from what is left, so that the Start label under its pin stays in sight.
private val CoveredTop = 184.dp
private val CoveredBottom = 264.dp
private val RouteEdge = 56.dp
private val StartPin = 32.dp
private val StartLabelWidth = 56.dp
private val SmallIcon = 16.dp

// The walk has left its start once it is this far from it; closer, "back to start" says nothing.
private const val AWAY_FROM_START_M = 50.0

@Composable
fun ActiveWalkScreen(
    state: ActiveWalkState,
    onMinimize: () -> Unit,
    onPause: () -> Unit,
    onCamera: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    /** Where the map waits for the first signal; null leaves it where Google Maps opens. */
    waitAt: LatLng? = null,
) {
    val camera = rememberCameraPositionState()
    val scope = rememberCoroutineScope()
    val route = state.route.map { LatLng(it.latitude, it.longitude) }
    val me = route.lastOrNull()
    LaunchedEffect(waitAt) { if (me == null && waitAt != null) camera.position = placeView(waitAt) }
    // The map follows the walker until it is dragged aside; the locate button brings it back.
    var following by remember { mutableStateOf(true) }
    val moved = camera.cameraMoveStartedReason
    LaunchedEffect(moved) { if (moved == CameraMoveStartedReason.GESTURE) following = false }
    val edge = with(LocalDensity.current) { RouteEdge.roundToPx() }
    // The start stays in the frame: the map shows the whole route, however far the walk has gone.
    val view = { routeView(route, edge) }
    var loaded by remember { mutableStateOf(false) }
    // A walk is not ended by a slip of the finger: Stop asks first.
    var stopping by remember { mutableStateOf(false) }
    LaunchedEffect(me, following, loaded) {
        if (loaded && following) view()?.let { camera.animate(it) }
    }

    Box(modifier.fillMaxSize()) {
        val covered = PaddingValues(top = CoveredTop, bottom = CoveredBottom)
        WalkMap(camera, Modifier.fillMaxSize(), covered, onLoaded = { loaded = true }) { Route(route) }
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = Spacing.m),
        ) {
            CircleButton(
                iconRes = R.drawable.ic_chevron_down,
                contentDescription = stringResource(R.string.walk_minimize),
                onClick = onMinimize,
                modifier = Modifier.padding(start = Spacing.ml, top = Spacing.m),
                raised = true,
            )
            Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                Timer(state)
                val away = state.toStartM?.takeIf { it >= AWAY_FROM_START_M }
                if (away != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    BackToStart(distanceText(away.toInt(), state.distanceUnit))
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.End) {
            CircleButton(
                iconRes = R.drawable.ic_locate,
                contentDescription = stringResource(R.string.walk_locate),
                onClick = {
                    following = true
                    if (loaded) view()?.let { scope.launch { camera.animate(it) } }
                },
                modifier = Modifier.padding(end = Spacing.ml, bottom = Spacing.ml),
                tint = AppTheme.colors.accent,
                raised = true,
            )
            ActiveWalkPanel(state, onPause, onCamera, onStop = { stopping = true })
        }
    }
    if (stopping) FinishWalkSheet(onFinish = onStop, onDismiss = { stopping = false })
}

@Composable
internal fun FinishWalkSheet(onFinish: () -> Unit, onDismiss: () -> Unit) {
    AppSheet(stringResource(R.string.walk_finish_title), onDismiss) {
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = stringResource(R.string.walk_finish_text),
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.l))
        AppButton(stringResource(R.string.walk_finish), onClick = onFinish)
        Spacer(Modifier.height(Spacing.sm))
        AppButton(stringResource(R.string.walk_keep_walking), onDismiss, style = AppButtonStyle.Secondary)
    }
}

/** Offered once on phones that take the step sensor away from an app behind a dark screen. */
@Composable
internal fun BatterySheet(onOpen: () -> Unit, onDismiss: () -> Unit) {
    AppSheet(stringResource(R.string.walk_battery_title), onDismiss) {
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = stringResource(R.string.walk_battery_text),
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.l))
        AppButton(stringResource(R.string.walk_battery_open), onClick = onOpen)
        Spacer(Modifier.height(Spacing.sm))
        AppButton(stringResource(R.string.walk_battery_later), onDismiss, style = AppButtonStyle.Secondary)
    }
}

// The route in the gradient of the app, the start under its flag and the walker at the end of the line.
@Composable
private fun Route(route: List<LatLng>) {
    val colors = AppTheme.colors
    RouteLine(route, RouteWidth)
    val centre = Offset(0.5f, 0.5f)
    route.firstOrNull()?.let { start ->
        // The marker is anchored at the middle of the pin, the label hangs under it.
        val pinMiddle = Offset(0.5f, StartPin / 2 / (StartPin + Spacing.xs + Spacing.l))
        MarkerComposable(state = rememberUpdatedMarkerState(start), anchor = pinMiddle) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StartFlag(StartPin)
                Spacer(Modifier.height(Spacing.xs))
                Box(
                    Modifier
                        .size(StartLabelWidth, Spacing.l)
                        .background(colors.accent, PillShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val label = stringResource(R.string.walk_start)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onAccent)
                }
            }
        }
    }
    route.lastOrNull()?.let { me ->
        MarkerComposable(state = rememberUpdatedMarkerState(me), anchor = centre) { MeDot() }
    }
}

@Composable
private fun Timer(state: ActiveWalkState) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .size(TimerWidth, TimerHeight)
            .dropShadow(CardShape, AppShadows.card)
            .background(colors.surface, CardShape)
            .padding(top = Spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(clockText(state.seconds), style = type.displaySmall, color = colors.textPrimary)
        val (status, live) = when {
            state.paused -> R.string.walk_paused to false
            state.route.isEmpty() -> R.string.walk_finding to false
            else -> R.string.walk_walking to true
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s - Spacing.xs / 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dot = if (live) colors.positive else colors.textSecondary
            Box(Modifier.size(Spacing.s).background(dot, CircleShape))
            Text(stringResource(status), style = type.bodyMedium, color = colors.textSecondary)
        }
    }
}

@Composable
private fun BackToStart(distance: String) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .height(Spacing.xl)
            .dropShadow(PillShape, AppShadows.circleButton)
            .background(colors.surface, PillShape)
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_flag), null, Modifier.size(SmallIcon), colors.accent)
        Spacer(Modifier.width(Spacing.s - Spacing.xs / 2))
        Text(
            text = stringResource(R.string.walk_back_to_start, distance),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
        )
    }
}
