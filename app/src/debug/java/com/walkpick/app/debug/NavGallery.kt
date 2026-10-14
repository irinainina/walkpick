package com.walkpick.app.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walkpick.app.ui.components.AppNavBar
import com.walkpick.app.ui.navigation.bottomTabs
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing

/** The four states of the bottom bar, stacked as the variants of nav-bar-v2 in Figma. */
@Composable
fun NavGallery() {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.ml),
    ) {
        bottomTabs.forEach { AppNavBar(bottomTabs, selectedRoute = it.route, onSelect = {}) }
    }
}
