package com.walkpick.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.walkpick.app.ui.theme.AppShadows
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.PillShape
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing

/**
 * Danger is a Secondary button whose label warns: what it does cannot be undone. Stop is a Primary
 * one in orange: it ends the walk under way.
 */
enum class AppButtonStyle { Primary, Secondary, Danger, Stop }

private const val PRESSED_SCALE = 0.96f

/** The icon of a Primary button is painted white; a Secondary one keeps its own colours. */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    @DrawableRes iconRes: Int? = null,
) {
    val colors = AppTheme.colors
    val primary = style == AppButtonStyle.Primary || style == AppButtonStyle.Stop
    val ground = if (style == AppButtonStyle.Stop) {
        Modifier
            .dropShadow(PillShape, AppShadows.stopButton)
            .background(colors.stopGradient, PillShape)
    } else if (primary) {
        Modifier
            .dropShadow(PillShape, AppShadows.accentButton)
            .background(colors.accentGradient, PillShape)
    } else {
        Modifier
            .dropShadow(PillShape, AppShadows.button)
            .background(colors.surface, PillShape)
            .border(Sizes.hairline, colors.divider, PillShape)
    }
    // No ripple: the button gives way under the finger instead.
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) PRESSED_SCALE else 1f, label = "press")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Sizes.control)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(ground)
            .clickable(touch, indication = null, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (primary) colors.onAccent else Color.Unspecified,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = when (style) {
                AppButtonStyle.Primary, AppButtonStyle.Stop -> colors.onAccent
                AppButtonStyle.Secondary -> colors.textPrimary
                AppButtonStyle.Danger -> colors.danger
            },
        )
    }
}
