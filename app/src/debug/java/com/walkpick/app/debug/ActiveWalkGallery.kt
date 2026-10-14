package com.walkpick.app.debug

import androidx.compose.runtime.Composable
import com.walkpick.app.data.walk.TrackPoint
import com.walkpick.app.ui.screens.ActiveWalkScreen
import com.walkpick.app.ui.screens.FinishWalkSheet
import com.walkpick.app.ui.walk.ActiveWalkState
import com.walkpick.app.ui.walk.WalkEnding
import java.time.Instant

// A walk through the centre of Kyiv shaped like the route of the Figma frame.
private val route = listOf(
    50.4452 to 30.5180,
    50.4478 to 30.5186,
    50.4486 to 30.5222,
    50.4512 to 30.5226,
    50.4517 to 30.5262,
    50.4488 to 30.5272,
    50.4476 to 30.5278,
).map { (latitude, longitude) -> TrackPoint(latitude, longitude, Instant.EPOCH, 8f) }

/** Active walk with the numbers of the Figma frame, for the comparison with the design. */
@Composable
fun ActiveWalkGallery(asked: Boolean = false, finishing: Boolean = false) {
    ActiveWalkScreen(
        state = ActiveWalkState(
            recording = true,
            seconds = 32 * 60 + 15,
            steps = 4120,
            metres = 2900.0,
            paceSecondsPerKm = 11 * 60 + 5,
            calories = 186,
            route = route,
            toStartM = 1200.0,
            kept = true,
            // The asked page shows a walk begun in a bad mood, with a ride in the middle of it.
            breaks = if (asked) setOf(4) else emptySet(),
            moodBefore = if (asked) 2 else null,
        ),
        onMinimize = {},
        onPause = {},
        onCamera = {},
        onStop = {},
    )
    if (finishing) FinishWalkSheet(WalkEnding.KeptWithMood, onFinish = {}, onDismiss = {})
}
