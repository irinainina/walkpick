package com.walkspot.app.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkspot.app.ui.gallery.GalleryViewModel
import com.walkspot.app.ui.screens.GalleryActions
import com.walkspot.app.ui.screens.GalleryScreen

/** [onSelecting] tells the app when photos are being picked: the tabs give way to the two actions. */
internal fun NavGraphBuilder.galleryRoute(navController: NavHostController, onSelecting: (Boolean) -> Unit) {
    composable(Screen.Gallery.route) {
        val model: GalleryViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        val shown = state ?: return@composable
        LaunchedEffect(shown.selected != null) { onSelecting(shown.selected != null) }
        val actions = remember(model) {
            GalleryActions(
                photo = model::photo,
                onTab = model::open,
                onOpen = { navController.navigate(photoViewerRoute(it.id)) },
                onSelecting = model::selecting,
                onToggle = model::toggle,
                onAddToBest = model::addSelectedToBest,
                onDelete = model::deleteSelected,
            )
        }
        GalleryScreen(shown, actions)
    }
}
