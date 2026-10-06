package com.walkspot.app.ui.account

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.account.AccountStore
import com.walkspot.app.data.account.AuthProblem
import com.walkspot.app.data.account.AuthRefused
import com.walkspot.app.data.account.AuthRepository
import com.walkspot.app.data.account.GoogleResult
import com.walkspot.app.data.account.GoogleSignIn
import com.walkspot.app.data.account.isEmailAddress
import com.walkspot.app.data.account.isOffline
import com.walkspot.app.data.local.WalkSpotDatabase
import com.walkspot.app.data.sync.SyncScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** The sign-in: one request at a time, and a reason when it does not go through. */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val account: AccountStore,
    private val database: WalkSpotDatabase,
    private val sync: SyncScheduler,
) : ViewModel() {

    var email by mutableStateOf("")
        private set
    var code by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var problem by mutableStateOf<AuthProblem?>(null)
        private set

    fun editEmail(value: String) {
        email = value
        if (problem == AuthProblem.InvalidEmail) problem = null
    }

    fun editCode(value: String) {
        code = value
        if (problem == AuthProblem.WrongCode) problem = null
    }

    // The two screens share this model: a complaint raised on one must not stand on the other.
    fun forgetProblem() {
        problem = null
    }

    fun sendCode(onSent: () -> Unit = {}) {
        val address = email.trim()
        if (!isEmailAddress(address)) {
            problem = AuthProblem.InvalidEmail
            return
        }
        code = ""
        request(refused = AuthProblem.InvalidEmail, onDone = onSent) { auth.sendCode(address) }
    }

    fun verifyCode(onSignedIn: () -> Unit) {
        request(refused = AuthProblem.WrongCode, onDone = onSignedIn) {
            auth.verifyCode(email.trim(), code)
            settle()
        }
    }

    fun signInWithGoogle(google: GoogleSignIn, onSignedIn: () -> Unit) {
        if (busy) return
        if (!google.isConfigured) {
            problem = AuthProblem.NoGoogle
            return
        }
        busy = true
        problem = null
        viewModelScope.launch {
            val result = google.requestToken()
            busy = false
            when (result) {
                GoogleResult.Dismissed -> Unit
                GoogleResult.Offline -> problem = AuthProblem.Offline
                is GoogleResult.Failed ->
                    problem = if (result.cause.isOffline()) AuthProblem.Offline else AuthProblem.NoGoogle
                is GoogleResult.Token -> request(refused = AuthProblem.NoGoogle, onDone = onSignedIn) {
                    auth.signInWithGoogle(result.idToken, result.nonce)
                    settle()
                }
            }
        }
    }

    // A different account than last time means the phone changed hands.
    private suspend fun settle() {
        val user = auth.currentUser() ?: return
        val previous = account.lastUserId()
        if (previous != null && previous != user.id) withContext(Dispatchers.IO) { database.clearAllTables() }
        account.signedIn(user.id)
        auth.rememberProfile()
        sync.restoreFromCloud()
    }

    // [refused] is what a refusal means here, when the server does not say.
    private fun request(refused: AuthProblem, onDone: () -> Unit, call: suspend () -> Unit) {
        if (busy) return
        busy = true
        problem = null
        viewModelScope.launch {
            try {
                problem = try {
                    call()
                    null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (refusal: AuthRefused) {
                    refusal.problem ?: refused
                }
                if (problem == null) onDone()
            } finally {
                busy = false
            }
        }
    }
}

@EntryPoint
@InstallIn(ActivityComponent::class)
internal interface GoogleSignInEntryPoint {
    fun googleSignIn(): GoogleSignIn
}

// A composable cannot be injected, so the activity's own graph is asked for the helper.
@Composable
internal fun rememberGoogleSignIn(): GoogleSignIn {
    val activity = checkNotNull(LocalActivity.current) { "the Google chooser needs an activity" }
    return remember(activity) {
        EntryPointAccessors.fromActivity(activity, GoogleSignInEntryPoint::class.java).googleSignIn()
    }
}
