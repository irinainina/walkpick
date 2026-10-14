package com.walkpick.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.data.walk.Season
import com.walkpick.app.ui.screens.SignInCodeScreen
import com.walkpick.app.ui.screens.SignInEmailScreen
import com.walkpick.app.ui.screens.SignInScreen
import com.walkpick.app.ui.theme.WalkPickTheme

/**
 * adb shell am start -n com.walkpick.app/.debug.GalleryActivity [--es page <page>] [--es season winter]
 * Pages: charts, signin, signin-email, signin-code, steps, today, today-off, today-walking,
 * stats-week, stats-month, stats-year,
 * stats-all,
 * health-mood, health-weight, health-pressure, health-glucose, health-empty, map, walk, active-walk,
 * walk-summary, profile, settings, settings-off; a new user's screens: today-new, stats-new,
 * stats-month-new, map-new, diary-new, gallery-new, gallery-best-new, profile-new.
 */
class GalleryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val season = intent.getStringExtra("season")?.uppercase() ?: Season.AUTUMN.name
            WalkPickTheme(Season.valueOf(season)) {
                val page = intent.getStringExtra("page").orEmpty()
                if (page.endsWith("-new")) NewUserGallery(page) else Page(page)
            }
        }
    }
}

@Composable
private fun Page(page: String) {
    when (page) {
        "charts" -> ChartGallery()
        "nav" -> NavGallery()
        "last-walk" -> LastWalkGallery()
        "periods-week", "periods-month", "periods-year", "periods-all" -> PeriodsGallery(page)
        "signin" -> SignInScreen(onGoogle = {}, onEmail = {})
        "signin-email" -> SignInEmailScreen("ira@example.com", {}, {}, {}, busy = false, problem = null)
        "signin-code" -> SignInCodeScreen("ira@example.com", "42", {}, {}, 48, {}, {}, problem = null)
        "today" -> TodayGallery(StepsAccess.COUNTING)
        "today-off" -> TodayGallery(StepsAccess.NOT_ALLOWED)
        "today-walking" -> TodayGallery(StepsAccess.COUNTING, walkSeconds = 1935)
        "health-mood", "health-weight", "health-pressure", "health-glucose", "health-empty" ->
            HealthGallery(page)
        "map" -> MapGallery()
        "walk" -> WalkCheck()
        "active-walk" -> ActiveWalkGallery()
        "active-walk-mood" -> ActiveWalkGallery(asked = true)
        "walk-summary" -> WalkSummaryGallery()
        "gallery", "gallery-best", "gallery-select" -> GalleryGallery(page)
        "find-card", "find-unknown" -> FindCardGallery(unknown = page == "find-unknown")
        "camera", "camera-what", "camera-identifying" ->
            CameraGallery(identifying = page == "camera-identifying", asking = page == "camera-what")
        "profile" -> ProfileGallery()
        "settings" -> SettingsGallery(StepsAccess.COUNTING)
        "settings-off" -> SettingsGallery(StepsAccess.NOT_ALLOWED)
        "stats-week", "stats-month", "stats-year", "stats-all" -> StatsGallery(page)
        else -> ComponentGallery()
    }
}
