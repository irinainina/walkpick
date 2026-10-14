package com.walkpick.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkpick.app.data.account.AccountStore
import com.walkpick.app.data.account.AuthRefused
import com.walkpick.app.data.account.AuthRepository
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.data.settings.SettingsStore
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.data.steps.StepsRepository
import com.walkpick.app.data.walk.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

// How stale the clock and the answer about the sensor may get while the screen is open.
private const val REFRESH_MS = 10_000L
private const val KEEP_AFTER_LEAVING_MS = 5_000L

data class TodayState(
    val now: LocalDateTime,
    val name: String? = null,
    val access: StepsAccess = StepsAccess.COUNTING,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    /** Nobody has walked with the app yet: the tab says what it is for. */
    val newcomer: Boolean = false,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val steps: StepsRepository,
    settings: SettingsStore,
    walks: WalkRepository,
    account: AccountStore,
    auth: AuthRepository,
) : ViewModel() {

    private val now = MutableStateFlow(LocalDateTime.now())
    private val access = MutableStateFlow(steps.access())

    val state: StateFlow<TodayState?> =
        combine(walks.watchStarts(), now, access, account.user, settings.value) {
                starts, now, access, user, units ->
            TodayState(now, user?.name, access, units.distance, newcomer = starts.isEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)

    init {
        viewModelScope.launch {
            // The name is refreshed for the next opening; this one shows what the phone remembers.
            val user = try {
                auth.currentUser()
            } catch (_: AuthRefused) {
                null
            }
            user?.let { account.remember(it) }
        }
    }

    // Runs while the screen is in front; the caller cancels it.
    suspend fun keepFresh() {
        while (true) {
            access.value = steps.access()
            now.value = LocalDateTime.now()
            delay(REFRESH_MS)
        }
    }
}
