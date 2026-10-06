package com.walkspot.app.ui.navigation

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkspot.app.ui.screens.SettingsActions
import com.walkspot.app.ui.screens.SettingsScreen
import com.walkspot.app.ui.settings.SettingsViewModel

internal fun NavGraphBuilder.settingsRoute(navController: NavHostController) {
    composable(Screen.Settings.route) {
        val model: SettingsViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        LifecycleResumeEffect(model) {
            model.lookAgain()
            onPauseOrDispose {}
        }
        val allowSteps = rememberPermissionRequest(STEPS_PERMISSION) { model.lookAgain() }
        val allowReminders = rememberPermissionRequest(NOTIFICATIONS_PERMISSION) { granted ->
            if (granted) model.change { it.copy(remindersOn = true) }
        }
        // Whoever leaves the account is taken to the sign-in with nothing to go back to.
        val leave = {
            navController.navigate(Screen.SignIn.route) { popUpTo(0) }
        }
        SettingsScreen(
            state = state,
            actions = SettingsActions(
                onBack = { navController.popBackStack() },
                onChange = model::change,
                onReminders = { on ->
                    if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        allowReminders()
                    } else {
                        model.change { it.copy(remindersOn = on) }
                    }
                },
                onAllowSteps = allowSteps,
                onSignOut = { model.signOut(leave) },
                onDelete = { model.deleteAccount(leave) },
            ),
        )
    }
}
