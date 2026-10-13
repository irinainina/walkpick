package com.walkpick.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.walk.Season
import com.walkpick.app.ui.theme.AppTheme

private val WaveHeight = 72.dp

// How much of the photo the gradient hides: thinner, and the white text over the bright leaf is lost.
private const val VEIL = 0.72f

private fun Season.photo() = when (this) {
    Season.SPRING -> R.drawable.header_spring
    Season.SUMMER -> R.drawable.header_summer
    Season.AUTUMN -> R.drawable.header_autumn
    Season.WINTER -> R.drawable.header_winter
}

/** The top of a screen: a photo under the gradient, cut by the wave; [height] includes the status bar. */
@Composable
fun GradientHeader(
    height: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height),
    ) {
        Image(
            painter = painterResource(AppTheme.season.photo()),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.matchParentSize().background(AppTheme.colors.accentGradient, alpha = VEIL))
        content()
        Image(
            painter = painterResource(R.drawable.bg_wave),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(WaveHeight),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(AppTheme.colors.canvas),
        )
    }
}
