package com.walkspot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.PillShape
import com.walkspot.app.ui.theme.SheetShape
import com.walkspot.app.ui.theme.Spacing

private val HandleWidth = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SheetShape,
        containerColor = AppTheme.colors.surface,
        scrimColor = AppTheme.colors.scrim,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = Spacing.s)
                    .size(HandleWidth, Spacing.xs)
                    .background(AppTheme.colors.divider, PillShape),
            )
        },
    ) {
        Column(Modifier.padding(start = Spacing.ml, top = Spacing.m, end = Spacing.ml, bottom = Spacing.xl)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.textPrimary)
            content()
        }
    }
}
