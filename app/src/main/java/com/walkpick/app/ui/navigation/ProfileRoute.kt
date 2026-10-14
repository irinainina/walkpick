package com.walkpick.app.ui.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkpick.app.ui.profile.ProfileViewModel
import com.walkpick.app.ui.screens.ProfileScreen

internal fun NavGraphBuilder.profileRoute(navController: NavHostController) {
    composable(Screen.Profile.route) {
        val model: ProfileViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        // Nothing is drawn for the instant before the profile is read.
        state?.let {
            ProfileScreen(
                state = it,
                onChange = model::change,
                onWeight = model::addWeight,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
