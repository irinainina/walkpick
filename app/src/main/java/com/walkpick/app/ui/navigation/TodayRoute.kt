package com.walkpick.app.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkpick.app.ui.screens.PeriodActions
import com.walkpick.app.ui.screens.TodayActions
import com.walkpick.app.ui.screens.TodayScreen
import com.walkpick.app.ui.screens.WalkPeriods
import com.walkpick.app.ui.stats.StatsViewModel
import com.walkpick.app.ui.today.LastWalkViewModel
import com.walkpick.app.ui.today.TodayViewModel
import com.walkpick.app.ui.today.WeatherViewModel
import com.walkpick.app.ui.walk.ActiveWalkViewModel

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
        val periods: StatsViewModel = hiltViewModel()
        val periodState by periods.state.collectAsStateWithLifecycle()
        val shown = state ?: return@composable
        SideEffect(onShown)
        TodayScreen(
            state = shown,
            walkSeconds = walking.seconds.takeIf { walking.recording },
            walkPaused = walking.paused,
            walkEnding = walking.ending,
            weather = weather,
            lastWalk = lastWalk,
            actions = TodayActions(
                onAllowSteps = allowSteps,
                onWalk = { navController.navigate(walkSummaryRoute(it)) },
                onProfile = { navController.navigate(Screen.Profile.route) },
                photo = last::photo,
            ),
            onStartWalk = { navController.navigate(Screen.ActiveWalk.route) },
            onStopWalk = { mood ->
                walk.stop(mood) { saved -> saved?.let { navController.navigate(walkSummaryRoute(it)) } }
            },
            periods = {
                periodState?.let {
                    WalkPeriods(
                        state = it,
                        actions = PeriodActions(
                            onTab = periods::open,
                            onMove = periods::move,
                            onPickBar = periods::pick,
                            onPickDay = { day ->
                                periods.pickDay(day) { id -> navController.navigate(walkSummaryRoute(id)) }
                            },
                            onWalk = { id ->
                                periods.closeDay()
                                navController.navigate(walkSummaryRoute(id))
                            },
                            onCloseDay = periods::closeDay,
                            photo = periods::photo,
                        ),
                    )
                }
            },
        )
    }
}
