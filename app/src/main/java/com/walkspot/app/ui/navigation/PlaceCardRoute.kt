package com.walkspot.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkspot.app.data.places.Place
import com.walkspot.app.ui.find.PlaceCardViewModel
import com.walkspot.app.ui.screens.PlaceCardScreen

private const val PLACE = "place"

/** The address of the card of a place from Wikipedia; the place travels in the address itself. */
internal fun placeCardRoute(place: Place) =
    "$PLACE/${Uri.encode(place.title)}/${place.latitude}/${place.longitude}"

internal fun NavGraphBuilder.placeCardRoute(navController: NavHostController) {
    val arguments = with(PlaceCardViewModel) { "{$TITLE}/{$LATITUDE}/{$LONGITUDE}" }
    composable("$PLACE/$arguments") {
        val model: PlaceCardViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        PlaceCardScreen(state, onBack = { navController.popBackStack() })
    }
}
