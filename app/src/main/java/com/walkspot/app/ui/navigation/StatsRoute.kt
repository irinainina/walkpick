package com.walkspot.app.ui.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkspot.app.ui.screens.StatsScreen
import com.walkspot.app.ui.stats.StatsViewModel

internal fun NavGraphBuilder.statsRoute(navController: NavHostController) {
    composable(Screen.Stats.route) {
        val model: StatsViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        // Nothing is drawn for the instant before the days are read.
        state?.let {
            StatsScreen(
                state = it,
                onBack = { navController.popBackStack() },
                onTab = model::open,
                onMove = model::move,
                onPickBar = model::pick,
                onPickDay = { day -> model.pickDay(day) { navController.navigate(walkSummaryRoute(it)) } },
                onWalk = { walk ->
                    model.closeDay()
                    navController.navigate(walkSummaryRoute(walk))
                },
                onCloseDay = model::closeDay,
            )
        }
    }
}
