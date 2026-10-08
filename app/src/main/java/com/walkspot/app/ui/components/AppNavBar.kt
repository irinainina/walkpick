package com.walkspot.app.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walkspot.app.ui.theme.AppShadows
import com.walkspot.app.ui.theme.AppShapes
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing

data class NavItem(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val route: String,
)

private val BarHeight = 72.dp
private const val PRESSED_TINT = 0.2f
private const val PRESS_MS = 100

@Composable
fun AppNavBar(
    items: List<NavItem>,
    selectedRoute: String?,
    onSelect: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Plain blue, not the gradient of the buttons: the small label stays readable from edge to edge.
    val pill = Modifier.background(AppTheme.colors.accent)
    val tintColor = AppTheme.colors.accent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .dropShadow(RectangleShape, AppShadows.navBar)
            .background(AppTheme.colors.surface)
            .padding(Spacing.s),
    ) {
        items.forEach { item ->
            val selected = item.route == selectedRoute
            val color = if (selected) AppTheme.colors.onAccent else AppTheme.colors.textSecondary
            // The whole cell takes the tap; the pill answers with a short tint, quicker than a wave.
            val touch = remember { MutableInteractionSource() }
            val pressed by touch.collectIsPressedAsState()
            val target = if (pressed) PRESSED_TINT else 0f
            val tint by animateFloatAsState(target, tween(PRESS_MS), label = "press")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(selected, touch, indication = null, role = Role.Tab) { onSelect(item) },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(AppShapes.large)
                        .then(if (selected) pill else Modifier)
                        .drawBehind { drawRect(tintColor, alpha = tint) }
                        .padding(vertical = Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(painterResource(item.iconRes), contentDescription = null, tint = color)
                    Text(
                        text = stringResource(item.labelRes),
                        style = with(MaterialTheme.typography) { if (selected) labelSmall else bodySmall },
                        color = color,
                    )
                }
            }
        }
    }
}
