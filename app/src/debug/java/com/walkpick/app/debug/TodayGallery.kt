package com.walkpick.app.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.data.walk.WalkWeather
import com.walkpick.app.ui.screens.LastWalkCard
import com.walkpick.app.ui.screens.TodayActions
import com.walkpick.app.ui.screens.TodayScreen
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing
import com.walkpick.app.ui.today.TodayState
import com.walkpick.app.ui.walk.DiaryWalk
import com.walkpick.app.ui.walk.WalkEnding
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

// The walk of the frame: yesterday at 18:40, its route shaped like the one drawn there.
private val evening = LocalDateTime.of(2026, 9, 29, 18, 40).atZone(ZoneId.systemDefault()).toInstant()
internal val ringSketch = listOf(
    0.00f to 0.73f, 0.07f to 0.24f, 0.60f to 0.14f, 0.62f to 0.00f, 0.92f to 0.00f, 0.98f to 0.30f,
    1.00f to 0.60f, 0.86f to 0.92f, 0.44f to 1.00f, 0.00f to 0.73f,
)

// The same ring laid on Lebedyn, as the walks table keeps a route: "longitude latitude, ...".
private val ring = ringSketch.joinToString(",") { (x, y) -> "${34.475 + x * 0.01} ${50.585 - y * 0.006}" }
private val finds = List(7) { index ->
    FindEntity(
        id = "find$index",
        walkId = "walk",
        takenAt = evening.plusSeconds(300L * index),
        latitude = 50.58,
        longitude = 34.48,
        photoPath = "none",
        updatedAt = evening,
    )
}
internal val lastWalk = DiaryWalk(
    walk = WalkEntity(
        id = "walk",
        number = 312,
        startedAt = evening,
        endedAt = evening.plusSeconds(2880),
        durationS = 2880,
        steps = 5230,
        distanceM = 3900,
        name = "Misty Evening",
        route = ring,
        moodBefore = 3,
        moodAfter = 5,
        updatedAt = evening,
    ),
    photos = 7,
    sketch = ringSketch,
    finds = finds,
)

/** Today with the data of the Figma frames, for the comparison with the design. */
@Composable
fun TodayGallery(access: StepsAccess, walkSeconds: Long? = null, paused: Boolean = false) {
    TodayScreen(
        state = TodayState(
            now = LocalDateTime.of(2026, 9, 30, 9, 41),
            name = "Ira",
            access = access,
        ),
        walkSeconds = walkSeconds,
        walkPaused = paused,
        walkEnding = WalkEnding.KeptWithMood,
        weather = WalkWeather(code = 2, celsius = 13.0, windMs = 2.0),
        lastWalk = lastWalk,
        actions = TodayActions(
            onAllowSteps = {},
            onWalk = {},
            onProfile = {},
        ),
        onStartWalk = {},
        onStopWalk = {},
        modifier = Modifier.navigationBarsPadding(),
    )
}

/** The Last walk card alone at the top of the screen, for the comparison with its Figma node. */
@Composable
fun LastWalkGallery() {
    Box(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .statusBarsPadding()
            .padding(Spacing.ml),
    ) {
        LastWalkCard(lastWalk, LocalDate.of(2026, 9, 30), DistanceUnit.KM, photo = { null }, onOpen = {})
    }
}
