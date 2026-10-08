package com.walkspot.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.data.settings.DistanceUnit
import com.walkspot.app.data.walk.title
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.RouteSketch
import com.walkspot.app.ui.components.RowChevron
import com.walkspot.app.ui.distanceText
import com.walkspot.app.ui.grouped
import com.walkspot.app.ui.map.DiaryWalk
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val RouteHeight = 104.dp
private val RouteLine = 4.dp
private val RouteStart = 24.dp
private val NumberWidth = 72.dp
private const val SECONDS_IN_MINUTE = 60.0
private val Clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** The latest walk on Today: its name, its route and its numbers; a tap opens its summary. */
@Composable
internal fun LastWalkCard(item: DiaryWalk, today: LocalDate, unit: DistanceUnit, onOpen: () -> Unit) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    val walk = item.walk
    val started = walk.startedAt.atZone(ZoneId.systemDefault())
    AppCard {
        Column(
            Modifier
                .clickable(onClick = onOpen)
                .padding(start = Spacing.ml, top = Spacing.m, end = Spacing.m, bottom = Spacing.m),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.today_last_walk),
                    modifier = Modifier.weight(1f),
                    style = type.titleSmall,
                    color = colors.textPrimary,
                )
                RowChevron()
            }
            Spacer(Modifier.height(Spacing.s))
            Row(Modifier.padding(end = Spacing.xs)) {
                Text(
                    text = walk.title(),
                    modifier = Modifier.weight(1f),
                    style = type.labelMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(Spacing.s))
                Text(
                    text = "${dayTitle(started.toLocalDate(), today)}, ${Clock.format(started)}",
                    style = type.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.height(Spacing.s))
            val route = Modifier.fillMaxWidth().padding(end = Spacing.xs).height(RouteHeight)
            RouteSketch(item.sketch, route, RouteLine, RouteStart)
            Spacer(Modifier.height(Spacing.sm))
            Row {
                val minutes = (walk.durationS / SECONDS_IN_MINUTE).roundToInt()
                Number(grouped(walk.steps), R.string.walk_steps)
                Number(distanceText(walk.distanceM, unit), R.string.walk_distance)
                Number(stringResource(R.string.walk_minutes, minutes), R.string.walk_time)
                Number(item.photos.toString(), R.string.today_photos)
            }
        }
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
