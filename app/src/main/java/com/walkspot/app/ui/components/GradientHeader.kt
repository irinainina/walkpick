package com.walkspot.app.ui.components

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
import com.walkspot.app.R
import com.walkspot.app.ui.theme.AppTheme

private val WaveHeight = 72.dp

/** The gradient top of a screen, cut by the wave at its bottom; [height] includes the status bar. */
@Composable
fun GradientHeader(
    height: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(AppTheme.colors.accentGradient),
    ) {
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
