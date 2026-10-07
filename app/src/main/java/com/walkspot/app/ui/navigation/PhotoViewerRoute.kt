package com.walkspot.app.ui.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkspot.app.ui.find.PhotoViewerViewModel
import com.walkspot.app.ui.screens.PhotoViewerScreen

/** The address of the viewer opened on the photo of one find. */
internal fun photoViewerRoute(id: String) = "${Screen.PhotoViewer.route}/$id"

internal fun NavGraphBuilder.photoViewerRoute(navController: NavHostController) {
    composable("${Screen.PhotoViewer.route}/{${PhotoViewerViewModel.FIND_ID}}") {
        val model: PhotoViewerViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        state?.let {
            PhotoViewerScreen(
                state = it,
                photo = model::photo,
                onBack = { navController.popBackStack() },
                onBest = model::toggleBest,
                onFind = { find -> navController.navigate(findCardRoute(find)) },
            )
        }
    }
}
