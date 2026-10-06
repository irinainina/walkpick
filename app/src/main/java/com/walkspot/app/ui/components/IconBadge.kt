package com.walkspot.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.walkspot.app.ui.theme.Sizes

/** An icon on a round tinted ground, 40 across. */
@Composable
fun IconBadge(@DrawableRes iconRes: Int, ground: Color, tint: Color, iconSize: Dp = Sizes.icon) {
    Box(
        modifier = Modifier
            .size(Sizes.circle)
            .background(ground, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, Modifier.size(iconSize), tint)
    }
}
