package com.walkpick.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.walkpick.app.R
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.ui.theme.AppTheme
import java.io.File

/** How the photos of a walk are lined up: the tile, how many fit, and the "+N" over the last one. */
data class StripLook(
    val tile: Dp,
    val count: Int,
    val gap: Dp,
    val shape: Shape,
    val more: TextStyle,
    /** Every n-th pixel of the photo is read: a small tile needs no more. */
    val sample: Int,
)

/** The first photos of a walk; when there are more than fit, the last tile says how many more. */
@Composable
fun PhotoStrip(
    finds: List<FindEntity>,
    photo: suspend (FindEntity) -> File?,
    look: StripLook,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val hidden = (finds.size - look.count).coerceAtLeast(0)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(look.gap)) {
        finds.take(look.count).forEachIndexed { index, find ->
            Box(Modifier.size(look.tile).clip(look.shape), contentAlignment = Alignment.Center) {
                FindPhoto(find, photo, Modifier.fillMaxSize(), look.sample)
                if (hidden > 0 && index == look.count - 1) {
                    Box(Modifier.fillMaxSize().background(colors.scrim))
                    // The tile itself is one of those it counts.
                    val more = stringResource(R.string.diary_more, hidden + 1)
                    Text(more, style = look.more, color = colors.onAccent)
                }
            }
        }
    }
}
