package com.walkspot.app.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkspot.app.ui.screens.ActiveWalkScreen
import com.walkspot.app.ui.walk.ActiveWalkViewModel

internal fun NavGraphBuilder.activeWalkRoute(navController: NavHostController) {
    composable(Screen.ActiveWalk.route) {
        val model: ActiveWalkViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        // The walk starts once the location is allowed; refused, the screen is left.
        val permissions = ActivityResultContracts.RequestMultiplePermissions()
        val ask = rememberLauncherForActivityResult(permissions) { answers ->
            val allowed = answers[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (allowed) model.start() else navController.popBackStack()
        }
        LaunchedEffect(Unit) {
            if (!model.state.value.recording) {
                val location = Manifest.permission.ACCESS_FINE_LOCATION
                // The notification of the recording needs its own permission from Android 13.
                val asked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(location, Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    arrayOf(location)
                }
                ask.launch(asked)
            }
        }
        ActiveWalkScreen(
            state = state,
            onMinimize = { navController.popBackStack() },
            onPause = model::togglePause,
            onCamera = { navController.navigate(Screen.Camera.route) },
            onStop = { model.stop(onDone = { navController.popBackStack() }) },
        )
    }
}
