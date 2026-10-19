package com.walkpick.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.PhotoStrip
import com.walkpick.app.ui.components.RowChevron
import com.walkpick.app.ui.components.StripLook
import com.walkpick.app.ui.grouped
import com.walkpick.app.ui.stats.StatsState
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing

private val ThumbSize = 66.dp
private const val THUMBS = 4
private const val THUMB_SAMPLE = 4

/** The Walk tab: the walks by period, then the photos of that period. */
@Composable
fun WalkScreen(
    state: StatsState,
    actions: PeriodActions,
    onPhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(top = Spacing.m, bottom = Spacing.ml),
    ) {
        WalkPeriods(state, actions)
        Spacer(Modifier.height(Spacing.m))
        NoticeCard(
            title = stringResource(R.string.walk_photos_call),
            text = stringResource(R.string.walk_photos_call_text),
            iconRes = R.drawable.ic_camera,
            modifier = Modifier.padding(horizontal = Spacing.ml),
        )
        Spacer(Modifier.height(Spacing.m))
        PhotosCard(state, actions, onPhotos)
    }
}

@Composable
private fun PhotosCard(state: StatsState, actions: PeriodActions, onOpen: () -> Unit) {
    val colors = AppTheme.colors
    val type = MaterialTheme.typography
    AppCard(Modifier.padding(horizontal = Spacing.ml)) {
        Column(
            Modifier
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(start = Spacing.m, top = Spacing.m, end = Spacing.m, bottom = Spacing.ml),
        ) {
            Row(Modifier.padding(start = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.walk_tab_photos),
                    modifier = Modifier.weight(1f),
                    style = type.titleSmall,
                    color = colors.textPrimary,
                )
                val period = stringResource(periodWord(state.tab, state.today in state.range))
                Text(
                    text = stringResource(R.string.walk_photos_count, grouped(state.photos.size), period),
                    style = type.bodyMedium,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.width(Spacing.s))
                RowChevron()
            }
            if (state.photos.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                val look = StripLook(
                    ThumbSize,
                    THUMBS,
                    Spacing.s,
                    MaterialTheme.shapes.small,
                    type.titleMedium,
                    THUMB_SAMPLE,
                )
                PhotoStrip(state.photos.take(THUMBS), actions.photo, look)
            }
        }
    }
}
