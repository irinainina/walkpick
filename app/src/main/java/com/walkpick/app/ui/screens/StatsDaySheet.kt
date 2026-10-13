package com.walkpick.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.walk.title
import com.walkpick.app.ui.components.AppSheet
import com.walkpick.app.ui.components.ListRow
import com.walkpick.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Title = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)
private val Clock = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** The walks of a day that has several: each row opens the summary of its walk. */
@Composable
internal fun DayWalksSheet(
    day: LocalDate,
    walks: List<WalkEntity>,
    onWalk: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(day.format(Title), onDismiss) {
        Spacer(Modifier.height(Spacing.s))
        walks.forEachIndexed { index, walk ->
            ListRow(
                label = walk.title(),
                value = Clock.format(walk.startedAt.atZone(ZoneId.systemDefault())),
                divider = index < walks.lastIndex,
                onClick = { onWalk(walk.id) },
            )
        }
    }
}
