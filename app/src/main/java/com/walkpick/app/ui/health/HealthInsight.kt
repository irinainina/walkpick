package com.walkpick.app.ui.health

import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.ui.localDay
import com.walkpick.app.ui.stats.StatsTab
import java.time.LocalDate
import java.time.ZoneId

/** Mood only: on [up] of the [walks] that have both moods the mood after was better than before. */
data class HealthInsight(val up: Int, val walks: Int)

/** A count of the walker's own walks, never a cause; null when no walk of the period has both moods. */
internal fun moodInsight(
    walks: List<WalkEntity>,
    period: StatsTab,
    today: LocalDate,
    zone: ZoneId,
): HealthInsight? {
    val first = walks.minOfOrNull { it.startedAt.localDay(zone) } ?: return null
    val range = healthRange(period, today, first)
    val both = walks
        .filter { it.startedAt.localDay(zone) in range }
        .mapNotNull { walk -> walk.moodBefore?.let { before -> walk.moodAfter?.let { before to it } } }
    if (both.isEmpty()) return null
    return HealthInsight(both.count { (before, after) -> after > before }, both.size)
}
