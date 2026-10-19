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
import com.walkpick.app.data.steps.weekOf
import com.walkpick.app.ui.screens.TodayActions
import com.walkpick.app.ui.screens.TodayScreen
import com.walkpick.app.ui.today.TodayViewModel
import com.walkpick.app.ui.today.WeatherViewModel
import com.walkpick.app.ui.walk.ActiveWalkViewModel

internal fun NavGraphBuilder.homeRoute(navController: NavHostController, onShown: () -> Unit) {
    composable(Screen.Home.route) { entry ->
        val model: TodayViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        val sky: WeatherViewModel = hiltViewModel()
        val weather by sky.now.collectAsStateWithLifecycle()
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
        val shown = state ?: return@composable
        SideEffect(onShown)
        // A fresh Walk tab: it opens on this week, whatever period it was left at.
        val openWeek = {
            navController.navigate(Screen.Walk.route) {
                popUpTo(Screen.Home.route)
                launchSingleTop = true
            }
        }
        TodayScreen(
            state = shown,
            walkSeconds = walking.seconds.takeIf { walking.recording },
            walkPaused = walking.paused,
            walkEnding = walking.ending,
            walkMoods = walking.moods,
            weather = weather,
            actions = TodayActions(
                onAllowSteps = allowSteps,
                onProfile = { navController.navigate(Screen.Profile.route) },
                onWeek = openWeek,
                // The only walk of the day opens at once, several are offered on the Walk tab.
                onDay = { index ->
                    val ids = shown.weekWalks[index]
                    if (ids.size == 1) {
                        navController.navigate(walkSummaryRoute(ids.first()))
                    } else {
                        if (ids.isNotEmpty()) {
                            entry.savedStateHandle[WEEK_DAY] = weekOf(shown.now.toLocalDate()).from
                                .plusDays(index.toLong()).toString()
                        }
                        openWeek()
                    }
                },
            ),
            onStartWalk = { navController.navigate(activeWalkRoute(it)) },
            onStopWalk = { mood ->
                walk.stop(mood) { saved -> saved?.let { navController.navigate(walkSummaryRoute(it)) } }
            },
        )
    }
}
