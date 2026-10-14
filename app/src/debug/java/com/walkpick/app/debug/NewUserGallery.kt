package com.walkpick.app.debug

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walkpick.app.data.walk.WalkWeather
import com.walkpick.app.ui.gallery.GalleryState
import com.walkpick.app.ui.gallery.GalleryTab
import com.walkpick.app.ui.profile.ProfileState
import com.walkpick.app.ui.screens.GalleryActions
import com.walkpick.app.ui.screens.GalleryScreen
import com.walkpick.app.ui.screens.ProfileScreen
import com.walkpick.app.ui.screens.TodayActions
import com.walkpick.app.ui.screens.TodayScreen
import com.walkpick.app.ui.today.TodayState
import com.walkpick.app.ui.walk.WalkEnding
import java.time.LocalDate
import java.time.LocalDateTime

private val today = LocalDate.of(2026, 9, 30)

/** The screens as somebody who has just signed in meets them: no walks, no photos, no entries. */
@Composable
fun NewUserGallery(page: String) {
    when (page) {
        "today-new" -> TodayScreen(
            state = TodayState(
                now = LocalDateTime.of(2026, 9, 30, 9, 41),
                name = "Ira",
                newcomer = true,
            ),
            walkSeconds = null,
            walkPaused = false,
            walkEnding = WalkEnding.Dropped,
            walkMoods = true,
            // The weather of the frame; a newcomer is greeted the same way.
            weather = WalkWeather(code = 2, celsius = 13.0, windMs = 2.0),
            lastWalk = null,
            actions = TodayActions(
                onAllowSteps = {},
                onWalk = {},
                onProfile = {},
            ),
            onStartWalk = {},
            onStopWalk = {},
            modifier = Modifier.navigationBarsPadding(),
        )
        "gallery-new", "gallery-best-new" -> {
            val tab = if (page == "gallery-new") GalleryTab.All else GalleryTab.Best
            GalleryScreen(GalleryState(tab, today), GalleryActions({ null }, {}, {}, {}, {}, {}, {}))
        }
        else -> ProfileScreen(ProfileState(today, "Ira", "ira@example.com"), {}, { _, _ -> }, {})
    }
}
