package com.walkpick.app.debug

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.data.walk.WalkWeather
import com.walkpick.app.ui.components.Bar
import com.walkpick.app.ui.components.BarKind
import com.walkpick.app.ui.screens.TodayActions
import com.walkpick.app.ui.screens.TodayScreen
import com.walkpick.app.ui.today.TodayState
import com.walkpick.app.ui.walk.WalkEnding
import java.time.LocalDateTime
import java.time.ZoneId

// The walk of the frame: yesterday at 18:40, its route shaped like the one drawn there.
private val evening = LocalDateTime.of(2026, 9, 29, 18, 40).atZone(ZoneId.systemDefault()).toInstant()
internal val ringSketch = listOf(
    0.00f to 0.73f, 0.07f to 0.24f, 0.60f to 0.14f, 0.62f to 0.00f, 0.92f to 0.00f, 0.98f to 0.30f,
    1.00f to 0.60f, 0.86f to 0.92f, 0.44f to 1.00f, 0.00f to 0.73f,
)

// The last walk of the Home frame.
private val lastWalk = WalkEntity(
    id = "walk",
    number = 312,
    startedAt = evening,
    endedAt = evening.plusSeconds(3120),
    durationS = 3120,
    steps = 6420,
    distanceM = 4800,
    name = "Misty Evening",
    updatedAt = evening,
)

// The bars of the frame on their track of 48.
private val week = listOf(39, 26, 31, 0, 0, 0, 0).mapIndexed { index, height ->
    val kind = if (index == 2) BarKind.Today else if (height > 0) BarKind.Walked else BarKind.Past
    Bar(height / 48f, "MTWTFSS"[index].toString(), kind)
}

/** Home with the data of the Figma frames, for the comparison with the design. */
@Composable
fun TodayGallery(access: StepsAccess, walkSeconds: Long? = null, paused: Boolean = false) {
    TodayScreen(
        state = TodayState(
            now = LocalDateTime.of(2026, 9, 30, 9, 41),
            name = "Ira",
            access = access,
            lastWalk = lastWalk,
            dailySteps = 10_000,
            streak = 5,
            week = week,
        ),
        walkSeconds = walkSeconds,
        walkPaused = paused,
        walkEnding = WalkEnding.KeptWithMood,
        walkMoods = true,
        weather = WalkWeather(code = 2, celsius = 13.0, windMs = 2.0),
        actions = TodayActions(onAllowSteps = {}, onProfile = {}, onWeek = {}, onDay = {}),
        onStartWalk = {},
        onStopWalk = {},
        modifier = Modifier.navigationBarsPadding(),
    )
}
