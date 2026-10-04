package com.walkspot.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.ListRow
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing

/** Stands in for a screen that is not laid out yet: its name and the screens it leads to. */
@Composable
fun PlaceholderScreen(
    title: String,
    links: List<String>,
    onOpen: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(Spacing.ml),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = AppTheme.colors.textPrimary)
        if (links.isNotEmpty()) {
            AppCard {
                links.forEachIndexed { index, link ->
                    ListRow(label = link, divider = index != links.lastIndex, onClick = { onOpen(index) })
                }
            }
        }
    }
}
