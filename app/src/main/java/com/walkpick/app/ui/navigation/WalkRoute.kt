package com.walkpick.app.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkpick.app.ui.screens.PeriodActions
import com.walkpick.app.ui.screens.WalkScreen
import com.walkpick.app.ui.stats.StatsViewModel
import java.time.LocalDate

/** Left by Home on its own entry: the day whose walks the Walk tab offers as it opens. */
internal const val WEEK_DAY = "week_day"

internal fun NavGraphBuilder.walkRoute(navController: NavHostController) {
    composable(Screen.Walk.route) {
        val periods: StatsViewModel = hiltViewModel()
        val state by periods.state.collectAsStateWithLifecycle()
        val shown = state ?: return@composable
        val openWalk = { id: String -> navController.navigate(walkSummaryRoute(id)) }
        LaunchedEffect(Unit) {
            navController.previousBackStackEntry?.savedStateHandle?.remove<String>(WEEK_DAY)
                ?.let { periods.openDay(LocalDate.parse(it)) }
        }
        WalkScreen(
            state = shown,
            actions = PeriodActions(
                onTab = periods::open,
                onMove = periods::move,
                onPickBar = { bar -> periods.pick(bar, openWalk) },
                onPickDay = { day -> periods.pickDay(day, openWalk) },
                onWalk = { id ->
                    periods.closeDay()
                    openWalk(id)
                },
                onCloseDay = periods::closeDay,
                photo = periods::photo,
            ),
            onPhotos = { navController.openTab(Screen.Gallery.route) },
        )
    }
}
