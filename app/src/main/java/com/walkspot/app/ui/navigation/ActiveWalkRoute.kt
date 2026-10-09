package com.walkspot.app.ui.navigation

import android.Manifest
import android.app.Activity
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.location.LocationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.walkspot.app.ui.screens.ActiveWalkScreen
import com.walkspot.app.ui.screens.BatterySheet
import com.walkspot.app.ui.walk.ActiveWalkViewModel

private const val LOCATION = Manifest.permission.ACCESS_FINE_LOCATION

internal fun NavGraphBuilder.activeWalkRoute(navController: NavHostController) {
    composable(Screen.ActiveWalk.route) {
        val model: ActiveWalkViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        val activity = LocalActivity.current
        // Google's own "turn on location" dialog; whatever the answer, the walk waits for a signal.
        val dialog = ActivityResultContracts.StartIntentSenderForResult()
        val turnOn = rememberLauncherForActivityResult(dialog) {}
        // The walk starts once the exact location is allowed; refused, the screen is left.
        val permissions = ActivityResultContracts.RequestMultiplePermissions()
        val ask = rememberLauncherForActivityResult(permissions) { answers ->
            if (answers[LOCATION] == true) {
                model.start()
                activity?.askToTurnLocationOn { turnOn.launch(it) }
            } else {
                activity?.openSettingsIfRefusedForGood(LOCATION)
                navController.popBackStack()
            }
        }
        LaunchedEffect(Unit) {
            // The notification of the recording needs its own permission from Android 13.
            val location = arrayOf(LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ask.launch(location + Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ask.launch(location)
            }
        }
        val batteryPage by model.batteryPage.collectAsStateWithLifecycle()
        batteryPage?.let { page ->
            BatterySheet(
                onOpen = {
                    model.batteryAnswered()
                    activity?.startActivity(page)
                },
                onDismiss = model::batteryAnswered,
            )
        }
        val lastStart by model.lastStart.collectAsStateWithLifecycle()
        ActiveWalkScreen(
            state = state,
            waitAt = lastStart?.let { (latitude, longitude) -> LatLng(latitude, longitude) },
            onMinimize = { navController.popBackStack() },
            onPause = model::togglePause,
            onCamera = { navController.navigate(Screen.Camera.route) },
            onStop = {
                model.stop { saved ->
                    // The summary takes the place of the walk screen; a walk not worth keeping just ends.
                    navController.popBackStack()
                    if (saved != null) navController.navigate(walkSummaryRoute(saved))
                }
            },
        )
    }
}

// With the location switched off in the phone the walk would wait for a signal for ever.
private fun Activity.askToTurnLocationOn(show: (IntentSenderRequest) -> Unit) {
    // Google also asks for Wi-Fi scanning when only Wi-Fi is off; GPS alone records a walk.
    if (LocationManagerCompat.isLocationEnabled(getSystemService(LocationManager::class.java))) return
    val needed = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0).build()
    val request = LocationSettingsRequest.Builder().addLocationRequest(needed).build()
    LocationServices.getSettingsClient(this).checkLocationSettings(request).addOnFailureListener { off ->
        if (off is ResolvableApiException) show(IntentSenderRequest.Builder(off.resolution).build())
    }
}
