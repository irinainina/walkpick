package com.walkspot.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.walkspot.app.ui.components.AppNavBar
import com.walkspot.app.ui.screens.PlaceholderScreen
import com.walkspot.app.ui.theme.AppTheme

@Composable
fun WalkSpotApp() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val tab = Screen.entries.firstOrNull { it.route == entry?.destination?.route }?.tab

    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas),
    ) {
        NavHost(navController, startDestination = Screen.Today.route, modifier = Modifier.weight(1f)) {
            Screen.entries.forEach { screen ->
                composable(screen.route) {
                    PlaceholderScreen(
                        title = stringResource(screen.titleRes),
                        links = screen.links.map { stringResource(it.titleRes) },
                        onOpen = { navController.navigate(screen.links[it].route) },
                    )
                }
            }
        }
        if (tab != null) {
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
}
