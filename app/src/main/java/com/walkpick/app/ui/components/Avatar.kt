package com.walkpick.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.walkpick.app.ui.theme.AppTheme

/** The first letter of the user's name in a ringed circle, as it stands on the season photo. */
@Composable
fun Avatar(name: String, size: Dp, ring: Dp, style: TextStyle, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .background(colors.surfaceTint, CircleShape)
            .border(ring, colors.onAccent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = name.take(1).uppercase(), style = style, color = colors.accent)
    }
}
