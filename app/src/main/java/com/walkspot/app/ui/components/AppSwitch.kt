package com.walkspot.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.walkspot.app.ui.theme.AppShadows
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.PillShape
import com.walkspot.app.ui.theme.Sizes

private val TrackWidth = 44.dp
private val TrackHeight = 28.dp
private val KnobInset = 2.dp

@Composable
fun AppSwitch(
    on: Boolean,
    onToggle: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // 0 is off, 1 is on: the knob slides and the gradient comes through the grey together.
    val turned by animateFloatAsState(targetValue = if (on) 1f else 0f, label = "switch")
    Box(
        modifier = modifier
            .size(TrackWidth, TrackHeight)
            .background(AppTheme.colors.switchOff, PillShape)
            .background(AppTheme.colors.accentGradient, PillShape, alpha = turned)
            // No ripple: the moving knob is the answer to the tap.
            .toggleable(
                value = on,
                interactionSource = null,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onToggle,
            )
            .semantics { contentDescription = label },
    ) {
        Box(
            Modifier
                .offset {
                    val travel = TrackWidth - Sizes.icon - KnobInset * 2
                    IntOffset((KnobInset + travel * turned).roundToPx(), KnobInset.roundToPx())
                }
                .size(Sizes.icon)
                .dropShadow(CircleShape, AppShadows.knob)
                .background(AppTheme.colors.surface, CircleShape),
        )
    }
}
