package com.walkpick.app.ui.navigation

import androidx.annotation.StringRes
import com.walkpick.app.R
import com.walkpick.app.ui.components.NavItem

enum class Screen(@StringRes val titleRes: Int) {
    SignIn(R.string.screen_sign_in),
    Today(R.string.tab_today),
    Health(R.string.screen_health),
    ActiveWalk(R.string.screen_active_walk),
    WalkSummary(R.string.screen_walk_summary),
    Camera(R.string.screen_camera),
    FindCard(R.string.screen_find_card),
    Gallery(R.string.tab_gallery),
    PhotoViewer(R.string.screen_photo_viewer),
    Profile(R.string.tab_profile),
    Settings(R.string.screen_settings),
    ;

    val route: String get() = name

    /** The tab lit in the bottom bar; null on a screen inside a tab, which has Back and no bar. */
    val tab: Screen?
        get() = when (this) {
            Today, Gallery, Health, Settings -> this
            else -> null
        }

    /** Where the screen leads, until the real screens replace the stubs. */
    val links: List<Screen>
        get() = when (this) {
            SignIn -> listOf(Today)
            Today -> listOf(Profile, ActiveWalk)
            ActiveWalk -> listOf(Camera, WalkSummary)
            Camera, PhotoViewer -> listOf(FindCard)
            Gallery -> listOf(PhotoViewer)
            Settings -> listOf(SignIn)
            else -> emptyList()
        }
}

val bottomTabs = listOf(
    NavItem(R.string.tab_walk, R.drawable.ic_walk, Screen.Today.route),
    NavItem(R.string.tab_photo, R.drawable.ic_gallery, Screen.Gallery.route),
    NavItem(R.string.screen_health, R.drawable.ic_health, Screen.Health.route),
    NavItem(R.string.screen_settings, R.drawable.ic_settings, Screen.Settings.route),
)
