package com.walkpick.app.ui.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkpick.app.ui.screens.WalkSummaryScreen
import com.walkpick.app.ui.walk.WalkSummaryViewModel

/** The address of the summary of one walk. */
internal fun walkSummaryRoute(id: String) = "${Screen.WalkSummary.route}/$id"

internal fun NavGraphBuilder.walkSummaryRoute(navController: NavHostController) {
    composable("${Screen.WalkSummary.route}/{${WalkSummaryViewModel.WALK_ID}}") {
        val model: WalkSummaryViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        // Nothing is drawn for the instant before the walk is read, nor after it is deleted.
        state?.let {
            WalkSummaryScreen(
                state = it,
                onBack = { navController.popBackStack() },
                onNote = model::saveNote,
                onRename = model::rename,
                onDelete = { model.delete(onDone = { navController.popBackStack() }) },
                onFind = { find -> navController.navigate(findCardRoute(find)) },
            )
        }
    }
}
