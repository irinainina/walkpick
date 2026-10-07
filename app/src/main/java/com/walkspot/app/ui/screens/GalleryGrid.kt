package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.ui.components.PhotoBox
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing
import java.io.File

private val PickedBorder = 3.dp
private val CheckBorder = 2.dp
private const val CHECK_ALPHA = 0.6f

// A tile is a third or a half of the screen wide: a quarter of the pixels of the photo is plenty.
private const val THUMB_SAMPLE = 4

/** What the tiles of a grid do and how they look: the same for every tile of a tab. */
internal class TileLook(
    val shape: Shape,
    /** The white circle of the star and the star in it. */
    val badge: Dp,
    val star: Dp,
    val inset: Dp,
    val photo: suspend (FindEntity) -> File?,
    val onTap: (FindEntity) -> Unit,
    val onHold: (FindEntity) -> Unit,
)

/** One row of the grid: [columns] tiles wide, the missing ones left empty. */
@Composable
internal fun TileRow(finds: List<FindEntity>, columns: Int, gap: Dp, selected: Set<String>?, look: TileLook) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        finds.forEach { Tile(it, selected, look) }
        repeat(columns - finds.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun RowScope.Tile(find: FindEntity, selected: Set<String>?, look: TileLook) {
    val colors = AppTheme.colors
    val picked = selected?.contains(find.id) == true
    val file by produceState<File?>(null, find.id) { value = look.photo(find) }
    val frame = if (picked) Modifier.border(PickedBorder, colors.accent, look.shape) else Modifier
    Box(
        Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(look.shape)
            .combinedClickable(onClick = { look.onTap(find) }, onLongClick = { look.onHold(find) }),
    ) {
        PhotoBox(file, Modifier.fillMaxSize(), THUMB_SAMPLE)
        Box(Modifier.fillMaxSize().then(frame))
        if (find.isBest) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(look.inset)
                    .size(look.badge)
                    .background(colors.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                val star = painterResource(R.drawable.ic_star_filled)
                Icon(star, null, Modifier.size(look.star), colors.streak)
            }
        }
        if (selected != null) Check(picked, Modifier.align(Alignment.TopStart).padding(Spacing.s))
    }
}

// The round mark of the selection mode: empty on every photo, filled on the picked ones.
@Composable
private fun Check(picked: Boolean, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val ground = if (picked) colors.accent else colors.surface.copy(alpha = CHECK_ALPHA)
    Box(
        modifier = modifier
            .size(Sizes.icon)
            .background(ground, CircleShape)
            .border(CheckBorder, colors.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (picked) {
            val check = painterResource(R.drawable.ic_check)
            Icon(check, null, Modifier.size(Spacing.m), colors.onAccent)
        }
    }
}
