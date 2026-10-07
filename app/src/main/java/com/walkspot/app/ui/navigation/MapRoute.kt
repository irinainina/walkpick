package com.walkspot.app.ui.navigation

import android.Manifest
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkspot.app.ui.map.MapViewModel
import com.walkspot.app.ui.screens.MapScreen

internal fun NavGraphBuilder.mapRoute(navController: NavHostController) {
    composable(Screen.Map.route) {
        val model: MapViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        // The location is asked for by the button that needs it, not on opening the map.
        val ask = rememberPermissionRequest(Manifest.permission.ACCESS_FINE_LOCATION) { granted ->
            if (granted) model.locate()
        }
        MapScreen(
            state = state,
            onTab = model::open,
            onLook = model::lookAround,
            onPlace = { navController.navigate(placeCardRoute(it)) },
            onLocate = { if (model.canLocate) model.locate() else ask() },
            onWalk = { navController.navigate(walkSummaryRoute(it)) },
        )
    }
}
