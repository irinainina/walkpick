package com.walkspot.app.ui.screens

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
import com.walkspot.app.R
import com.walkspot.app.data.settings.DistanceUnit
import com.walkspot.app.data.walk.TrackPoint
import com.walkspot.app.data.walk.distanceM
import com.walkspot.app.ui.components.AppCard
import com.walkspot.app.ui.components.PhotoBox
import com.walkspot.app.ui.distanceText
import com.walkspot.app.ui.find.look
import com.walkspot.app.ui.map.MapPick
import com.walkspot.app.ui.theme.AppTheme
import com.walkspot.app.ui.theme.Spacing
import java.time.Instant

private val PhotoSize = 88.dp
private const val THUMB_SAMPLE = 4

/** The find or the place tapped on the map: its photo, its name, how far it is; a tap opens its card. */
@Composable
internal fun MapPickCard(
    pick: MapPick,
    me: TrackPoint?,
    unit: DistanceUnit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = MaterialTheme.typography
    val colors = AppTheme.colors
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
                val title = pick.title ?: pick.find?.let { findTitle(it) }.orEmpty()
                Text(title, style = type.labelLarge, color = colors.textPrimary, maxLines = 1)
                Spacer(Modifier.height(Spacing.xs / 2))
                // A place of Wikipedia is a landmark; a find is what its kind says.
                val kind = if (pick.find == null) R.string.place_landmark else pick.kind.look().label
                val here = TrackPoint(pick.latitude, pick.longitude, Instant.EPOCH, 0f)
                val away = me?.let { distanceText(distanceM(it, here).toInt(), unit) }
                val line = listOfNotNull(stringResource(kind), away).joinToString(" · ")
                Text(line, style = type.labelSmall, color = colors.accent, maxLines = 1)
                if (!pick.text.isNullOrBlank()) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = pick.text,
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
