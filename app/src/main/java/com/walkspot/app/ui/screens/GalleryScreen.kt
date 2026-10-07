package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.walk.title
import com.walkspot.app.ui.components.AppButton
import com.walkspot.app.ui.components.AppButtonStyle
import com.walkspot.app.ui.components.AppSheet
import com.walkspot.app.ui.components.EmptyState
import com.walkspot.app.ui.components.SegmentedControl
import com.walkspot.app.ui.counted
import com.walkspot.app.ui.gallery.GalleryGroup
import com.walkspot.app.ui.gallery.GalleryState
import com.walkspot.app.ui.gallery.GalleryTab
import com.walkspot.app.ui.localDay
import com.walkspot.app.ui.theme.AppShadows
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.ParagraphStyle
import com.walkspot.app.ui.theme.Spacing
import java.io.File

private const val ALL_COLUMNS = 3
private const val BEST_COLUMNS = 2
private val BestBadge = 36.dp
private val ActionsHeight = 72.dp

/** What the gallery asks of its owner. */
class GalleryActions(
    val photo: suspend (FindEntity) -> File?,
    val onTab: (GalleryTab) -> Unit,
    val onOpen: (FindEntity) -> Unit,
    /** Starts the picking of photos with none picked, or ends it. */
    val onSelecting: (Boolean) -> Unit,
    val onToggle: (String) -> Unit,
    val onAddToBest: () -> Unit,
    val onDelete: () -> Unit,
)

@Composable
fun GalleryScreen(state: GalleryState, actions: GalleryActions, modifier: Modifier = Modifier) {
    val selected = state.selected
    val all = state.tab == GalleryTab.All
    val empty = if (all) state.groups.isEmpty() else state.best.isEmpty()
    var deleting by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas),
    ) {
        Column(Modifier.statusBarsPadding()) {
            Heading(selected, actions, canSelect = !empty)
            Spacer(Modifier.height(Spacing.sm))
            SegmentedControl(
                options = listOf(stringResource(R.string.gallery_all), stringResource(R.string.gallery_best)),
                selectedIndex = state.tab.ordinal,
                onSelect = { actions.onTab(GalleryTab.entries[it]) },
                modifier = Modifier.padding(horizontal = Spacing.ml),
            )
            when {
                !empty -> Photos(state, actions)
                all -> EmptyState(
                    iconRes = R.drawable.ic_camera,
                    title = stringResource(R.string.gallery_empty_title),
                    text = stringResource(R.string.gallery_empty_text),
                )
                else -> EmptyState(
                    iconRes = R.drawable.ic_star,
                    title = stringResource(R.string.gallery_best_empty_title),
                    text = stringResource(R.string.gallery_best_empty_text),
                )
            }
        }
        if (selected != null) {
            SelectionBar(
                enabled = selected.isNotEmpty(),
                onAddToBest = actions.onAddToBest,
                onDelete = { deleting = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
    if (deleting && selected != null) {
        val title = counted(selected.size, R.string.gallery_delete_one, R.string.gallery_delete_many)
        AppSheet(title, onDismiss = { deleting = false }) {
            Spacer(Modifier.height(Spacing.s))
            val warning = stringResource(R.string.gallery_delete_text)
            Text(warning, style = ParagraphStyle, color = AppTheme.colors.textSecondary)
            Spacer(Modifier.height(Spacing.l))
            val delete = {
                deleting = false
                actions.onDelete()
            }
            AppButton(stringResource(R.string.gallery_delete), delete, style = AppButtonStyle.Danger)
            Spacer(Modifier.height(Spacing.sm))
            val cancel = stringResource(R.string.settings_cancel)
            AppButton(cancel, { deleting = false }, style = AppButtonStyle.Secondary)
        }
    }
}

// "Gallery" and Select, or the number picked and Cancel.
@Composable
private fun Heading(selected: Set<String>?, actions: GalleryActions, canSelect: Boolean) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.padding(start = Spacing.ml, top = Spacing.m, end = Spacing.ml),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = selected?.let { stringResource(R.string.gallery_selected, it.size) }
                ?: stringResource(R.string.tab_gallery),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.textPrimary,
        )
        val picking = selected != null
        // Nothing to pick from: the word would lead nowhere.
        if (!picking && !canSelect) return@Row
        Text(
            text = stringResource(if (picking) R.string.settings_cancel else R.string.gallery_select),
            modifier = Modifier.clickable(role = Role.Button) {
                actions.onSelecting(!picking)
            },
            style = MaterialTheme.typography.labelLarge,
            color = colors.accent,
        )
    }
}

@Composable
private fun Photos(state: GalleryState, actions: GalleryActions) {
    val picking = state.selected != null
    // In the selection mode a tap picks, outside it a tap opens and a long press starts picking.
    val look = TileLook(
        shape = with(MaterialTheme.shapes) { if (state.tab == GalleryTab.All) small else medium },
        badge = if (state.tab == GalleryTab.All) Spacing.l else BestBadge,
        star = if (state.tab == GalleryTab.All) Spacing.m else Spacing.l,
        inset = if (state.tab == GalleryTab.All) Spacing.s else Spacing.sm,
        photo = actions.photo,
        onTap = { if (picking) actions.onToggle(it.id) else actions.onOpen(it) },
        onHold = { actions.onToggle(it.id) },
    )
    val bottom = if (picking) ActionsHeight + Spacing.l else Spacing.l
    LazyColumn(contentPadding = PaddingValues(Spacing.ml, Spacing.m, Spacing.ml, bottom)) {
        if (state.tab == GalleryTab.All) {
            state.groups.forEachIndexed { index, group -> group(group, first = index == 0, state, look) }
        } else {
            item {
                val count = counted(state.best.size, R.string.gallery_best_one, R.string.gallery_best_many)
                val grey = AppTheme.colors.textSecondary
                Text(count, style = MaterialTheme.typography.bodyMedium, color = grey)
                Spacer(Modifier.height(Spacing.sm))
            }
            items(state.best.chunked(BEST_COLUMNS), key = { it.first().id }) { row ->
                TileRow(row, BEST_COLUMNS, Spacing.s, state.selected, look)
                Spacer(Modifier.height(Spacing.s))
            }
        }
    }
}

// The name of a walk with its day and its number of photos, then its photos three in a row.
private fun LazyListScope.group(group: GalleryGroup, first: Boolean, state: GalleryState, look: TileLook) {
    item(key = group.walk.id) {
        if (!first) Spacer(Modifier.height(Spacing.m))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = group.walk.title(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(Spacing.s))
            val day = dayTitle(group.walk.startedAt.localDay(), state.today)
            val photos = counted(group.finds.size, R.string.diary_photo, R.string.diary_photos)
            Text(
                text = stringResource(R.string.gallery_day_photos, day, photos),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
    }
    items(group.finds.chunked(ALL_COLUMNS), key = { it.first().id }) { row ->
        TileRow(row, ALL_COLUMNS, Spacing.xs, state.selected, look)
        Spacer(Modifier.height(Spacing.xs))
    }
}

// Stands where the tabs of the app stand while photos are being picked.
@Composable
private fun SelectionBar(
    enabled: Boolean,
    onAddToBest: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(RectangleShape, AppShadows.navBar)
            .background(colors.surface)
            .navigationBarsPadding()
            .height(ActionsHeight)
            .padding(vertical = Spacing.s),
    ) {
        Action(R.drawable.ic_star, R.string.gallery_add_best, colors.streak, enabled, onAddToBest)
        Action(R.drawable.ic_trash, R.string.gallery_delete, colors.danger, enabled, onDelete)
    }
}

@Composable
private fun RowScope.Action(
    iconRes: Int,
    labelRes: Int,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = color)
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall, color = color)
    }
}
