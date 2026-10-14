package com.walkpick.app

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import com.walkpick.app.data.account.AccountStore
import com.walkpick.app.data.settings.SettingsStore
import com.walkpick.app.data.walk.Season
import com.walkpick.app.data.walk.SeasonStore
import com.walkpick.app.ui.navigation.WalkPickApp
import com.walkpick.app.ui.theme.WalkPickTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var account: AccountStore

    @Inject
    lateinit var seasons: SeasonStore

    @Inject
    lateinit var settings: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Android reports its bars after the first layout: drawn before that, the screen jumps.
        var barsKnown by mutableStateOf(false)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            barsKnown = true
            ViewCompat.onApplyWindowInsets(view, insets)
        }
        var firstScreenReady = false
        holdSplashUntil { firstScreenReady }
        // The notification of a walk opens the walk; counted, so that a second tap opens it again.
        var walkAsked by mutableIntStateOf(if (intent.asksForWalk()) 1 else 0)
        addOnNewIntentListener { if (it.asksForWalk()) walkAsked++ }
        setContent {
            // Nothing is drawn until the phone knows the season and whether somebody is signed in.
            val season by produceState<Season?>(null) {
                combine(seasons.now, settings.value) { seen, chosen -> chosen.theme ?: seen }
                    .collect { value = it }
            }
            val signedIn by produceState<Boolean?>(null) { value = account.isSignedIn() }
            season?.let {
                WalkPickTheme(it) {
                    if (signedIn != null && barsKnown) {
                        WalkPickApp(signedIn == true, walkAsked, onStartShown = { firstScreenReady = true })
                    }
                }
            }
        }
    }

    private fun Intent.asksForWalk() = getBooleanExtra(EXTRA_OPEN_WALK, false)

    // Android keeps its splash screen (or the picture of the app it remembers) until the first frame is
    // drawn; frames are refused until the first screen has its data, so no empty screen flashes in between.
    private fun holdSplashUntil(ready: () -> Boolean) {
        val content = findViewById<View>(android.R.id.content)
        val giveUpAt = SystemClock.uptimeMillis() + SPLASH_LIMIT_MS
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val draw = ready() || SystemClock.uptimeMillis() > giveUpAt
                    if (draw) content.viewTreeObserver.removeOnPreDrawListener(this) else content.invalidate()
                    return draw
                }
            },
        )
    }

    companion object {
        /** Set on the intent of the walk notification. */
        const val EXTRA_OPEN_WALK = "open_walk"

        // A screen that never reports itself must not leave the app behind its splash for good.
        private const val SPLASH_LIMIT_MS = 3_000L
    }
}
