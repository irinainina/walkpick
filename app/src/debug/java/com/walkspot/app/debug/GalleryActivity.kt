package com.walkspot.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.walkspot.app.data.steps.StepsAccess
import com.walkspot.app.ui.screens.SignInScreen
import com.walkspot.app.ui.theme.WalkSpotTheme

/**
 * adb shell am start -n com.walkspot.app/.debug.GalleryActivity [--es page <page>]
 * Pages: charts, signin, steps, today, today-off, stats-week, stats-month, stats-year, stats-all,
 * health-mood, health-weight, health-pressure, health-glucose, health-empty, map, walk, active-walk,
 * walk-summary, profile, settings, settings-off.
 */
class GalleryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WalkSpotTheme {
                when (val page = intent.getStringExtra("page")) {
                    "charts" -> ChartGallery()
                    "signin" -> SignInScreen(onGoogle = {}, onEmail = {})
                    "today" -> TodayGallery(StepsAccess.COUNTING)
                    "today-off" -> TodayGallery(StepsAccess.NOT_ALLOWED)
                    "health-mood", "health-weight", "health-pressure", "health-glucose", "health-empty" ->
                        HealthGallery(page)
                    "map" -> MapGallery()
                    "walk" -> WalkCheck()
                    "active-walk" -> ActiveWalkGallery()
                    "walk-summary" -> WalkSummaryGallery()
                    "find-card" -> FindCardGallery()
                    "camera", "camera-identifying" -> CameraGallery(page == "camera-identifying")
                    "profile" -> ProfileGallery()
                    "settings" -> SettingsGallery(StepsAccess.COUNTING)
                    "settings-off" -> SettingsGallery(StepsAccess.NOT_ALLOWED)
                    "stats-week", "stats-month", "stats-year", "stats-all" -> StatsGallery(page)
                    else -> ComponentGallery()
                }
            }
        }
    }
}
