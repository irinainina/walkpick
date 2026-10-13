package com.walkpick.app.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkpick.app.BuildConfig
import com.walkpick.app.data.account.AccountExit
import com.walkpick.app.data.account.AuthRefused
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.profile.ProfileRepository
import com.walkpick.app.data.settings.AppSettings
import com.walkpick.app.data.settings.Reminders
import com.walkpick.app.data.settings.SettingsStore
import com.walkpick.app.data.steps.DEFAULT_DAILY_GOAL
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.data.steps.StepsRepository
import com.walkpick.app.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SettingsViewModel"
private const val KEEP_AFTER_LEAVING_MS = 5_000L

data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val steps: StepsAccess = StepsAccess.COUNTING,
    /** The steps of a full day: the ring of Today and the bars of Steps are measured by it. */
    val dailySteps: Int = DEFAULT_DAILY_GOAL,
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
    private val profiles: ProfileRepository,
    private val steps: StepsRepository,
    private val reminders: Reminders,
    private val sync: SyncScheduler,
    private val exit: AccountExit,
) : ViewModel() {

    private val access = MutableStateFlow(steps.access())
    private val busy = MutableStateFlow(false)
    private val deleteFailed = MutableStateFlow(false)

    val state: StateFlow<SettingsState> =
        combine(store.value, profiles.profile, access, busy, deleteFailed) { chosen, me, access, busy, bad ->
            val glucose = HealthKind.GLUCOSE in me.healthTabs
            SettingsState(chosen, access, me.dailyGoal, glucose, busy = busy, deleteFailed = bad)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), SettingsState())

    /** The permission may have been given or taken away while the screen was out of sight. */
    fun lookAgain() {
        access.value = steps.access()
    }

    fun setDailySteps(steps: Int) {
        viewModelScope.launch { profiles.change { it.copy(dailyGoal = steps) } }
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
