package com.walkspot.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.account.AccountStore
import com.walkspot.app.data.account.AuthRefused
import com.walkspot.app.data.account.AuthRepository
import com.walkspot.app.data.health.HealthRepository
import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.profile.ProfileRepository
import com.walkspot.app.data.settings.AppSettings
import com.walkspot.app.data.settings.DistanceUnit
import com.walkspot.app.data.settings.SettingsStore
import com.walkspot.app.data.steps.DEFAULT_DAILY_GOAL
import com.walkspot.app.data.steps.StepsAccess
import com.walkspot.app.data.steps.StepsRepository
import com.walkspot.app.data.steps.currentStreak
import com.walkspot.app.data.steps.weekOf
import com.walkspot.app.data.walk.WalkRepository
import com.walkspot.app.ui.components.Bar
import com.walkspot.app.ui.components.BarKind
import com.walkspot.app.ui.health.HealthTile
import com.walkspot.app.ui.health.healthCurrent
import com.walkspot.app.ui.health.healthTargets
import com.walkspot.app.ui.stats.StatsTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

// How stale the clock and the answer about the sensor may get while the screen is open.
private const val REFRESH_MS = 10_000L
private const val KEEP_AFTER_LEAVING_MS = 5_000L

data class TodayState(
    val now: LocalDateTime,
    val name: String? = null,
    val access: StepsAccess = StepsAccess.COUNTING,
    val steps: Int = 0,
    /** The steps of a full day, as set in Settings. */
    val dailySteps: Int = DEFAULT_DAILY_GOAL,
    val distanceM: Int = 0,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val activeMinutes: Int = 0,
    val streak: Int = 0,
    /** Nobody has walked with the app yet: Today says what it is for. */
    val newcomer: Boolean = false,
    val week: List<Bar> = emptyList(),
    /** The tabs of Health turned on in Profile. */
    val health: List<HealthTile> = emptyList(),
    val units: AppSettings = AppSettings(),
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val steps: StepsRepository,
    profiles: ProfileRepository,
    settings: SettingsStore,
    walks: WalkRepository,
    health: HealthRepository,
    account: AccountStore,
    auth: AuthRepository,
) : ViewModel() {

    private val now = MutableStateFlow(LocalDateTime.now())
    private val access = MutableStateFlow(steps.access())

    private val walkDays = walks.watchStarts().map { starts ->
        starts.map { it.atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
    }

    private val who = combine(account.user, settings.value, profiles.profile) { user, settings, me ->
        Triple(user?.name, settings, me.dailyGoal)
    }

    // One tile for a tab turned on in Profile, with the change of this month, as Health opens.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val tiles = profiles.profile.flatMapLatest { profile ->
        val tabs = HealthKind.entries.filter { it in profile.healthTabs }
        combine(tabs.map { health.watch(it) }) { entries ->
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()
            tabs.mapIndexed { index, kind ->
                val target = profile.healthTargets(kind).firstOrNull()
                HealthTile(kind, healthCurrent(entries[index], StatsTab.Month, today, target, zone))
            }
        }
    }

    // Null until the days are read: zeros shown first would jump to the real numbers.
    val state: StateFlow<TodayState?> =
        combine(steps.watchDays(), walkDays, now, access, who) { days, walkDays, now, access, who ->
            val (name, units, dailySteps) = who
            val shown = todayState(days, walkDays, now, access, name, dailySteps)
            shown.copy(distanceUnit = units.distance, units = units)
        }
            .combine(tiles) { state, tiles -> state.copy(health = tiles) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)

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

internal fun todayState(
    days: List<DailyStepsEntity>,
    walkDays: Set<LocalDate>,
    now: LocalDateTime,
    access: StepsAccess,
    name: String?,
    dailySteps: Int,
): TodayState {
    val today = now.toLocalDate()
    val byDay = days.associateBy { it.day }
    val row = byDay[today]
    val monday = weekOf(today).from
    return TodayState(
        now = now,
        name = name,
        access = access,
        steps = row?.steps ?: 0,
        dailySteps = dailySteps,
        distanceM = row?.distanceM ?: 0,
        activeMinutes = row?.activeMinutes ?: 0,
        streak = currentStreak(walkDays, today),
        newcomer = walkDays.isEmpty(),
        week = DayOfWeek.entries.map { weekday ->
            val day = monday.plusDays(weekday.ordinal.toLong())
            val steps = byDay[day]
            Bar(
                fraction = (steps?.steps ?: 0).toFloat() / dailySteps,
                label = weekday.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                kind = if (day == today) BarKind.Today else BarKind.Past,
            )
        },
    )
}
