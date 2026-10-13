package com.walkpick.app.ui.navigation

import androidx.annotation.StringRes
import com.walkpick.app.R
import com.walkpick.app.ui.components.NavItem

enum class Screen(@StringRes val titleRes: Int) {
    SignIn(R.string.screen_sign_in),
    Today(R.string.tab_today),
    Stats(R.string.screen_stats),
    Health(R.string.screen_health),
    ActiveWalk(R.string.screen_active_walk),
    WalkSummary(R.string.screen_walk_summary),
    Map(R.string.tab_map),
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
            Today, Map, Gallery, Profile -> this
            else -> null
        }

    /** Where the screen leads, until the real screens replace the stubs. */
    val links: List<Screen>
        get() = when (this) {
            SignIn -> listOf(Today)
            Today -> listOf(Stats, Health, ActiveWalk)
            ActiveWalk -> listOf(Camera, WalkSummary)
            Camera, PhotoViewer -> listOf(FindCard)
            Map -> listOf(WalkSummary, FindCard)
            Gallery -> listOf(PhotoViewer)
            Profile -> listOf(Settings)
            Settings -> listOf(SignIn)
            else -> emptyList()
        }
}

val bottomTabs = listOf(
    NavItem(R.string.tab_today, R.drawable.ic_home, Screen.Today.route),
    NavItem(R.string.tab_map, R.drawable.ic_map, Screen.Map.route),
    NavItem(R.string.tab_gallery, R.drawable.ic_gallery, Screen.Gallery.route),
    NavItem(R.string.tab_profile, R.drawable.ic_profile, Screen.Profile.route),
)
