package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.walkspot.app.R
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.data.walk.title
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.FindPin
import com.walkspot.app.ui.components.IconBadge
import com.walkspot.app.ui.components.RowChevron
import com.walkspot.app.ui.components.WalkMap
import com.walkspot.app.ui.components.routeView
import com.walkspot.app.ui.find.FindCardState
import com.walkspot.app.ui.find.look
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.ParagraphStyle
import com.walkspot.app.ui.theme.PillShape
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val ChipHeight = 28.dp
private val ChipStart = 10.dp
private val ChipIcon = 16.dp
private val ChipGap = 6.dp
private val MatchTrack = 8.dp
private val MapHeight = 140.dp
private const val PERCENT = 100
private val Taken = DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm", Locale.ENGLISH)

/** Everything under the photo of a find. */
@Composable
internal fun FindCardBody(
    state: FindCardState,
    onWalk: (String) -> Unit,
    onNotThis: () -> Unit,
    onName: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val find = state.find
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    Column(modifier.padding(start = Spacing.ml, top = Spacing.l, end = Spacing.ml, bottom = Spacing.l)) {
        KindChip(find.kind)
        Spacer(Modifier.height(Spacing.sm))
        val (name, under) = when {
            find.name != null -> find.name to find.latinName
            find.kind == FindKind.UNKNOWN -> {
                stringResource(R.string.find_unknown_title) to stringResource(R.string.find_unknown_text)
            }
            else -> stringResource(R.string.find_waiting_title) to stringResource(R.string.find_waiting_text)
        }
        Text(name, style = type.headlineLarge, color = colors.textPrimary)
        if (under != null) Text(under, style = ParagraphStyle, color = colors.textSecondary)
        if (find.name != null) {
            Match(find.match, onNotThis)
        } else {
            UnnamedControls(unknown = find.kind == FindKind.UNKNOWN, onName, onRetry)
        }
        if (!find.description.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.ml))
            Text(find.description, style = ParagraphStyle, color = colors.textPrimary)
        }
        find.wikipediaUrl?.let { WikipediaLink(it) }
        Spacer(Modifier.height(Spacing.l))
        Text(stringResource(R.string.find_where), style = type.titleSmall, color = colors.textPrimary)
        Spacer(Modifier.height(Spacing.sm))
        Where(find)
        Spacer(Modifier.height(Spacing.ml))
        WalkRow(state, onWalk)
    }
}

@Composable
internal fun KindChip(kind: FindKind) {
    val look = kind.look()
    Row(
        modifier = Modifier
            .height(ChipHeight)
            .background(look.tint, PillShape)
            .padding(start = ChipStart, end = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(look.icon), null, Modifier.size(ChipIcon), look.color)
        Spacer(Modifier.width(ChipGap))
        Text(stringResource(look.label), style = MaterialTheme.typography.labelMedium, color = look.color)
    }
}

// How sure the service is, as a number and as a bar, and the way to another name. A place and a
// name typed by hand have no match: only the link is left of the row.
@Composable
private fun Match(match: Float?, onNotThis: () -> Unit) {
    val colors = AppTheme.colors
    val type = MaterialTheme.typography
    Spacer(Modifier.height(Spacing.sm))
    Row {
        if (match != null) {
            Text(stringResource(R.string.find_match), style = type.bodyMedium, color = colors.textSecondary)
            Spacer(Modifier.width(Spacing.s))
            val percent = stringResource(R.string.find_percent, (match * PERCENT).roundToInt())
            Text(percent, style = type.labelMedium, color = colors.positive)
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.find_not_this),
            modifier = Modifier.clickable(onClick = onNotThis),
            style = type.labelMedium,
            color = colors.accent,
        )
    }
    if (match == null) return
    Spacer(Modifier.height(Spacing.xs))
    Box(
        Modifier
            .fillMaxWidth()
            .height(MatchTrack)
            .background(colors.surfaceTint, PillShape),
    ) {
        val bar = Brush.horizontalGradient(listOf(colors.positive, colors.accentLight))
        Box(
            Modifier
                .fillMaxWidth(match.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(bar, PillShape),
        )
    }
}

@Composable
internal fun WikipediaLink(url: String, above: Dp = Spacing.l) {
    val colors = AppTheme.colors
    val links = LocalUriHandler.current
    Spacer(Modifier.height(above))
    val row = Modifier
        .height(Spacing.l)
        .clickable { links.openUri(url) }
    Row(row, verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_globe), null, Modifier.size(Sizes.iconSmall), colors.accent)
        Spacer(Modifier.width(Spacing.s))
        Text(
            text = stringResource(R.string.find_wikipedia),
            style = MaterialTheme.typography.labelLarge,
            color = colors.accent,
        )
    }
}

// A still map with the pin of the find where the photo was taken.
@Composable
private fun Where(find: FindEntity) {
    val place = LatLng(find.latitude, find.longitude)
    val camera = rememberCameraPositionState()
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(loaded, place) {
        if (loaded) routeView(listOf(place), 0)?.let { camera.move(it) }
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
        MarkerComposable(state = rememberUpdatedMarkerState(place), anchor = Offset(0.5f, 0.5f)) {
            FindPin(find.kind.look())
        }
    }
}

// The walk the find belongs to and when the photo was taken; a saved walk opens on a tap.
@Composable
private fun WalkRow(state: FindCardState, onWalk: (String) -> Unit) {
    val colors = AppTheme.colors
    val type = MaterialTheme.typography
    val walk = state.walk
    AppCard {
        Row(
            modifier = Modifier
                .clickable(enabled = walk != null) { walk?.let { onWalk(it.id) } }
                .padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(R.drawable.ic_route, colors.surfaceTint, colors.accent, Sizes.iconSmall)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                val name = walk?.title() ?: stringResource(R.string.find_walk_going_on)
                Text(name, style = type.labelLarge, color = colors.textPrimary, maxLines = 1)
                val taken = Taken.format(state.find.takenAt.atZone(ZoneId.systemDefault()))
                Text(taken, style = type.bodySmall, color = colors.textSecondary)
            }
            if (walk != null) RowChevron()
        }
    }
}
