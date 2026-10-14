package com.walkpick.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.data.walk.title
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.PhotoStrip
import com.walkpick.app.ui.components.RouteSketch
import com.walkpick.app.ui.components.RowChevron
import com.walkpick.app.ui.components.StripLook
import com.walkpick.app.ui.components.look
import com.walkpick.app.ui.distanceText
import com.walkpick.app.ui.grouped
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing
import com.walkpick.app.ui.walk.DiaryWalk
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val SketchSize = 104.dp
private val ThumbSize = 32.dp
private const val THUMBS = 4

// A thumbnail of 32 dp needs an eighth of the pixels of the photo at most.
private const val THUMB_SAMPLE = 8
private const val SECONDS_IN_MINUTE = 60.0
private val Clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val ThisYear = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
private val AnotherYear = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH)

/** A walk in a list: its route, name, time and numbers, and its first photos; a tap opens it. */
@Composable
internal fun WalkCard(
    item: DiaryWalk,
    unit: DistanceUnit,
    onWalk: (String) -> Unit,
    photo: suspend (FindEntity) -> File?,
    // Inside a sheet the card is outlined: a shadow would stand out on the white.
    outlined: Boolean = false,
) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    val walk = item.walk
    AppCard(outlined = outlined) {
        Row(
            modifier = Modifier
                .clickable { onWalk(walk.id) }
                .padding(start = Spacing.sm, top = Spacing.sm, end = Spacing.m, bottom = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RouteSketch(item.sketch, Modifier.size(SketchSize), look = walk.look())
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = SketchSize)
                    .padding(top = Spacing.xs / 2),
            ) {
                val minutes = (walk.durationS / SECONDS_IN_MINUTE).roundToInt()
                val start = Clock.format(walk.startedAt.atZone(ZoneId.systemDefault()))
                val distance = distanceText(walk.distanceM, unit)
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
                if (item.finds.isEmpty()) {
                    val none = stringResource(R.string.diary_no_photos)
                    Text(none, style = type.bodyMedium, color = colors.textSecondary)
                } else {
                    val look = StripLook(
                        ThumbSize,
                        THUMBS,
                        Spacing.xs,
                        MaterialTheme.shapes.extraSmall,
                        MaterialTheme.typography.labelSmall,
                        THUMB_SAMPLE,
                    )
                    PhotoStrip(item.finds, photo, look)
                }
            }
            RowChevron()
        }
    }
}

/** A day as a walk card calls it: Today, Yesterday, or its date. */
@Composable
internal fun dayTitle(day: LocalDate, today: LocalDate): String = when {
    day == today -> stringResource(R.string.diary_today)
    day == today.minusDays(1) -> stringResource(R.string.diary_yesterday)
    day.year == today.year -> ThisYear.format(day)
    else -> AnotherYear.format(day)
}
