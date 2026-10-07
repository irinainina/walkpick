package com.walkspot.app.debug

import androidx.compose.runtime.Composable
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.ui.gallery.GalleryGroup
import com.walkspot.app.ui.gallery.GalleryState
import com.walkspot.app.ui.gallery.GalleryTab
import com.walkspot.app.ui.screens.GalleryActions
import com.walkspot.app.ui.screens.GalleryScreen
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private val today = LocalDate.of(2026, 9, 30)

private fun at(day: Int, hour: Int) =
    LocalDateTime.of(2026, 9, day, hour, 40).atZone(ZoneId.systemDefault()).toInstant()

private fun walk(id: String, name: String, day: Int, hour: Int) =
    WalkEntity(id, 0, at(day, hour), at(day, hour + 1), 2880, name = name, updatedAt = at(day, hour))

// The photos of the Figma frame: seven and three, the starred ones where the frame has its stars.
private fun finds(walk: WalkEntity, count: Int, best: Set<Int>) = List(count) { index ->
    val taken = walk.startedAt.plusSeconds(index * 60L)
    val starred = index in best
    FindEntity("${walk.id}-$index", walk.id, taken, 50.45, 30.52, "", isBest = starred, updatedAt = taken)
}

private val evening = walk("evening", "Misty Evening Walk", 29, 18)
private val morning = walk("morning", "Sunny Morning Walk", 27, 9)
private val groups = listOf(
    GalleryGroup(evening, finds(evening, 7, setOf(0, 4))),
    GalleryGroup(morning, finds(morning, 3, setOf(1))),
)

/** The gallery with the photos of the Figma frames, for the comparison with the design. */
@Composable
fun GalleryGallery(page: String) {
    val all = groups.flatMap { it.finds }
    val state = GalleryState(
        tab = if (page == "gallery-best") GalleryTab.Best else GalleryTab.All,
        today = today,
        groups = groups,
        best = all.take(6).map { it.copy(isBest = true) },
        selected = setOf("evening-1", "evening-2", "evening-5").takeIf { page == "gallery-select" },
    )
    GalleryScreen(state, GalleryActions({ null }, {}, {}, {}, {}, {}, {}))
}
