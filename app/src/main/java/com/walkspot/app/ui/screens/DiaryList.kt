package com.walkspot.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.data.walk.title
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.RouteSketch
import com.walkspot.app.ui.components.RowChevron
import com.walkspot.app.ui.counted
import com.walkspot.app.ui.distanceText
import com.walkspot.app.ui.grouped
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
                    text = dayTitle(day.day, state.today),
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

/** A day as the diary calls it: Today, Yesterday, or its date. */
@Composable
internal fun dayTitle(day: LocalDate, today: LocalDate): String = when {
    day == today -> stringResource(R.string.diary_today)
    day == today.minusDays(1) -> stringResource(R.string.diary_yesterday)
    day.year == today.year -> ThisYear.format(day)
    else -> AnotherYear.format(day)
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
            RouteSketch(item.sketch, Modifier.size(SketchSize))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = SketchSize)
                    .padding(top = Spacing.xs / 2),
            ) {
                val minutes = (walk.durationS / SECONDS_IN_MINUTE).roundToInt()
                val start = Clock.format(walk.startedAt.atZone(ZoneId.systemDefault()))
                val distance = distanceText(walk.distanceM, state.distanceUnit)
                Text(
                    text = walk.title(),
                    style = type.labelLarge,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
