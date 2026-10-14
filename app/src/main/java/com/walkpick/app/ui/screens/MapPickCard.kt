package com.walkpick.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.walk.title
import com.walkpick.app.ui.components.AppCard
import com.walkpick.app.ui.components.PhotoBox
import com.walkpick.app.ui.find.look
import com.walkpick.app.ui.localDay
import com.walkpick.app.ui.map.MapPick
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PhotoSize = 88.dp
private const val THUMB_SAMPLE = 4
private val Clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** The find tapped on the map: its photo, name, kind and day, the walk it was taken on; a tap opens it. */
@Composable
internal fun MapPickCard(pick: MapPick, today: LocalDate, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
    val find = pick.find
    AppCard(modifier) {
        Row(
            Modifier
                .clickable(onClick = onOpen)
                .padding(Spacing.sm),
        ) {
            val photo = Modifier
                .size(PhotoSize)
                .clip(MaterialTheme.shapes.medium)
            PhotoBox(pick.photo, photo, THUMB_SAMPLE)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.padding(top = Spacing.xs / 2)) {
                Text(findTitle(find), style = type.labelLarge, color = colors.textPrimary, maxLines = 1)
                Spacer(Modifier.height(Spacing.xs / 2))
                val day = dayTitle(find.takenAt.localDay(), today)
                val kind = "${stringResource(find.kind.look().label)} · $day"
                Text(kind, style = type.labelSmall, color = colors.accent, maxLines = 1)
                pick.walk?.let { walk ->
                    Spacer(Modifier.height(Spacing.xs))
                    val time = Clock.format(find.takenAt.atZone(ZoneId.systemDefault()))
                    Text(
                        text = stringResource(R.string.map_walk_time, walk.title(), time),
                        style = type.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
