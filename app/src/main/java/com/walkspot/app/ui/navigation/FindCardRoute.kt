package com.walkspot.app.ui.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkspot.app.ui.find.FindCardViewModel
import com.walkspot.app.ui.screens.FindCardScreen

/** The address of the card of one find. */
internal fun findCardRoute(id: String) = "${Screen.FindCard.route}/$id"

internal fun NavGraphBuilder.findCardRoute(navController: NavHostController) {
    composable("${Screen.FindCard.route}/{${FindCardViewModel.FIND_ID}}") {
        val model: FindCardViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        // Nothing is drawn before the find is read and after it is deleted.
        state?.let {
            FindCardScreen(
                state = it,
                onBack = { navController.popBackStack() },
                onBest = model::toggleBest,
                onDelete = { model.delete(onDone = { navController.popBackStack() }) },
                onWalk = { walk -> navController.navigate(walkSummaryRoute(walk)) },
                onChoose = model::choose,
                onRename = model::rename,
                onRetry = model::retry,
            )
        }
    }
}
