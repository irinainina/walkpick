package com.walkspot.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.walk.routeBreaks
import com.walkspot.app.ui.theme.AppTheme

/**
 * How the line of a walk is drawn besides its points: where a pause breaks it and the moods the
 * walk began and ended in, 1 (awful) to 5 (great).
 */
data class RouteLook(
    /** The indexes of the points a leg begins at after a pause. */
    val breaks: Set<Int> = emptySet(),
    val moodBefore: Int? = null,
    val moodAfter: Int? = null,
) {
    /** [points] cut into the legs the walk was made in; the way between two legs was not walked. */
    fun <T> legs(points: List<T>): List<List<T>> {
        val cuts = listOf(0) + breaks.filter { it in 1 until points.size }.sorted() + points.size
        return cuts.zipWithNext { from, to -> points.subList(from, to) }
    }
}

fun WalkEntity.look() = RouteLook(routeBreaks(breaks), moodBefore, moodAfter)

/**
 * The colours of the two ends of the line: the moods of the walk, or the gradient of the app when
 * no mood was said. One mood said colours the whole line.
 */
@Composable
fun RouteLook.ends(): Pair<Color, Color> {
    val colors = AppTheme.colors
    val first = moodBefore ?: moodAfter
    val last = moodAfter ?: moodBefore
    if (first == null || last == null) return colors.accent to colors.accentLight
    val shade = { mood: Int -> colors.moods[(mood - 1).coerceIn(colors.moods.indices)] }
    return shade(first) to shade(last)
}
