package com.walkspot.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.walkspot.app.ui.theme.AppRipples
import com.walkspot.app.ui.theme.AppShadows
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.PillShape
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing

/** Danger is a Secondary button whose label warns: what it does cannot be undone. */
enum class AppButtonStyle { Primary, Secondary, Danger }

/** The icon of a Primary button is painted white; a Secondary one keeps its own colours. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    @DrawableRes iconRes: Int? = null,
) {
    val colors = AppTheme.colors
    val primary = style == AppButtonStyle.Primary
    val ground = if (primary) {
        Modifier
            .dropShadow(PillShape, AppShadows.accentButton)
            .background(colors.accentGradient, PillShape)
    } else {
        Modifier
            .dropShadow(PillShape, AppShadows.button)
            .background(colors.surface, PillShape)
            .border(Sizes.hairline, colors.divider, PillShape)
    }
    // White on the gradient, where the blue of the theme would not show.
    val ripple = if (primary) AppRipples.onAccent else AppRipples.soft
    CompositionLocalProvider(LocalRippleConfiguration provides ripple) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(Sizes.control)
                .then(ground)
                .clip(PillShape)
                .clickable(role = Role.Button, onClick = onClick),
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
                    AppButtonStyle.Primary -> colors.onAccent
                    AppButtonStyle.Secondary -> colors.textPrimary
                    AppButtonStyle.Danger -> colors.danger
                },
            )
        }
    }
}
