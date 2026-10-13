package com.walkpick.app.debug

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.steps.PeriodStats
import com.walkpick.app.data.steps.monthOf
import com.walkpick.app.data.steps.weekOf
import com.walkpick.app.ui.components.Bar
import com.walkpick.app.ui.components.BarKind
import com.walkpick.app.ui.gallery.GalleryState
import com.walkpick.app.ui.gallery.GalleryTab
import com.walkpick.app.ui.health.HealthTile
import com.walkpick.app.ui.map.MapState
import com.walkpick.app.ui.map.MapTab
import com.walkpick.app.ui.profile.ProfileState
import com.walkpick.app.ui.screens.GalleryActions
import com.walkpick.app.ui.screens.GalleryScreen
import com.walkpick.app.ui.screens.MapActions
import com.walkpick.app.ui.screens.MapScreen
import com.walkpick.app.ui.screens.ProfileScreen
import com.walkpick.app.ui.screens.StatsScreen
import com.walkpick.app.ui.screens.TodayActions
import com.walkpick.app.ui.screens.TodayScreen
import com.walkpick.app.ui.stats.CalendarDay
import com.walkpick.app.ui.stats.StatsState
import com.walkpick.app.ui.stats.StatsTab
import com.walkpick.app.ui.today.TodayState
import com.walkpick.app.ui.walk.WalkEnding
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
            walkEnding = WalkEnding.Dropped,
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
