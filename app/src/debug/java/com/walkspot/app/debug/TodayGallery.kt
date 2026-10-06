package com.walkspot.app.debug

import androidx.compose.runtime.Composable
import com.walkspot.app.data.local.HealthEntryEntity
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.steps.StepsAccess
import com.walkspot.app.ui.components.Bar
import com.walkspot.app.ui.components.BarKind
import com.walkspot.app.ui.health.HealthCurrent
import com.walkspot.app.ui.health.HealthTile
import com.walkspot.app.ui.screens.TodayScreen
import com.walkspot.app.ui.today.TodayState
import java.time.LocalDateTime
import java.time.ZoneId

private val week = listOf(
    Bar(39 / 48f, "M"),
    Bar(26 / 48f, "T"),
    Bar(31 / 48f, "W", BarKind.Today),
    Bar(0f, "T"),
    Bar(0f, "F"),
    Bar(0f, "S"),
    Bar(0f, "S"),
)

private val morning = LocalDateTime.of(2026, 9, 30, 8, 10).atZone(ZoneId.systemDefault()).toInstant()

private fun tile(kind: HealthKind, value: Double, change: Double? = null, lower: Int? = null): HealthTile {
    val entry = HealthEntryEntity(kind.name, kind, morning, value, lower)
    return HealthTile(kind, HealthCurrent(entry, change, towardTarget = true))
}

private val health = listOf(
    tile(HealthKind.MOOD, 4.0),
    tile(HealthKind.WEIGHT, 64.2, -1.3),
    tile(HealthKind.PRESSURE, 122.0, -4.0, 78),
    tile(HealthKind.GLUCOSE, 5.4, -0.1),
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
            health = health,
        ),
        onAllowSteps = {},
        onStartWalk = {},
        onStats = {},
        onHealth = {},
        onReminders = {},
    )
}
