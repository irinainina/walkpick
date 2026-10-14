package com.walkpick.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.data.walk.title
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.Mood
import com.walkpick.app.ui.components.MoodFace
import com.walkpick.app.ui.components.PhotoStrip
import com.walkpick.app.ui.components.RoutePicture
import com.walkpick.app.ui.components.RowChevron
import com.walkpick.app.ui.components.StripLook
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

// The odd steps are the frame as drawn, kept by the author (2026-10-14).
private val NameTop = 4.dp
private val RouteEdge = 56.dp
private val MoodSize = 24.dp
private val NumberWidth = 72.dp
private val ThumbSize = 48.dp
private const val THUMBS = 5

// A thumbnail of 48 dp needs a quarter of the pixels of the photo at most.
private const val THUMB_SAMPLE = 4
private const val SECONDS_IN_MINUTE = 60.0
private val Clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** The latest walk on Walk: its name, its route on the map, its numbers and photos; a tap opens it. */
@Composable
internal fun LastWalkCard(
    item: DiaryWalk,
    today: LocalDate,
    unit: DistanceUnit,
    photo: suspend (FindEntity) -> File?,
    onOpen: () -> Unit,
) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    val walk = item.walk
    val started = walk.startedAt.atZone(ZoneId.systemDefault())
    val minutes = (walk.durationS / SECONDS_IN_MINUTE).roundToInt()
    val duration = stringResource(R.string.walk_minutes, minutes)
    AppCard {
        Column(
            Modifier
                .clickable(onClick = onOpen)
                .padding(start = Spacing.ml, top = Spacing.m, end = Spacing.ml, bottom = Spacing.ml),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val day = started.toLocalDate()
                // Today and Yesterday go small after the dot; a date keeps its capitals.
                val said = dayTitle(day, today).let { if (day >= today.minusDays(1)) it.lowercase() else it }
                Text(
                    text = stringResource(R.string.today_last_walk_when, said),
                    modifier = Modifier.weight(1f),
                    style = type.bodyMedium,
                    color = colors.textSecondary,
                )
                RowChevron()
            }
            Spacer(Modifier.height(NameTop))
            Text(
                text = walk.title(),
                style = type.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${Clock.format(started)} · $duration",
                    modifier = Modifier.weight(1f),
                    style = type.bodyMedium,
                    color = colors.textSecondary,
                )
                Moods(walk)
            }
            // The faces make this row 24 high: the time sits 2 lower by itself, as in the frame.
            Spacer(Modifier.height(Spacing.sm))
            RoutePicture(
                walk = walk,
                finds = item.finds,
                edge = RouteEdge,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium),
            )
            Spacer(Modifier.height(Spacing.m))
            Row {
                Number(grouped(walk.steps), R.string.walk_steps)
                Number(distanceText(walk.distanceM, unit), R.string.walk_distance)
                Number(duration, R.string.walk_time)
                Number(item.photos.toString(), R.string.today_photos)
            }
            if (item.finds.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.m))
                val look = StripLook(
                    tile = ThumbSize,
                    count = THUMBS,
                    gap = Spacing.s,
                    shape = MaterialTheme.shapes.small,
                    more = type.titleMedium,
                    sample = THUMB_SAMPLE,
                )
                PhotoStrip(item.finds, photo, look)
            }
        }
    }
}

// The mood before the walk and after it; a walk without one shows nothing here.
@Composable
private fun Moods(walk: WalkEntity) {
    val before = walk.moodBefore ?: return
    val after = walk.moodAfter ?: return
    // The frame keeps the faces a step in from the edge, under the chevron.
    Row(Modifier.padding(end = Spacing.ml), verticalAlignment = Alignment.CenterVertically) {
        MoodFace(Mood.of(before), size = MoodSize)
        Text(
            text = "→",
            modifier = Modifier.padding(horizontal = Spacing.xs),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.textSecondary,
        )
        MoodFace(Mood.of(after), size = MoodSize)
    }
}

@Composable
private fun Number(value: String, labelRes: Int) {
    Column(Modifier.width(NumberWidth)) {
        val type = MaterialTheme.typography
        Text(value, style = type.labelLarge, color = AppTheme.colors.textPrimary, maxLines = 1)
        Text(stringResource(labelRes), style = type.bodySmall, color = AppTheme.colors.textSecondary)
    }
}
