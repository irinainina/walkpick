package com.walkspot.app.debug

import androidx.compose.runtime.Composable
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.ui.find.FindCardState
import com.walkspot.app.ui.screens.FindCardScreen
import java.time.LocalDateTime
import java.time.ZoneId

private val taken = LocalDateTime.of(2026, 9, 29, 18, 52).atZone(ZoneId.systemDefault()).toInstant()

private val yarrow = FindEntity(
    id = "find",
    walkId = "walk",
    takenAt = taken,
    latitude = 50.4478,
    longitude = 30.5222,
    photoPath = "",
    kind = FindKind.PLANT,
    name = "Common yarrow",
    latinName = "Achillea millefolium",
    match = 0.94f,
    description = "A hardy wildflower with flat clusters of tiny white flowers. It grows in meadows and " +
        "along paths and blooms from June to September.",
    wikipediaUrl = "https://en.wikipedia.org/wiki/Achillea_millefolium",
    updatedAt = taken,
)

private val walk = WalkEntity(
    id = "walk",
    number = 312,
    startedAt = taken.minusSeconds(720),
    endedAt = taken.plusSeconds(2160),
    durationS = 2880,
    name = "Misty Evening Walk",
    updatedAt = taken,
)

/** The card of a find with the data of the Figma frame, for the comparison with the design. */
@Composable
fun FindCardGallery() {
    FindCardScreen(FindCardState(yarrow, walk = walk), onBack = {}, onBest = {}, onDelete = {}, onWalk = {})
}
