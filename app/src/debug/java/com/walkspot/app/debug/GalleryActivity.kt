package com.walkspot.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.walkspot.app.ui.theme.WalkSpotTheme

/** adb shell am start -n com.walkspot.app/.debug.GalleryActivity [--es page charts] */
class GalleryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WalkSpotTheme {
                if (intent.getStringExtra("page") == "charts") ChartGallery() else ComponentGallery()
            }
        }
    }
}
