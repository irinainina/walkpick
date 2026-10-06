package com.walkspot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.walkspot.app.R
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.settings.AppSettings
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.Mood
import com.walkspot.app.ui.components.MoodFace
import com.walkspot.app.ui.components.RowChevron
import com.walkspot.app.ui.health.HealthCurrent
import com.walkspot.app.ui.health.HealthTile
import com.walkspot.app.ui.localDay
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Sizes
import com.walkspot.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val BadgeSize = 36.dp
private const val NOTHING = "–"
private val ShortDay = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

/** The latest entry of every Health tab that is turned on; a tap opens Health. */
@Composable
internal fun HealthCard(tiles: List<HealthTile>, units: AppSettings, today: LocalDate, onOpen: () -> Unit) {
    AppCard {
        Column(
            Modifier
                .clickable(onClick = onOpen)
                .padding(top = Spacing.m, bottom = Spacing.m),
        ) {
            Row(
                modifier = Modifier.padding(start = Spacing.ml, end = Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.screen_health),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.colors.textPrimary,
                )
                RowChevron()
            }
            Spacer(Modifier.height(Spacing.sm))
            Column(
                modifier = Modifier.padding(horizontal = Spacing.ml),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                tiles.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        pair.forEach { Tile(it, units, today) }
                        // A tile left alone in its row keeps the width of a half.
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.Tile(tile: HealthTile, units: AppSettings, today: LocalDate) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    val current = tile.current
    Row(
        modifier = Modifier
            .weight(1f)
            .height(Sizes.control)
            .background(colors.surfaceTint, MaterialTheme.shapes.medium)
            .padding(horizontal = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (current != null && tile.kind == HealthKind.MOOD) {
            MoodFace(Mood.of(current.latest.value.roundToInt()), size = BadgeSize)
        } else {
            Box(Modifier.size(BadgeSize).background(colors.surface, CircleShape), Alignment.Center) {
                Icon(painterResource(tile.kind.iconRes()), contentDescription = null, tint = colors.accent)
            }
        }
        val (value, note, good) = tileText(tile.kind, current, units, today)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(value, style = type.labelLarge, color = colors.textPrimary, maxLines = 1)
            Text(
                text = note,
                style = type.bodySmall,
                color = if (good) colors.positive else colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

private fun HealthKind.iconRes() = when (this) {
    HealthKind.MOOD -> R.drawable.mood_good
    HealthKind.WEIGHT -> R.drawable.ic_weight
    HealthKind.PRESSURE -> R.drawable.ic_pressure
    HealthKind.GLUCOSE -> R.drawable.ic_glucose
}

/** The value, the line under it and whether that line is good news. */
@Composable
private fun tileText(
    kind: HealthKind,
    current: HealthCurrent?,
    units: AppSettings,
    today: LocalDate,
): Triple<String, String, Boolean> {
    if (current == null) return Triple(NOTHING, stringResource(kind.titleRes()), false)
    if (kind == HealthKind.MOOD) {
        val day = current.latest.measuredAt.localDay()
        val note = when (day) {
            today -> stringResource(R.string.health_day_today)
            today.minusDays(1) -> stringResource(R.string.health_day_yesterday)
            else -> ShortDay.format(day)
        }
        return Triple(stringResource(Mood.of(current.latest.value.roundToInt()).titleRes), note, false)
    }
    val (number, unit) = readingText(current.latest, units)
    val change = current.change ?: return Triple(number, unit, false)
    val size = changeText(kind, abs(change), units)
    if (size == changeText(kind, 0.0, units)) return Triple(number, unit, false)
    return Triple(number, "$unit ${if (change < 0) "↓" else "↑"} $size", current.towardTarget)
}
