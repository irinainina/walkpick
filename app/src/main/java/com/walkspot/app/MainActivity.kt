package com.walkspot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
        setContent {
            WalkSpotTheme {
                // Nothing is drawn until the phone knows whether somebody is signed in.
                val signedIn by produceState<Boolean?>(null) { value = account.isSignedIn() }
                signedIn?.let { WalkSpotApp(it) }
            }
        }
    }
}
