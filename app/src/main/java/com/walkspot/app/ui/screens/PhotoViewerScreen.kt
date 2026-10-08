package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.walk.title
import com.walkspot.app.ui.components.CircleButton
import com.walkspot.app.ui.components.DarkSystemBars
import com.walkspot.app.ui.components.PhotoBox
import com.walkspot.app.ui.find.PhotoViewerState
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Dot = 6.dp
private val DotGap = 3.dp
private val CaptionHeight = 64.dp
private const val DIM_DOT = 0.4f

// More dots than this would not fit between the edges of the screen as a hint: the counter is enough.
private const val MOST_DOTS = 12
private const val CLOSE_UP = 2.5f
private const val MOST_ZOOM = 5f
private val Taken = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH)

/** The photos of a walk one at a time on a dark ground: swiped sideways, pinched to look closer. */
@Composable
fun PhotoViewerScreen(
    state: PhotoViewerState,
    photo: suspend (FindEntity) -> File?,
    onBack: () -> Unit,
    onBest: (FindEntity) -> Unit,
    onFind: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DarkSystemBars()
    val colors = AppTheme.colors
    val pager = rememberPagerState(initialPage = state.start) { state.finds.size }
    // The photo in front may be gone for an instant after a deletion elsewhere.
    val find = state.finds.getOrNull(pager.currentPage) ?: return
    Box(
        modifier
            .fillMaxSize()
            .background(colors.photoGround),
    ) {
        HorizontalPager(pager, Modifier.fillMaxSize(), key = { state.finds[it].id }) { page ->
            ZoomablePhoto(state.finds[page], photo)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.ml, vertical = Spacing.m),
        ) {
            CircleButton(
                iconRes = R.drawable.ic_back,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
                tint = colors.onAccent,
                ground = colors.photoChrome,
            )
            Text(
                text = stringResource(R.string.viewer_count, pager.currentPage + 1, state.finds.size),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onAccent,
                textAlign = TextAlign.Center,
            )
            CircleButton(
                iconRes = if (find.isBest) R.drawable.ic_star_filled else R.drawable.ic_star,
                contentDescription = stringResource(
                    if (find.isBest) R.string.find_not_best else R.string.find_best,
                ),
                onClick = { onBest(find) },
                modifier = Modifier.align(Alignment.CenterEnd),
                tint = colors.streak,
                ground = colors.photoChrome,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = Spacing.ml, end = Spacing.ml, bottom = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.finds.size in 2..MOST_DOTS) {
                Row(horizontalArrangement = Arrangement.spacedBy(DotGap)) {
                    state.finds.indices.forEach { index ->
                        val alpha = if (index == pager.currentPage) 1f else DIM_DOT
                        Box(Modifier.size(Dot).background(colors.onAccent.copy(alpha = alpha), CircleShape))
                    }
                }
                Spacer(Modifier.height(Spacing.xl - Dot))
            }
            Caption(find, state.walk?.title(), onFind)
        }
    }
}

// What the photo shows, its walk and when it was taken; a tap opens the card of the find.
@Composable
private fun Caption(find: FindEntity, walk: String?, onFind: (String) -> Unit) {
    val colors = AppTheme.colors
    val type = MaterialTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CaptionHeight)
            .clip(MaterialTheme.shapes.large)
            .background(colors.photoChrome)
            .clickable { onFind(find.id) }
            .padding(horizontal = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(findTitle(find), style = type.labelLarge, color = colors.onAccent, maxLines = 1)
            Spacer(Modifier.height(Spacing.xs / 2))
            val taken = Taken.format(find.takenAt.atZone(ZoneId.systemDefault()))
            val under = listOfNotNull(walk, taken).joinToString(" · ")
            Text(under, style = type.bodySmall, color = colors.textSecondary, maxLines = 1)
        }
        val chevron = painterResource(R.drawable.ic_chevron_right)
        Icon(chevron, null, Modifier.size(Sizes.iconSmall), colors.onAccent)
    }
}

// One photo, whole on the screen. Pinching looks closer and a double tap looks close or steps back;
// while the photo is larger than the screen a drag moves it instead of turning the page.
@Composable
private fun ZoomablePhoto(find: FindEntity, photo: suspend (FindEntity) -> File?) {
    val file by produceState<File?>(null, find.id) { value = photo(find) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var shift by remember { mutableStateOf(Offset.Zero) }
    val gestures = rememberTransformableState { _, zoomBy, panBy, _ ->
        zoom = (zoom * zoomBy).coerceIn(1f, MOST_ZOOM)
        shift = if (zoom == 1f) Offset.Zero else shift + panBy
    }
    PhotoBox(
        file = file,
        modifier = Modifier
            .fillMaxSize()
            .transformable(gestures, canPan = { zoom > 1f })
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        zoom = if (zoom > 1f) 1f else CLOSE_UP
                        shift = Offset.Zero
                    },
                )
            }
            .graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                translationX = shift.x
                translationY = shift.y
            },
        scale = ContentScale.Fit,
        ground = AppTheme.colors.photoGround,
    )
}
