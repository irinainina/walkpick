package com.walkspot.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.walkspot.app.R
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.walk.paceSecondsPerKm
import com.walkspot.app.data.walk.routePlaces
import com.walkspot.app.ui.components.AppButton
import com.walkspot.app.ui.components.AppButtonStyle
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.AppSheet
import com.walkspot.app.ui.components.CircleButton
import com.walkspot.app.ui.components.IconBadge
import com.walkspot.app.ui.components.RouteLine
import com.walkspot.app.ui.components.StartFlag
import com.walkspot.app.ui.components.WalkMap
import com.walkspot.app.ui.components.routeView
import com.walkspot.app.ui.distanceText
import com.walkspot.app.ui.grouped
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.ParagraphStyle
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing
import com.walkspot.app.ui.walk.WalkSummaryState
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val MapHeight = 320.dp

// The card lies over the bottom of the map; the route keeps clear of it and of the round buttons.
private val CardTop = 268.dp
private val MapCoveredTop = 56.dp
private val RouteEdge = 32.dp
private val RouteWidth = 4.dp
private val StartPin = 28.dp
private val CardBottom = 28.dp
private const val SECONDS_IN_MINUTE = 60.0

private val Day = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
private val Clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

@Composable
fun WalkSummaryScreen(
    state: WalkSummaryState,
    onBack: () -> Unit,
    onNote: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val walk = state.walk
    var note by rememberSaveable { mutableStateOf(walk.note.orEmpty()) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    // The note is kept on the way out, whichever way that is.
    val leave = {
        onNote(note)
        onBack()
    }
    BackHandler(onBack = leave)
    Box(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .verticalScroll(rememberScrollState()),
    ) {
        RouteMap(walk)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.ml, vertical = Spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleButton(R.drawable.ic_back, stringResource(R.string.back), leave, raised = true)
            val more = stringResource(R.string.walk_more)
            CircleButton(R.drawable.ic_more, more, { deleting = true }, raised = true)
        }
        Column(
            Modifier
                .padding(start = Spacing.ml, top = CardTop, end = Spacing.ml, bottom = Spacing.l)
                .navigationBarsPadding(),
        ) {
            SummaryCard(state)
            Spacer(Modifier.height(Spacing.l))
            NoteField(note, onChange = { note = it })
            Spacer(Modifier.height(Spacing.l))
            AppButton(stringResource(R.string.walk_done), onClick = leave)
        }
    }
    if (deleting) {
        AppSheet(stringResource(R.string.walk_delete_title), onDismiss = { deleting = false }) {
            val warning = stringResource(R.string.walk_delete_text)
            val cancel = stringResource(R.string.settings_cancel)
            Spacer(Modifier.height(Spacing.s))
            Text(warning, style = ParagraphStyle, color = AppTheme.colors.textSecondary)
            Spacer(Modifier.height(Spacing.l))
            AppButton(stringResource(R.string.walk_delete), onDelete, style = AppButtonStyle.Danger)
            Spacer(Modifier.height(Spacing.sm))
            AppButton(cancel, { deleting = false }, style = AppButtonStyle.Secondary)
        }
    }
}

// The whole route as a picture at the top of the page: the page scrolls, the map does not.
@Composable
private fun RouteMap(walk: WalkEntity) {
    val route = remember(walk.route) { routePlaces(walk.route).map { (lat, lon) -> LatLng(lat, lon) } }
    val camera = rememberCameraPositionState()
    val edge = with(LocalDensity.current) { RouteEdge.roundToPx() }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(route, loaded) {
        if (loaded) routeView(route, edge)?.let { camera.move(it) }
    }
    WalkMap(
        camera = camera,
        modifier = Modifier
            .fillMaxWidth()
            .height(MapHeight),
        covered = PaddingValues(top = MapCoveredTop, bottom = MapHeight - CardTop),
        still = true,
        onLoaded = { loaded = true },
    ) {
        RouteLine(route, RouteWidth)
        route.firstOrNull()?.let { start ->
            MarkerComposable(state = rememberUpdatedMarkerState(start), anchor = Offset(0.5f, 0.5f)) {
                StartFlag(StartPin)
            }
        }
    }
}

@Composable
private fun SummaryCard(state: WalkSummaryState) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    val walk = state.walk
    val zone = ZoneId.systemDefault()
    val (from, to) = walk.startedAt.atZone(zone) to walk.endedAt.atZone(zone)
    AppCard {
        Column(Modifier.padding(Spacing.ml, Spacing.ml, Spacing.ml, CardBottom)) {
            val title = stringResource(R.string.walk_number, walk.number)
            Text(title, style = type.headlineMedium, color = colors.textPrimary)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "${Day.format(from)} · ${Clock.format(from)} – ${Clock.format(to)}",
                style = type.bodyMedium,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(Spacing.l))
            val unit = state.distanceUnit
            val minutes = (walk.durationS / SECONDS_IN_MINUTE).roundToInt()
            val pace = paceSecondsPerKm(walk.distanceM.toDouble(), walk.durationS.toLong())
                ?.let { clockText((it / unit.perMetric).roundToInt().toLong()) }
            Row {
                Stat(R.drawable.ic_shoe, grouped(walk.steps), R.string.walk_steps)
                Stat(R.drawable.ic_route, distanceText(walk.distanceM, unit), R.string.walk_distance)
            }
            Spacer(Modifier.height(Spacing.l))
            Row {
                Stat(R.drawable.ic_clock, stringResource(R.string.walk_minutes, minutes), R.string.walk_time)
                Stat(R.drawable.ic_flame, pace ?: "–", R.string.walk_pace, unit.symbol)
            }
        }
    }
}

@Composable
private fun RowScope.Stat(
    @DrawableRes iconRes: Int,
    value: String,
    @StringRes labelRes: Int,
    labelUnit: String = "",
) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(iconRes, colors.surfaceTint, colors.accent, Sizes.iconSmall)
        Column {
            Text(value, style = type.titleMedium, color = colors.textPrimary, maxLines = 1)
            Text(stringResource(labelRes, labelUnit), style = type.bodySmall, color = colors.textSecondary)
        }
    }
}

// A few lines about the walk; grows with the text.
@Composable
private fun NoteField(note: String, onChange: (String) -> Unit) {
    val colors = AppTheme.colors
    val shape = MaterialTheme.shapes.medium
    BasicTextField(
        value = note,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = ParagraphStyle.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.accent),
        decorationBox = { field ->
            Box(
                modifier = Modifier
                    .heightIn(min = Sizes.control)
                    .background(colors.surface, shape)
                    .border(Sizes.hairline, colors.divider, shape)
                    .padding(Spacing.m),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (note.isEmpty()) {
                    val hint = stringResource(R.string.walk_note_hint)
                    Text(hint, style = ParagraphStyle, color = colors.textSecondary)
                }
                field()
            }
        },
    )
}
