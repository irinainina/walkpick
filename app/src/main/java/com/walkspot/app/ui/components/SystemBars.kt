package com.walkspot.app.ui.components

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/** White system icons and clear bars while a dark screen is shown; the light bars return after it. */
@Composable
fun DarkSystemBars() {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    DisposableEffect(activity) {
        val clear = SystemBarStyle.dark(Color.TRANSPARENT)
        activity.enableEdgeToEdge(clear, clear)
        onDispose { activity.enableEdgeToEdge() }
    }
}
