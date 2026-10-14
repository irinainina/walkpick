package com.walkpick.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import com.walkpick.app.ui.theme.AppShadows
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.CardShape
import com.walkpick.app.ui.theme.Sizes

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    // A hairline instead of the shadow, for a card lying on white.
    outlined: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val edge = if (outlined) {
        Modifier.border(Sizes.hairline, AppTheme.colors.divider, CardShape)
    } else {
        Modifier.dropShadow(CardShape, AppShadows.card)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(edge)
            .clip(CardShape)
            .background(AppTheme.colors.surface),
        content = content,
    )
}
