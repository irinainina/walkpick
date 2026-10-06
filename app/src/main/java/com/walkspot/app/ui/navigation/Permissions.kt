package com.walkspot.app.ui.navigation

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.net.toUri

// Asked only on Android 10 and later: before that the step sensor is open and nothing offers to ask.
internal const val STEPS_PERMISSION = "android.permission.ACTIVITY_RECOGNITION"

// Asked only on Android 13 and later: before that notifications need no permission.
internal const val NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"

/**
 * Asks Android for [permission] and reports the answer. Refused for good, Android shows
 * no question any more, so the app's page in the phone's settings is opened instead.
 */
@Composable
internal fun rememberPermissionRequest(permission: String, onAnswer: (Boolean) -> Unit): () -> Unit {
    val activity = LocalActivity.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && activity?.shouldShowRequestPermissionRationale(permission) == false) {
            val settings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            activity.startActivity(settings.setData("package:${activity.packageName}".toUri()))
        }
        onAnswer(granted)
    }
    return { ask.launch(permission) }
}
