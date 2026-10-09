package com.walkspot.app.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.ui.health.HealthViewModel
import com.walkspot.app.ui.screens.NewEntrySheet
import com.walkspot.app.ui.screens.TodayActions
import com.walkspot.app.ui.screens.TodayScreen
import com.walkspot.app.ui.today.LastWalkViewModel
import com.walkspot.app.ui.today.TodayViewModel
import com.walkspot.app.ui.today.WeatherViewModel
import com.walkspot.app.ui.walk.ActiveWalkViewModel

internal fun NavGraphBuilder.todayRoute(navController: NavHostController, onShown: () -> Unit) {
    composable(Screen.Today.route) {
        val model: TodayViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        val sky: WeatherViewModel = hiltViewModel()
        val weather by sky.now.collectAsStateWithLifecycle()
        val last: LastWalkViewModel = hiltViewModel()
        val lastWalk by last.walk.collectAsStateWithLifecycle()
        val owner = LocalLifecycleOwner.current
        LaunchedEffect(owner) {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sky.refresh()
                model.keepFresh()
            }
        }
        // The screen notices the answer by itself: it looks at the permission every few seconds.
        val allowSteps = rememberPermissionRequest(STEPS_PERMISSION) {}
        // The walk going on is known to the whole app; this screen only needs to know there is one.
        val walk: ActiveWalkViewModel = hiltViewModel()
        val walking by walk.state.collectAsStateWithLifecycle()
        // A Health tile that asks for an entry opens the sheet of Health right here.
        val health: HealthViewModel = hiltViewModel()
        val healthState by health.state.collectAsStateWithLifecycle()
        var adding by rememberSaveable { mutableStateOf<HealthKind?>(null) }
        val shown = state ?: return@composable
        SideEffect(onShown)
        TodayScreen(
            state = shown,
            walkSeconds = walking.seconds.takeIf { walking.recording },
            walkKept = walking.kept,
            weather = weather,
            lastWalk = lastWalk,
            actions = TodayActions(
                onAllowSteps = allowSteps,
                onStats = { navController.navigate(Screen.Stats.route) },
                onHealth = { navController.navigate(Screen.Health.route) },
                onAddHealth = { adding = it },
                onWalk = { navController.navigate(walkSummaryRoute(it)) },
                onReminders = { navController.navigate(Screen.Settings.route) },
            ),
            onStartWalk = { navController.navigate(Screen.ActiveWalk.route) },
            onStopWalk = {
                walk.stop { saved -> saved?.let { navController.navigate(walkSummaryRoute(it)) } }
            },
        )
        val entry = adding?.let { kind -> healthState?.copy(tab = kind) }
        if (entry != null) NewEntrySheet(entry, health::add, onDismiss = { adding = null })
    }
}
