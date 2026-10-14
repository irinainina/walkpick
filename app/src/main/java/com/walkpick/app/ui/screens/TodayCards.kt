package com.walkpick.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.IconBadge
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing

private val NoticeTextTop = 14.dp
private val NoticeTextGap = 2.dp

@Composable
private fun Badge(@DrawableRes iconRes: Int) =
    IconBadge(iconRes, AppTheme.colors.streakTint, AppTheme.colors.streak)

@Composable
internal fun NoStepsCard(sensor: Boolean, onAllow: () -> Unit) {
    val reason = if (sensor) R.string.today_no_permission else R.string.today_no_sensor
    NoticeCard(
        title = stringResource(R.string.today_no_steps),
        text = stringResource(reason),
        action = stringResource(R.string.today_allow).takeIf { sensor },
        onAction = onAllow,
    )
}

/** Shown until the first walk: what the ring counts and what the app is for. */
@Composable
internal fun FirstWalkCard() {
    NoticeCard(stringResource(R.string.today_first_title), stringResource(R.string.today_first_text))
}

@Composable
private fun NoticeCard(title: String, text: String, action: String? = null, onAction: () -> Unit = {}) {
    val colors = AppTheme.colors
    AppCard {
        Row(
            modifier = Modifier.padding(Spacing.m, NoticeTextTop, Spacing.m, Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(Modifier.padding(top = NoticeTextGap)) { Badge(R.drawable.ic_alert) }
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                Spacer(Modifier.height(NoticeTextGap))
                Text(text, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                if (action != null) {
                    Spacer(Modifier.height(Spacing.ml))
                    Text(
                        text = action,
                        modifier = Modifier.clickable(role = Role.Button, onClick = onAction),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.accent,
                    )
                }
            }
        }
    }
}
