package com.walkpick.app.ui.navigation

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkpick.app.ui.profile.ProfileViewModel
import com.walkpick.app.ui.screens.ProfileScreen
import com.walkpick.app.ui.screens.SettingsActions
import com.walkpick.app.ui.settings.SettingsViewModel
import java.time.LocalDate

private const val JSON = "application/json"

internal fun NavGraphBuilder.profileRoute(navController: NavHostController) {
    composable(Screen.Profile.route) {
        val model: ProfileViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        val settings: SettingsViewModel = hiltViewModel()
        val settingsState by settings.state.collectAsStateWithLifecycle()
        LifecycleResumeEffect(settings) {
            settings.lookAgain()
            onPauseOrDispose {}
        }
        val allowSteps = rememberPermissionRequest(STEPS_PERMISSION) { settings.lookAgain() }
        val allowReminders = rememberPermissionRequest(NOTIFICATIONS_PERMISSION) { granted ->
            if (granted) settings.change { it.copy(remindersOn = true) }
        }
        // Whoever leaves the account is taken to the sign-in with nothing to go back to.
        val leave = {
            navController.navigate(Screen.SignIn.route) { popUpTo(0) }
        }
        val export = rememberLauncherForActivityResult(CreateDocument(JSON)) { target ->
            target?.let(settings::exportTo)
        }
        // Nothing is drawn for the instant before the profile is read.
        state?.let {
            ProfileScreen(
                state = it,
                settings = settingsState,
                actions = SettingsActions(
                    onChange = settings::change,
                    onDailySteps = settings::setDailySteps,
                    onReminders = { on ->
                        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            allowReminders()
                        } else {
                            settings.change { it.copy(remindersOn = on) }
                        }
                    },
                    onAllowSteps = allowSteps,
                    onSignOut = { settings.signOut(leave) },
                    onDelete = { settings.deleteAccount(leave) },
                    onExport = { export.launch("walkpick-${LocalDate.now()}.json") },
                ),
                onChange = model::change,
                onRename = model::rename,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
