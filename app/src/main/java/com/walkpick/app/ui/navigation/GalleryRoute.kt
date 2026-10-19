package com.walkpick.app.ui.navigation

import android.Manifest
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkpick.app.ui.gallery.GalleryViewModel
import com.walkpick.app.ui.map.PhotoMapViewModel
import com.walkpick.app.ui.screens.GalleryActions
import com.walkpick.app.ui.screens.GalleryScreen
import com.walkpick.app.ui.screens.PhotoMap
import com.walkpick.app.ui.screens.PhotoMapActions

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
        val map: PhotoMapViewModel = hiltViewModel()
        val mapState by map.state.collectAsStateWithLifecycle()
        // The location is asked for by the button that needs it, not on opening the map.
        val locate = rememberPermissionRequest(Manifest.permission.ACCESS_FINE_LOCATION) { granted ->
            if (granted) map.locate()
        }
        val mapActions = remember(map, locate) {
            PhotoMapActions(
                onFilter = map::filter,
                onPick = map::pick,
                onOpen = { navController.navigate(findCardRoute(it.id)) },
                onLocate = { if (map.canLocate) map.locate() else locate() },
            )
        }
        GalleryScreen(shown, actions, onWalk = { navController.navigate(walkSummaryRoute(it)) }) { top ->
            PhotoMap(mapState, mapActions, top)
        }
    }
}
