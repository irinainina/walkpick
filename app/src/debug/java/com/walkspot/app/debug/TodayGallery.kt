package com.walkspot.app.debug

import androidx.compose.runtime.Composable
import com.walkspot.app.data.steps.StepsAccess
import com.walkspot.app.ui.components.Bar
import com.walkspot.app.ui.components.BarKind
import com.walkspot.app.ui.screens.TodayScreen
import com.walkspot.app.ui.today.TodayState
import java.time.LocalDateTime

private val week = listOf(
    Bar(39 / 48f, "M"),
    Bar(26 / 48f, "T"),
    Bar(31 / 48f, "W", BarKind.Today),
    Bar(0f, "T"),
    Bar(0f, "F"),
    Bar(0f, "S"),
    Bar(0f, "S"),
)

/** Today with the data of the Figma frames, for the comparison with the design. */
@Composable
fun TodayGallery(access: StepsAccess) {
    val counting = access == StepsAccess.COUNTING
    TodayScreen(
        state = TodayState(
            now = LocalDateTime.of(2026, 9, 30, 9, 41),
            name = "Ira",
            access = access,
            steps = if (counting) 6420 else 0,
            distanceM = if (counting) 4800 else 0,
            activeMinutes = if (counting) 52 else 0,
            streak = 5,
            week = week,
        ),
        onAllowSteps = {},
        onStartWalk = {},
        onStats = {},
        onReminders = {},
    )
}
