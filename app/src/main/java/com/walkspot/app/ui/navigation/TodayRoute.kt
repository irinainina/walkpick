package com.walkspot.app.ui.navigation

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkspot.app.ui.screens.TodayScreen
import com.walkspot.app.ui.today.TodayViewModel

// Asked only on Android 10 and later: before that the step sensor is open and the card is never shown.
private const val STEPS_PERMISSION = "android.permission.ACTIVITY_RECOGNITION"

internal fun NavGraphBuilder.todayRoute(navController: NavHostController) {
    composable(Screen.Today.route) {
        val model: TodayViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        val owner = LocalLifecycleOwner.current
        LaunchedEffect(owner) {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) { model.keepFresh() }
        }
        val activity = LocalActivity.current
        val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Refused for good: Android shows no question any more, only its settings can allow it.
            if (!granted && activity?.shouldShowRequestPermissionRationale(STEPS_PERMISSION) == false) {
                val settings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                activity.startActivity(settings.setData("package:${activity.packageName}".toUri()))
            }
        }
        TodayScreen(
            state = state,
            onAllowSteps = { ask.launch(STEPS_PERMISSION) },
            onStartWalk = { navController.navigate(Screen.ActiveWalk.route) },
            onStats = { navController.navigate(Screen.Stats.route) },
            onReminders = { navController.navigate(Screen.Settings.route) },
        )
    }
}
