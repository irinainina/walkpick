package com.walkpick.app.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkpick.app.data.health.HealthRepository
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.profile.ProfileRepository
import com.walkpick.app.data.settings.AppSettings
import com.walkpick.app.data.settings.SettingsStore
import com.walkpick.app.data.steps.StepsRepository
import com.walkpick.app.data.walk.WalkRepository
import com.walkpick.app.ui.stats.StatsTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L

data class HealthState(
    /** The tabs turned on in Profile, in their fixed order. */
    val tabs: List<HealthKind>,
    val tab: HealthKind,
    val period: StatsTab,
    val today: LocalDate = LocalDate.now(),
    /** Null until the tab has its first entry. */
    val current: HealthCurrent? = null,
    val chart: HealthChart? = null,
    val insight: HealthInsight? = null,
    val units: AppSettings = AppSettings(),
)

/** What the user chose; no tab means the first one that is turned on. */
data class HealthChoice(
    val tab: HealthKind? = null,
    val period: StatsTab = StatsTab.Month,
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    profiles: ProfileRepository,
    private val health: HealthRepository,
    steps: StepsRepository,
    walks: WalkRepository,
    settings: SettingsStore,
) : ViewModel() {

    private val choice = MutableStateFlow(HealthChoice())

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HealthState?> =
        profiles.profile
            .combine(choice) { profile, made -> profile to healthState(profile.healthTabs, made) }
            .flatMapLatest { (profile, chosen) ->
                val entries = health.watch(chosen.tab)
                val sources = combine(entries, steps.watchDays(), walks.watchAll(), ::HealthData)
                combine(sources, settings.value) { data, units ->
                    val today = LocalDate.now()
                    val zone = ZoneId.systemDefault()
                    val (tab, period) = chosen.tab to chosen.period
                    val targets = profile.healthTargets(tab)
                    val target = targets.firstOrNull()
                    chosen.copy(
                        today = today,
                        current = healthCurrent(data.entries, period, today, target, zone),
                        chart = healthChart(tab, data, period, today, targets, zone),
                        // Only the mood has a remark: a count of the walker's own walks, never a cause.
                        insight = moodInsight(data.walks, period, today, zone)
                            .takeIf { tab == HealthKind.MOOD },
                        units = units,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)

    fun add(kind: HealthKind, value: Double, diastolic: Int?, measuredAt: Instant) {
        viewModelScope.launch { health.add(kind, value, diastolic, measuredAt) }
    }

    fun delete(id: String) {
        viewModelScope.launch { health.delete(id) }
    }

    fun open(tab: HealthKind) = choice.update { it.copy(tab = tab) }

    fun show(period: StatsTab) = choice.update { it.copy(period = period) }
}

internal fun healthState(turnedOn: Set<HealthKind>, choice: HealthChoice): HealthState {
    val tabs = HealthKind.entries.filter { it in turnedOn }
    // A tab turned off in Profile while it was open gives way to the first one left.
    return HealthState(tabs, choice.tab?.takeIf { it in tabs } ?: tabs.first(), choice.period)
}
