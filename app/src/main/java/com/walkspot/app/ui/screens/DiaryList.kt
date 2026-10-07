package com.walkspot.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.RowChevron
import com.walkspot.app.ui.counted
import com.walkspot.app.ui.distanceText
import com.walkspot.app.ui.grouped
import com.walkspot.app.ui.map.DiaryDay
import com.walkspot.app.ui.map.DiaryWalk
import com.walkspot.app.ui.map.MapState
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val SketchSize = 104.dp
private val SketchEdge = 20.dp
private val SketchLine = 3.dp
private val SketchStart = 8.dp
private val SketchStartLine = 2.dp
private val FlagHalfWidth = 3.dp
private val FlagHalfHeight = 4.dp
private val FlagNotch = 1.dp
private val FlagLine = 1.5.dp
private const val SECONDS_IN_MINUTE = 60.0

private val ThisYear = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
private val AnotherYear = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH)
private val Clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** Every walk by days, the latest first; the list draws only what is in sight, however long it is. */
@Composable
internal fun DiaryList(
    state: MapState,
    onWalk: (String) -> Unit,
    content: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, contentPadding = content) {
        state.days.forEachIndexed { index, day ->
            item(key = day.day.toEpochDay()) {
                if (index > 0) Spacer(Modifier.height(Spacing.l))
                Text(
                    text = dayTitle(day, state.today),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.colors.textPrimary,
                )
            }
            items(day.walks, key = { it.walk.id }) { walk ->
                // 12 under the title of the day, 16 between the walks of one day.
                val first = walk == day.walks.first()
                Spacer(Modifier.height(if (first) Spacing.sm else Spacing.m))
                WalkCard(walk, state, onWalk)
            }
        }
    }
}

@Composable
private fun dayTitle(day: DiaryDay, today: LocalDate): String = when {
    day.day == today -> stringResource(R.string.diary_today)
    day.day == today.minusDays(1) -> stringResource(R.string.diary_yesterday)
    day.day.year == today.year -> ThisYear.format(day.day)
    else -> AnotherYear.format(day.day)
}

@Composable
private fun WalkCard(item: DiaryWalk, state: MapState, onWalk: (String) -> Unit) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    val walk = item.walk
    AppCard {
        Row(
            modifier = Modifier
                .clickable { onWalk(walk.id) }
                .padding(start = Spacing.sm, top = Spacing.sm, end = Spacing.m, bottom = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RouteSketch(item.sketch)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(SketchSize)
                    .padding(top = Spacing.xs / 2),
            ) {
                val minutes = (walk.durationS / SECONDS_IN_MINUTE).roundToInt()
                val start = Clock.format(walk.startedAt.atZone(ZoneId.systemDefault()))
                val distance = distanceText(walk.distanceM, state.distanceUnit)
                val title = stringResource(R.string.walk_number, walk.number)
                Text(title, style = type.labelLarge, color = colors.textPrimary)
                Spacer(Modifier.height(Spacing.xs / 2))
                Text(
                    text = "$start · ${stringResource(R.string.walk_minutes, minutes)}",
                    style = type.bodySmall,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(Spacing.xs / 2))
                Text(
                    text = stringResource(R.string.diary_steps, grouped(walk.steps), distance),
                    style = type.labelMedium,
                    color = colors.accent,
                )
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = if (item.photos == 0) {
                        stringResource(R.string.diary_no_photos)
                    } else {
                        counted(item.photos, R.string.diary_photo, R.string.diary_photos)
                    },
                    style = type.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            RowChevron()
        }
    }
}

// The line of the walk alone, on a tile of the colour of the map: a real map in every card of a long
// list would be too heavy.
@Composable
private fun RouteSketch(sketch: List<Pair<Float, Float>>) {
    val colors = AppTheme.colors
    Canvas(
        Modifier
            .size(SketchSize)
            .clip(MaterialTheme.shapes.medium)
            .background(colors.mapGround),
    ) {
        if (sketch.isEmpty()) return@Canvas
        val edge = SketchEdge.toPx()
        val side = size.width - edge * 2
        val points = sketch.map { (x, y) -> Offset(edge + x * side, edge + y * side) }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(colors.accent, colors.accentLight), edge, edge + side),
            style = Stroke(SketchLine.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(colors.surface, SketchStart.toPx(), points.first())
        drawCircle(colors.accent, SketchStart.toPx(), points.first(), style = Stroke(SketchStartLine.toPx()))
        val (x, y) = points.first()
        val (halfWidth, halfHeight) = FlagHalfWidth.toPx() to FlagHalfHeight.toPx()
        val flag = Path().apply {
            moveTo(x - halfWidth, y + halfHeight)
            lineTo(x - halfWidth, y - halfHeight)
            lineTo(x + halfWidth, y - halfHeight)
            lineTo(x + halfWidth - FlagNotch.toPx(), y - halfHeight / 2)
            lineTo(x + halfWidth, y)
            lineTo(x - halfWidth, y)
        }
        val line = Stroke(FlagLine.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(flag, colors.accent, style = line)
    }
}
