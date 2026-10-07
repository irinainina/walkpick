package com.walkspot.app

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
import com.walkspot.app.data.account.AccountStore
import com.walkspot.app.ui.navigation.WalkSpotApp
import com.walkspot.app.ui.theme.WalkSpotTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var account: AccountStore

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
            WalkSpotTheme {
                // Nothing is drawn until the phone knows whether somebody is signed in.
                val signedIn by produceState<Boolean?>(null) { value = account.isSignedIn() }
                if (signedIn != null && barsKnown) {
                    WalkSpotApp(signedIn == true, walkAsked, onStartShown = { firstScreenReady = true })
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
