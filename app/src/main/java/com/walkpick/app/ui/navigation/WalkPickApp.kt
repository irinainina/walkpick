package com.walkpick.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.walkpick.app.ui.account.AuthViewModel
import com.walkpick.app.ui.components.AppNavBar
import com.walkpick.app.ui.screens.PlaceholderScreen
import com.walkpick.app.ui.theme.AppTheme

/** [onStartShown] is called once the first screen has something to show. */
@Composable
fun WalkPickApp(signedIn: Boolean, walkAsked: Int, onStartShown: () -> Unit) {
    val navController = rememberNavController()
    // Above the NavHost: the three sign-in screens share it.
    val auth: AuthViewModel = hiltViewModel()
    val entry by navController.currentBackStackEntryAsState()
    val start = if (signedIn) Screen.Today else Screen.SignIn
    // Until the first destination reports itself the start screen stands in, or the bar pops in late.
    val route = entry?.destination?.route ?: start.route
    val tab = Screen.entries.firstOrNull { it.route == route }?.tab
    // While photos are picked in the gallery its two actions stand in place of the tabs.
    var selecting by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas),
    ) {
        NavHost(navController, startDestination = start.route, modifier = Modifier.weight(1f)) {
            signInRoutes(navController, auth, onStartShown)
            todayRoute(navController, onStartShown)
            statsRoute(navController)
            healthRoute()
            activeWalkRoute(navController)
            walkSummaryRoute(navController)
            cameraRoute(navController)
            findCardRoute(navController)
            placeCardRoute(navController)
            photoViewerRoute(navController)
            galleryRoute(navController, onSelecting = { selecting = it })
            mapRoute(navController)
            profileRoute(navController)
            settingsRoute(navController)
            val laidOut = setOf(
                Screen.SignIn,
                Screen.Today,
                Screen.Stats,
                Screen.Health,
                Screen.ActiveWalk,
                Screen.Camera,
                Screen.FindCard,
                Screen.Gallery,
                Screen.PhotoViewer,
                Screen.Map,
                Screen.Profile,
                Screen.Settings,
            )
            (Screen.entries - laidOut).forEach { screen ->
                composable(screen.route) {
                    PlaceholderScreen(
                        title = stringResource(screen.titleRes),
                        links = screen.links.map { stringResource(it.titleRes) },
                        onOpen = { navController.navigate(screen.links[it].route) },
                    )
                }
            }
        }
        if (tab != null && !selecting) {
            Box(
                Modifier
                    .background(AppTheme.colors.surface)
                    .navigationBarsPadding(),
            ) {
                AppNavBar(bottomTabs, selectedRoute = tab.route, onSelect = { item ->
                    navController.navigate(item.route) {
                        popUpTo(Screen.Today.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
        }
    }
    // [walkAsked] grows with every tap on the notification of a walk; placed after the NavHost, which
    // must have its graph before anything can be opened.
    LaunchedEffect(walkAsked) {
        if (signedIn && walkAsked > 0) {
            navController.navigate(Screen.ActiveWalk.route) { launchSingleTop = true }
        }
    }
}
