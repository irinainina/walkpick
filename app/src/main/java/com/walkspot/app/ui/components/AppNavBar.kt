package com.walkspot.app.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walkspot.app.ui.theme.AppShadows
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing

data class NavItem(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val route: String,
)

private val BarHeight = 72.dp

@Composable
fun AppNavBar(
    items: List<NavItem>,
    selectedRoute: String?,
    onSelect: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .dropShadow(RectangleShape, AppShadows.navBar)
            .background(AppTheme.colors.surface)
            .padding(vertical = Spacing.s),
    ) {
        items.forEach { item ->
            val selected = item.route == selectedRoute
            val color = if (selected) AppTheme.colors.accent else AppTheme.colors.textSecondary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(item) })
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
