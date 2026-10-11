package com.walkspot.app.ui.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkspot.app.ui.health.HealthViewModel
import com.walkspot.app.ui.screens.HealthScreen

internal fun NavGraphBuilder.healthRoute(navController: NavHostController) {
    composable(Screen.Health.route) {
        val model: HealthViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        // Nothing is drawn for the instant before the profile is read.
        state?.let {
            HealthScreen(
                state = it,
                onBack = { navController.popBackStack() },
                onSave = model::add,
                onDelete = model::delete,
                onTab = model::open,
                onPeriod = model::show,
            )
        }
    }
}
