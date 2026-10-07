package com.walkspot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walkspot.app.ui.theme.AppShadows
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.PillShape
import com.walkspot.app.ui.theme.Spacing

private val ControlHeight = 40.dp

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    // White where the control lies over a map.
    ground: Color = AppTheme.colors.surfaceTint,
    selectedText: Color = AppTheme.colors.accent,
    otherText: Color = AppTheme.colors.textInactive,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ControlHeight)
            .background(ground, PillShape)
            .padding(Spacing.xs),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val ground = if (selected) {
                Modifier
                    .dropShadow(PillShape, AppShadows.segment)
                    .background(AppTheme.colors.surface, PillShape)
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(ground)
                    .clip(PillShape)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = with(MaterialTheme.typography) { if (selected) labelMedium else bodyMedium },
                    color = if (selected) selectedText else otherText,
                    maxLines = 1,
                )
            }
        }
    }
}
