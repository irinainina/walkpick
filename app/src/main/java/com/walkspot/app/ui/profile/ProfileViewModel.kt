package com.walkspot.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.account.AccountStore
import com.walkspot.app.data.health.HealthRepository
import com.walkspot.app.data.local.FindDao
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.local.WalkDao
import com.walkspot.app.data.profile.Profile
import com.walkspot.app.data.profile.ProfileRepository
import com.walkspot.app.data.settings.AppSettings
import com.walkspot.app.data.settings.SettingsStore
import com.walkspot.app.data.steps.StepsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L

/** Steps, walks and photos since the first day. */
data class Totals(val steps: Int = 0, val walks: Int = 0, val photos: Int = 0)

data class ProfileState(
    val today: LocalDate,
    val name: String? = null,
    val email: String = "",
    val profile: Profile = Profile(),
    val totals: Totals = Totals(),
    /** The latest weight saved on Health. */
    val weightKg: Double? = null,
    val units: AppSettings = AppSettings(),
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    steps: StepsRepository,
    walks: WalkDao,
    finds: FindDao,
    private val health: HealthRepository,
    settings: SettingsStore,
    account: AccountStore,
) : ViewModel() {

    private val totals =
        combine(steps.watchDays(), walks.watchStarts(), finds.watchCount()) { days, starts, photos ->
            Totals(days.sumOf { it.steps }, starts.size, photos)
        }

    private val weight = health.watchLatest(HealthKind.WEIGHT)
    private val units = settings.value

    val state: StateFlow<ProfileState?> =
        combine(profiles.profile, totals, weight, account.user, units) { profile, totals, kg, who, units ->
            ProfileState(LocalDate.now(), who?.name, who?.email.orEmpty(), profile, totals, kg, units)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)

    fun addWeight(kg: Double, at: Instant) {
        viewModelScope.launch { health.add(HealthKind.WEIGHT, kg, diastolic = null, measuredAt = at) }
    }

    fun change(transform: (Profile) -> Profile) {
        viewModelScope.launch { profiles.change(transform) }
    }
}
