package com.walkpick.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.FindKind
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.IconBadge
import com.walkpick.app.ui.components.PhotoBox
import com.walkpick.app.ui.components.RowChevron
import com.walkpick.app.ui.find.look
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing
import com.walkpick.app.ui.walk.WalkSummaryState
import kotlin.math.roundToInt

private val PhotoSize = 96.dp
private val RowHeight = 64.dp

// A thumbnail of 96 dp needs a quarter of the pixels of the photo at most.
private const val THUMB_SAMPLE = 4
private const val PERCENT = 100

/** The photos of the walk in a strip and its finds in a list; both lead to the card of a find. */
@Composable
internal fun WalkFinds(state: WalkSummaryState, onFind: (String) -> Unit) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    val finds = state.finds
    if (finds.isEmpty()) return
    val title = stringResource(R.string.walk_photos, finds.size)
    Text(title, style = type.titleSmall, color = colors.textPrimary)
    Spacer(Modifier.height(Spacing.sm))
    Row(
        modifier = Modifier
            .bleed(Spacing.ml)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.ml),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        finds.forEach { find ->
            val shape = MaterialTheme.shapes.medium
            val photo = Modifier
                .size(PhotoSize)
                .clip(shape)
                .clickable { onFind(find.id) }
            PhotoBox(state.photos[find.id], photo, THUMB_SAMPLE)
        }
    }
    Spacer(Modifier.height(Spacing.l))
    Text(stringResource(R.string.walk_finds), style = type.titleSmall, color = colors.textPrimary)
    Spacer(Modifier.height(Spacing.sm))
    AppCard {
        finds.forEachIndexed { index, find -> FindRow(find, divider = index < finds.lastIndex, onFind) }
    }
}

@Composable
private fun FindRow(find: FindEntity, divider: Boolean, onFind: (String) -> Unit) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    val look = find.kind.look()
    Column(Modifier.clickable { onFind(find.id) }) {
        Row(
            modifier = Modifier
                .height(RowHeight)
                .padding(horizontal = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(look.icon, look.tint, look.color)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(findTitle(find), style = type.labelLarge, color = colors.textPrimary, maxLines = 1)
                Spacer(Modifier.height(Spacing.xs / 2))
                Text(findNote(find), style = type.bodySmall, color = colors.textSecondary, maxLines = 1)
            }
            RowChevron()
        }
        if (divider) {
            Box(
                Modifier
                    .padding(start = Spacing.m + Sizes.circle + Spacing.sm, end = Spacing.m)
                    .fillMaxWidth()
                    .height(Sizes.hairline)
                    .background(colors.divider),
            )
        }
    }
}

// Lets the strip run to the edges of the screen from inside a column that keeps [side] clear.
private fun Modifier.bleed(side: Dp) = layout { measurable, constraints ->
    val wider = constraints.copy(maxWidth = constraints.maxWidth + side.roundToPx() * 2)
    val strip = measurable.measure(wider)
    layout(constraints.maxWidth, strip.height) { strip.place(-side.roundToPx(), 0) }
}

// A service less sure than this has only made a guess, and the name says so.
private const val SURE_FROM = 0.5

/** What a find is called where there is one line for it. */
@Composable
internal fun findTitle(find: FindEntity): String = when {
    find.name != null && find.match != null && find.match < SURE_FROM ->
        stringResource(R.string.find_possibly, find.name)
    find.name != null -> find.name
    find.kind == FindKind.PHOTO -> stringResource(R.string.find_photo)
    find.kind == FindKind.UNKNOWN -> stringResource(R.string.find_unknown_title)
    else -> stringResource(R.string.find_waiting_title)
}

// The Latin name and how sure the service is, or the kind where there is neither.
@Composable
private fun findNote(find: FindEntity): String {
    val sure = find.match?.let { stringResource(R.string.find_percent, (it * PERCENT).roundToInt()) }
    val parts = listOfNotNull(find.latinName, sure)
    return if (parts.isEmpty()) stringResource(find.kind.look().label) else parts.joinToString(" · ")
}
