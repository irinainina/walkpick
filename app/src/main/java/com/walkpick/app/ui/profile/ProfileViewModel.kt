package com.walkpick.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkpick.app.data.account.AccountStore
import com.walkpick.app.data.find.FindStore
import com.walkpick.app.data.profile.Profile
import com.walkpick.app.data.profile.ProfileRepository
import com.walkpick.app.data.settings.AppSettings
import com.walkpick.app.data.settings.SettingsStore
import com.walkpick.app.data.walk.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L

/** Steps, walks and photos since the first day. */
data class Totals(val walks: Int = 0, val metres: Long = 0, val photos: Int = 0)

data class ProfileState(
    val name: String? = null,
    val email: String = "",
    val profile: Profile = Profile(),
    val totals: Totals = Totals(),
    val units: AppSettings = AppSettings(),
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    walks: WalkRepository,
    finds: FindStore,
    settings: SettingsStore,
    account: AccountStore,
) : ViewModel() {

    private val totals =
        combine(walks.watchAll(), finds.watchCount()) { walked, photos ->
            Totals(walked.size, walked.sumOf { it.distanceM.toLong() }, photos)
        }

    val state: StateFlow<ProfileState?> =
        combine(profiles.profile, totals, account.user, settings.value) { profile, totals, who, units ->
            ProfileState(who?.name, who?.email.orEmpty(), profile, totals, units)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)

    fun change(transform: (Profile) -> Profile) {
        viewModelScope.launch { profiles.change(transform) }
    }
}
