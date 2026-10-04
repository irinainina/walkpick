package com.walkspot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walkspot.app.ui.theme.AppShadows
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.PillShape
import com.walkspot.app.ui.theme.Spacing

private val ChipHeight = 36.dp

@Composable
fun AppChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ground = if (selected) {
        Modifier.background(AppTheme.colors.accentGradient, PillShape)
    } else {
        Modifier.background(AppTheme.colors.surface, PillShape)
    }
    Box(
        modifier = modifier
            .height(ChipHeight)
            .dropShadow(PillShape, AppShadows.chip)
            .then(ground)
            .clip(PillShape)
            .selectable(selected = selected, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = Spacing.m),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AppTheme.colors.onAccent else AppTheme.colors.textInactive,
        )
    }
}
