package com.walkspot.app.debug

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.steps.PeriodStats
import com.walkspot.app.data.steps.monthOf
import com.walkspot.app.data.steps.weekOf
import com.walkspot.app.ui.components.Bar
import com.walkspot.app.ui.components.BarKind
import com.walkspot.app.ui.gallery.GalleryState
import com.walkspot.app.ui.gallery.GalleryTab
import com.walkspot.app.ui.health.HealthTile
import com.walkspot.app.ui.map.MapState
import com.walkspot.app.ui.map.MapTab
import com.walkspot.app.ui.profile.ProfileState
import com.walkspot.app.ui.screens.GalleryActions
import com.walkspot.app.ui.screens.GalleryScreen
import com.walkspot.app.ui.screens.MapActions
import com.walkspot.app.ui.screens.MapScreen
import com.walkspot.app.ui.screens.ProfileScreen
import com.walkspot.app.ui.screens.StatsScreen
import com.walkspot.app.ui.screens.TodayActions
import com.walkspot.app.ui.screens.TodayScreen
import com.walkspot.app.ui.stats.CalendarDay
import com.walkspot.app.ui.stats.StatsState
import com.walkspot.app.ui.stats.StatsTab
import com.walkspot.app.ui.today.TodayState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private val today = LocalDate.of(2026, 9, 30)

private val week = "MTWTFSS".mapIndexed { index, day ->
    Bar(0f, day.toString(), if (index == 2) BarKind.Today else BarKind.Past)
}

private val stats = StatsState(
    tab = StatsTab.Week,
    today = today,
    range = weekOf(today),
    canGoBack = false,
    canGoForward = false,
    stats = PeriodStats(0, 0, 0, null),
    bars = week,
)

/** The screens as somebody who has just signed in meets them: no walks, no photos, no entries. */
@Composable
fun NewUserGallery(page: String) {
    when (page) {
        "today-new" -> TodayScreen(
            state = TodayState(
                now = LocalDateTime.of(2026, 9, 30, 9, 41),
                name = "Ira",
                newcomer = true,
                week = week,
                health = listOf(HealthTile(HealthKind.MOOD, null), HealthTile(HealthKind.WEIGHT, null)),
            ),
            walkSeconds = null,
            walkKept = false,
            weather = null,
            lastWalk = null,
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
        "stats-new", "stats-month-new" -> {
            val september = YearMonth.of(2026, 9)
            val month = stats.copy(
                tab = StatsTab.Month,
                range = monthOf(september),
                bars = emptyList(),
                calendar = List(september.lengthOfMonth()) { CalendarDay(september.atDay(it + 1), false) },
            )
            StatsScreen(if (page == "stats-new") stats else month, {}, {}, {}, {}, {}, {}, {})
        }
        "map-new", "diary-new" -> MapScreen(
            state = MapState(if (page == "map-new") MapTab.Map else MapTab.Diary, today, loaded = true),
            actions = MapActions({}, {}, { _, _ -> }, {}, {}, {}, {}),
            photo = { null },
        )
        "gallery-new", "gallery-best-new" -> {
            val tab = if (page == "gallery-new") GalleryTab.All else GalleryTab.Best
            GalleryScreen(GalleryState(tab, today), GalleryActions({ null }, {}, {}, {}, {}, {}, {}))
        }
        else -> ProfileScreen(ProfileState(today, "Ira", "ira@example.com"), {}, { _, _ -> }, {})
    }
}
