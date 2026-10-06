package com.walkspot.app.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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

internal fun NavGraphBuilder.todayRoute(navController: NavHostController, onShown: () -> Unit) {
    composable(Screen.Today.route) {
        val model: TodayViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        val owner = LocalLifecycleOwner.current
        LaunchedEffect(owner) {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) { model.keepFresh() }
        }
        // The screen notices the answer by itself: it looks at the permission every few seconds.
        val allowSteps = rememberPermissionRequest(STEPS_PERMISSION) {}
        val shown = state ?: return@composable
        SideEffect(onShown)
        TodayScreen(
            state = shown,
            onAllowSteps = allowSteps,
            onStartWalk = { navController.navigate(Screen.ActiveWalk.route) },
            onStats = { navController.navigate(Screen.Stats.route) },
            onReminders = { navController.navigate(Screen.Settings.route) },
        )
    }
}
