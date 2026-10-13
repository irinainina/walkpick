package com.walkpick.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.ParagraphStyle
import com.walkpick.app.ui.theme.Spacing

private val Badge = 72.dp

/** What a list says while it has nothing to show: where its content comes from. */
@Composable
fun EmptyState(@DrawableRes iconRes: Int, title: String, text: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(Badge).background(colors.surfaceTint, CircleShape), Alignment.Center) {
            Icon(painterResource(iconRes), null, Modifier.size(Spacing.xl), colors.accent)
        }
        Spacer(Modifier.height(Spacing.m))
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Spacer(Modifier.height(Spacing.s))
        Text(text, style = ParagraphStyle, color = colors.textSecondary, textAlign = TextAlign.Center)
    }
}
