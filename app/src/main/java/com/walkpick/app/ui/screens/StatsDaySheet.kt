package com.walkpick.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walkpick.app.R
import com.walkpick.app.ui.components.AppSheet
import com.walkpick.app.ui.counted
import com.walkpick.app.ui.distanceText
import com.walkpick.app.ui.stats.StatsState
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Title = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)

/** The walks of a day that has several, with what they add up to; each card opens its walk. */
@Composable
internal fun DayWalksSheet(state: StatsState, actions: PeriodActions) {
    val day = state.day ?: return
    val walks = state.dayWalks
    AppSheet(day.format(Title), actions.onCloseDay) {
        val sum = listOf(
            counted(walks.size, R.string.stats_walks_one, R.string.stats_walks_many),
            distanceText(walks.sumOf { it.walk.distanceM }, state.distanceUnit),
            durationText(walks.sumOf { it.walk.durationS }, hoursOnly = false),
        ).joinToString(" · ")
        Spacer(Modifier.height(Spacing.xs))
        Text(sum, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
        Spacer(Modifier.height(Spacing.m))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            walks.forEach { WalkCard(it, state.distanceUnit, actions.onWalk, actions.photo, outlined = true) }
        }
    }
}
