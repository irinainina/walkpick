package com.walkpick.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.GlassRadius
import com.walkpick.app.ui.theme.GlassShape
import com.walkpick.app.ui.theme.Sizes

private val GlassBlur = 24.dp

// How much of the photo the dark veil hides at the top, in the middle and at the bottom: the
// white words stand at the ends, the picture shows in between.
private const val VEIL_TOP = 0.55f
private const val VEIL_MIDDLE = 0.15f
private const val VEIL_BOTTOM = 0.5f

/**
 * A screen over the photo of the season. [content] gets the modifier of its glass card: whatever
 * wears it is drawn frosted, with the photo blurred behind.
 */
@Composable
fun GlassScreen(modifier: Modifier = Modifier, content: @Composable BoxScope.(glass: Modifier) -> Unit) {
    DarkSystemBars()
    val colors = AppTheme.colors
    var card by remember { mutableStateOf(Rect.Zero) }
    val radius = with(LocalDensity.current) { GlassRadius.toPx() }
    Box(modifier.fillMaxSize()) {
        Backdrop(Modifier.matchParentSize())
        // The glass: the same picture once more, blurred and cut to the card.
        Backdrop(
            Modifier
                .matchParentSize()
                .clip(GenericShape { _, _ -> addRoundRect(RoundRect(card, CornerRadius(radius))) })
                .blur(GlassBlur, BlurredEdgeTreatment.Unbounded),
        )
        content(
            Modifier
                .onGloballyPositioned { card = it.boundsInRoot() }
                .background(colors.glass, GlassShape)
                .border(Sizes.hairline, colors.glassEdge, GlassShape),
        )
    }
}

@Composable
private fun Backdrop(modifier: Modifier = Modifier) {
    val veil = AppTheme.colors.photoGround
    Box(modifier) {
        Image(
            painter = painterResource(AppTheme.season.photo()),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to veil.copy(alpha = VEIL_TOP),
                        0.5f to veil.copy(alpha = VEIL_MIDDLE),
                        1f to veil.copy(alpha = VEIL_BOTTOM),
                    ),
                ),
        )
    }
}
