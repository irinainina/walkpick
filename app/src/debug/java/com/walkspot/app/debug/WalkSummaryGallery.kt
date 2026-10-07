package com.walkspot.app.debug

import androidx.compose.runtime.Composable
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.ui.screens.WalkSummaryScreen
import com.walkspot.app.ui.walk.WalkSummaryState
import java.time.LocalDateTime
import java.time.ZoneId

private fun evening(hour: Int, minute: Int) =
    LocalDateTime.of(2026, 9, 29, hour, minute).atZone(ZoneId.systemDefault()).toInstant()

// A ring through the centre of Kyiv shaped like the route of the Figma frame.
private const val RING = "30.5180 50.4476,30.5186 50.4500,30.5226 50.4506,30.5232 50.4528,30.5276 50.4532," +
    "30.5288 50.4510,30.5318 50.4504,30.5300 50.4480,30.5236 50.4474,30.5180 50.4476"

/** Walk summary with the numbers of the Figma frame, for the comparison with the design. */
@Composable
fun WalkSummaryGallery() {
    WalkSummaryScreen(
        state = WalkSummaryState(
            WalkEntity(
                id = "walk",
                number = 312,
                name = "Misty Evening Walk",
                startedAt = evening(18, 40),
                endedAt = evening(19, 28),
                durationS = 48 * 60,
                steps = 5230,
                distanceM = 3900,
                route = RING,
                updatedAt = evening(19, 28),
            ),
        ),
        onBack = {},
        onNote = {},
        onRename = {},
        onDelete = {},
    )
}
