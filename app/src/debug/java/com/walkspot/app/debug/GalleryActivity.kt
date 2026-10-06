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
 * Pages: charts, signin, steps, today, today-off.
 */
class GalleryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WalkSpotTheme {
                when (intent.getStringExtra("page")) {
                    "charts" -> ChartGallery()
                    "signin" -> SignInScreen(onGoogle = {}, onEmail = {})
                    "steps" -> StepsCheck()
                    "today" -> TodayGallery(StepsAccess.COUNTING)
                    "today-off" -> TodayGallery(StepsAccess.NOT_ALLOWED)
                    else -> ComponentGallery()
                }
            }
        }
    }
}
