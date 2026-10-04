package com.walkspot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import com.walkspot.app.ui.theme.AppShadows
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.CardShape

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(CardShape, AppShadows.card)
            .clip(CardShape)
            .background(AppTheme.colors.surface),
        content = content,
    )
}
