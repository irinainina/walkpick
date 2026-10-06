package com.walkspot.app.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.BuildConfig
import com.walkspot.app.data.account.AccountExit
import com.walkspot.app.data.account.AuthRefused
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.profile.ProfileStore
import com.walkspot.app.data.settings.AppSettings
import com.walkspot.app.data.settings.Reminders
import com.walkspot.app.data.settings.SettingsStore
import com.walkspot.app.data.steps.StepsAccess
import com.walkspot.app.data.steps.StepsRepository
import com.walkspot.app.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SettingsViewModel"
private const val KEEP_AFTER_LEAVING_MS = 5_000L

data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val steps: StepsAccess = StepsAccess.COUNTING,
    /** The glucose unit is offered only while the Glucose tab is on. */
    val glucoseShown: Boolean = false,
    val version: String = BuildConfig.VERSION_NAME,
    /** A sign-out or a deletion is on its way to the server. */
    val busy: Boolean = false,
    val deleteFailed: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    profiles: ProfileStore,
    private val steps: StepsRepository,
    private val reminders: Reminders,
    private val sync: SyncScheduler,
    private val exit: AccountExit,
) : ViewModel() {

    private val access = MutableStateFlow(steps.access())
    private val busy = MutableStateFlow(false)
    private val deleteFailed = MutableStateFlow(false)
    private val glucoseShown = profiles.value.map { HealthKind.GLUCOSE in it.healthTabs }

    val state: StateFlow<SettingsState> =
        combine(store.value, glucoseShown, access, busy, deleteFailed) { chosen, glucose, access, busy, bad ->
            SettingsState(chosen, access, glucose, busy = busy, deleteFailed = bad)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), SettingsState())

    /** The permission may have been given or taken away while the screen was out of sight. */
    fun lookAgain() {
        access.value = steps.access()
    }

    fun change(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            store.change(transform)
            reminders.schedule(store.value.first())
            sync.pushSoon()
        }
    }

    fun signOut(onDone: () -> Unit) = leave(onDone) { exit.signOut() }

    fun deleteAccount(onDone: () -> Unit) = leave(onDone) {
        try {
            exit.deleteAccount()
            exit.signOut()
        } catch (refused: AuthRefused) {
            Log.w(TAG, "the account was not deleted", refused)
            deleteFailed.value = true
        }
    }

    // One goodbye at a time; [onDone] is skipped when the deletion was refused.
    private fun leave(onDone: () -> Unit, goodbye: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        deleteFailed.value = false
        viewModelScope.launch {
            goodbye()
            busy.value = false
            if (!deleteFailed.value) onDone()
        }
    }
}
