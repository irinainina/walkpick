package com.walkspot.app.debug

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walkspot.app.data.local.HealthEntryEntity
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.steps.StepsAccess
import com.walkspot.app.data.walk.WalkWeather
import com.walkspot.app.ui.components.Bar
import com.walkspot.app.ui.components.BarKind
import com.walkspot.app.ui.health.HealthCurrent
import com.walkspot.app.ui.health.HealthTile
import com.walkspot.app.ui.map.DiaryWalk
import com.walkspot.app.ui.screens.TodayActions
import com.walkspot.app.ui.screens.TodayScreen
import com.walkspot.app.ui.today.TodayState
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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

private fun tile(
    kind: HealthKind,
    value: Double,
    change: Double? = null,
    lower: Int? = null,
    daysAgo: Long = 0,
): HealthTile {
    val entry = HealthEntryEntity(kind.name, kind, morning.minus(daysAgo, ChronoUnit.DAYS), value, lower)
    return HealthTile(kind, HealthCurrent(entry, change, towardTarget = true))
}

private val health = listOf(
    tile(HealthKind.MOOD, 4.0),
    tile(HealthKind.WEIGHT, 64.2, -1.3),
    tile(HealthKind.PRESSURE, 122.0, -4.0, 78),
    tile(HealthKind.GLUCOSE, 5.4, -0.1),
)

// The mood is yesterday's and the weight nine days old: both tiles ask for an entry.
private val staleHealth = listOf(
    tile(HealthKind.MOOD, 4.0, daysAgo = 1),
    tile(HealthKind.WEIGHT, 64.2, -1.3, daysAgo = 9),
) + health.drop(2)

// The walk of the frame: yesterday at 18:40, its route shaped like the one drawn there.
private val evening = LocalDateTime.of(2026, 9, 29, 18, 40).atZone(ZoneId.systemDefault()).toInstant()
private val route = listOf(
    0.00f to 0.73f, 0.07f to 0.24f, 0.60f to 0.14f, 0.62f to 0.00f, 0.92f to 0.00f, 0.98f to 0.30f,
    1.00f to 0.60f, 0.86f to 0.92f, 0.44f to 1.00f, 0.00f to 0.73f,
)
private val lastWalk = DiaryWalk(
    walk = WalkEntity(
        id = "walk",
        number = 312,
        startedAt = evening,
        endedAt = evening.plusSeconds(2880),
        durationS = 2880,
        steps = 5230,
        distanceM = 3900,
        name = "Misty Evening",
        updatedAt = evening,
    ),
    photos = 7,
    sketch = route,
)

/** Today with the data of the Figma frames, for the comparison with the design. */
@Composable
fun TodayGallery(access: StepsAccess, walkSeconds: Long? = null) {
    val counting = access == StepsAccess.COUNTING
    TodayScreen(
        state = TodayState(
            now = LocalDateTime.of(2026, 9, 30, 9, 41),
            name = "Ira",
            access = access,
            steps = if (counting) 6420 else 0,
            dailySteps = 10_000,
            distanceM = if (counting) 4800 else 0,
            activeMinutes = if (counting) 52 else 0,
            streak = 5,
            week = week,
            health = if (walkSeconds == null) health else staleHealth,
        ),
        walkSeconds = walkSeconds,
        walkKept = true,
        weather = WalkWeather(code = 3, celsius = 13.0, windMs = 2.0),
        lastWalk = lastWalk,
        actions = TodayActions(
            onAllowSteps = {},
            onStats = {},
            onHealth = {},
            onAddHealth = {},
            onWalk = {},
            onReminders = {},
        ),
        onStartWalk = {},
        onStopWalk = {},
        modifier = Modifier.navigationBarsPadding(),
    )
}
