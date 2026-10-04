package com.walkspot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.walkspot.app.R
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing

/**
 * A row of a settings card. [leading] is a 40 dp badge, [trailing] a chevron or a switch;
 * the divider starts under the label, so it moves right when there is a badge.
 */
@Composable
fun ListRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable () -> Unit = { RowChevron() },
) {
    val labelStart = if (leading == null) Spacing.m else Spacing.m + Sizes.circle + Spacing.sm
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Sizes.control)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(label, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textPrimary)
            Text(
                text = value.orEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.s, end = Spacing.xs),
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            trailing()
        }
        if (divider) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = labelStart, end = Spacing.m)
                    .fillMaxWidth()
                    .height(Sizes.hairline)
                    .background(AppTheme.colors.divider),
            )
        }
    }
}

@Composable
fun RowChevron() {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = AppTheme.colors.textSecondary,
    )
}
