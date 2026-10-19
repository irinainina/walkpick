package com.walkpick.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkpick.app.data.account.AccountStore
import com.walkpick.app.data.account.AuthRefused
import com.walkpick.app.data.account.AuthRepository
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.profile.ProfileRepository
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.data.settings.SettingsStore
import com.walkpick.app.data.steps.DEFAULT_DAILY_GOAL
import com.walkpick.app.data.steps.StepsAccess
import com.walkpick.app.data.steps.StepsRepository
import com.walkpick.app.data.steps.currentStreak
import com.walkpick.app.data.steps.weekOf
import com.walkpick.app.data.walk.WalkRepository
import com.walkpick.app.ui.components.Bar
import com.walkpick.app.ui.localDay
import com.walkpick.app.ui.stats.dayBars
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
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
    /** The latest walk; null before the first one. */
    val lastWalk: WalkEntity? = null,
    /** The steps of a full day, as set in Profile. */
    val dailySteps: Int = DEFAULT_DAILY_GOAL,
    val streak: Int = 0,
    /** The steps walked on each day of this week. */
    val week: List<Bar> = emptyList(),
    /** The ids of the walks of each day of this week. */
    val weekWalks: List<List<String>> = emptyList(),
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val steps: StepsRepository,
    settings: SettingsStore,
    profiles: ProfileRepository,
    walks: WalkRepository,
    account: AccountStore,
    auth: AuthRepository,
) : ViewModel() {

    private val now = MutableStateFlow(LocalDateTime.now())
    private val access = MutableStateFlow(steps.access())

    private val who = combine(account.user, settings.value, profiles.profile) { user, units, profile ->
        Triple(user?.name, units.distance, profile.dailyGoal)
    }

    val state: StateFlow<TodayState?> =
        combine(walks.watchAll(), now, access, who) { walks, now, access, (name, unit, dailySteps) ->
            todayState(walks, now, access, name, dailySteps).copy(distanceUnit = unit)
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

internal fun todayState(
    walks: List<WalkEntity>,
    now: LocalDateTime,
    access: StepsAccess,
    name: String?,
    dailySteps: Int,
): TodayState {
    val today = now.toLocalDate()
    val byDay = walks.groupBy { it.startedAt.localDay() }
    val week = weekOf(today)
    return TodayState(
        now = now,
        name = name,
        access = access,
        newcomer = walks.isEmpty(),
        lastWalk = walks.maxByOrNull { it.startedAt },
        dailySteps = dailySteps,
        streak = currentStreak(byDay.keys, today),
        week = dayBars(byDay.mapValues { (_, ofDay) -> ofDay.sumOf { it.steps } }, week, today),
        weekWalks = (0..DayOfWeek.SUNDAY.ordinal).map { day ->
            byDay[week.from.plusDays(day.toLong())].orEmpty().map { it.id }
        },
    )
}
