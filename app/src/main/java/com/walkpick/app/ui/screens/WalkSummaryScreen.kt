package com.walkpick.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.walk.paceSecondsPerKm
import com.walkpick.app.data.walk.title
import com.walkpick.app.ui.components.AppButton
import com.walkpick.app.ui.components.AppButtonStyle
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.AppSheet
import com.walkpick.app.ui.components.CircleButton
import com.walkpick.app.ui.components.IconBadge
import com.walkpick.app.ui.components.RoutePicture
import com.walkpick.app.ui.distanceText
import com.walkpick.app.ui.grouped
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.ParagraphStyle
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing
import com.walkpick.app.ui.walk.WalkSummaryState
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val MapHeight = 480.dp

// The card lies over the bottom of the map; the route keeps clear of it and of the round buttons.
private val CardTop = 428.dp
private val MapCoveredTop = 56.dp
private val RouteEdge = 32.dp
private val RouteLine = 6.dp
private val StartPin = 32.dp
private val CardBottom = 28.dp

// The number stands 4 above its badge, as drawn.
private val StatIconTop = 4.dp
private val StatColumn = 144.dp
private const val SECONDS_IN_MINUTE = 60.0

private val Day = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
private val Clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

@Composable
fun WalkSummaryScreen(
    state: WalkSummaryState,
    onBack: () -> Unit,
    onNote: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onFind: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val walk = state.walk
    var note by rememberSaveable { mutableStateOf(walk.note.orEmpty()) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    // The note is kept on the way out, whichever way that is.
    val leave = {
        onNote(note)
        onBack()
    }
    BackHandler(onBack = leave)
    Box(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .verticalScroll(rememberScrollState()),
    ) {
        RoutePicture(
            walk = state.walk,
            finds = state.finds,
            edge = RouteEdge,
            modifier = Modifier
                .fillMaxWidth()
                .height(MapHeight),
            covered = PaddingValues(top = MapCoveredTop, bottom = MapHeight - CardTop),
            line = RouteLine,
            startPin = StartPin,
            photos = state.photos,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.ml, vertical = Spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleButton(R.drawable.ic_back, stringResource(R.string.back), leave, raised = true)
            val more = stringResource(R.string.walk_more)
            CircleButton(R.drawable.ic_more, more, { deleting = true }, raised = true)
        }
        Column(
            Modifier
                .padding(start = Spacing.ml, top = CardTop, end = Spacing.ml, bottom = Spacing.l)
                .navigationBarsPadding(),
        ) {
            SummaryCard(state, onRename = { renaming = true })
            Spacer(Modifier.height(Spacing.l))
            WalkFinds(state, onFind)
            if (state.finds.isNotEmpty()) Spacer(Modifier.height(Spacing.l))
            NoteField(note, onChange = { note = it })
            Spacer(Modifier.height(Spacing.l))
            AppButton(stringResource(R.string.walk_done), onClick = leave)
        }
    }
    if (renaming) {
        NameSheet(stringResource(R.string.walk_name_title), walk.title(), onDismiss = { renaming = false }) {
            onRename(it)
            renaming = false
        }
    }
    if (deleting) {
        AppSheet(stringResource(R.string.walk_delete_title), onDismiss = { deleting = false }) {
            val warning = stringResource(R.string.walk_delete_text)
            val cancel = stringResource(R.string.settings_cancel)
            Spacer(Modifier.height(Spacing.s))
            Text(warning, style = ParagraphStyle, color = AppTheme.colors.textSecondary)
            Spacer(Modifier.height(Spacing.l))
            AppButton(stringResource(R.string.walk_delete), onDelete, style = AppButtonStyle.Danger)
            Spacer(Modifier.height(Spacing.sm))
            AppButton(cancel, { deleting = false }, style = AppButtonStyle.Secondary)
        }
    }
}

@Composable
private fun SummaryCard(state: WalkSummaryState, onRename: () -> Unit) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    val walk = state.walk
    val zone = ZoneId.systemDefault()
    val (from, to) = walk.startedAt.atZone(zone) to walk.endedAt.atZone(zone)
    AppCard {
        Column(Modifier.padding(Spacing.ml, Spacing.ml, Spacing.ml, CardBottom)) {
            // The name is the walker's to change: a tap on it opens the sheet.
            Text(
                text = walk.title(),
                modifier = Modifier.clickable(
                    onClickLabel = stringResource(R.string.walk_rename),
                    role = Role.Button,
                    onClick = onRename,
                ),
                color = colors.textPrimary,
                style = type.headlineMedium,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "${Day.format(from)} · ${Clock.format(from)} – ${Clock.format(to)}",
                style = type.bodyMedium,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(Spacing.ml))
            val unit = state.distanceUnit
            val minutes = (walk.durationS / SECONDS_IN_MINUTE).roundToInt()
            val pace = paceSecondsPerKm(walk.distanceM.toDouble(), walk.durationS.toLong())
                ?.let { clockText((it / unit.perMetric).roundToInt().toLong()) }
            Row {
                Stat(R.drawable.ic_shoe, grouped(walk.steps), R.string.walk_steps, Modifier.width(StatColumn))
                Stat(R.drawable.ic_route, distanceText(walk.distanceM, unit), R.string.walk_distance)
            }
            Spacer(Modifier.height(Spacing.ml))
            Row {
                val time = stringResource(R.string.walk_minutes, minutes)
                Stat(R.drawable.ic_clock, time, R.string.walk_time, Modifier.width(StatColumn))
                Stat(R.drawable.ic_flame, pace ?: "–", R.string.walk_pace, labelUnit = unit.symbol)
            }
        }
    }
}

@Composable
private fun Stat(
    @DrawableRes iconRes: Int,
    value: String,
    @StringRes labelRes: Int,
    modifier: Modifier = Modifier,
    labelUnit: String = "",
) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(Modifier.padding(top = StatIconTop)) {
            IconBadge(iconRes, colors.surfaceTint, colors.accent, Sizes.iconSmall)
        }
        Column {
            Text(value, style = type.titleMedium, color = colors.textPrimary, maxLines = 1)
            Text(stringResource(labelRes, labelUnit), style = type.bodySmall, color = colors.textSecondary)
        }
    }
}

// A few lines about the walk; grows with the text.
@Composable
private fun NoteField(note: String, onChange: (String) -> Unit) {
    val colors = AppTheme.colors
    val shape = MaterialTheme.shapes.medium
    BasicTextField(
        value = note,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = ParagraphStyle.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.accent),
        decorationBox = { field ->
            Box(
                modifier = Modifier
                    .heightIn(min = Sizes.control)
                    .background(colors.surface, shape)
                    .border(Sizes.hairline, colors.divider, shape)
                    .padding(Spacing.m),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (note.isEmpty()) {
                    val hint = stringResource(R.string.walk_note_hint)
                    Text(hint, style = ParagraphStyle, color = colors.textSecondary)
                }
                field()
            }
        },
    )
}
