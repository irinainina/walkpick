package com.walkspot.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.account.AuthRefused
import com.walkspot.app.data.account.AuthRepository
import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.WalkDao
import com.walkspot.app.data.steps.DEFAULT_DAILY_GOAL
import com.walkspot.app.data.steps.StepsAccess
import com.walkspot.app.data.steps.StepsRepository
import com.walkspot.app.data.steps.currentStreak
import com.walkspot.app.data.steps.goalMet
import com.walkspot.app.data.steps.weekOf
import com.walkspot.app.ui.components.Bar
import com.walkspot.app.ui.components.BarKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

// The sensor is cheap to ask; this is how stale the ring may get while the screen is open.
private const val REFRESH_MS = 10_000L
private const val KEEP_AFTER_LEAVING_MS = 5_000L

data class TodayState(
    val now: LocalDateTime,
    val name: String? = null,
    val access: StepsAccess = StepsAccess.COUNTING,
    val steps: Int = 0,
    val goal: Int = DEFAULT_DAILY_GOAL,
    val distanceM: Int = 0,
    val activeMinutes: Int = 0,
    val streak: Int = 0,
    val week: List<Bar> = emptyList(),
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val steps: StepsRepository,
    walks: WalkDao,
    auth: AuthRepository,
) : ViewModel() {

    private val now = MutableStateFlow(LocalDateTime.now())
    private val access = MutableStateFlow(steps.access())
    private val name = MutableStateFlow<String?>(null)

    private val walkDays = walks.watchStarts().map { starts ->
        starts.map { it.atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
    }

    val state: StateFlow<TodayState> = combine(steps.watchDays(), walkDays, now, access, name, ::todayState)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS),
            TodayState(now.value, access = access.value),
        )

    init {
        viewModelScope.launch {
            // The greeting simply goes without a name.
            name.value = try {
                auth.currentUser()?.name
            } catch (_: AuthRefused) {
                null
            }
        }
    }

    // Runs while the screen is in front; the caller cancels it.
    suspend fun keepFresh() {
        while (true) {
            access.value = steps.access()
            steps.refresh()
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
        goal = row?.goal ?: DEFAULT_DAILY_GOAL,
        distanceM = row?.distanceM ?: 0,
        activeMinutes = row?.activeMinutes ?: 0,
        streak = currentStreak(walkDays, today),
        week = DayOfWeek.entries.map { weekday ->
            val day = monday.plusDays(weekday.ordinal.toLong())
            val steps = byDay[day]
            Bar(
                fraction = steps?.let { it.steps.toFloat() / it.goal } ?: 0f,
                label = weekday.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                kind = when {
                    day == today -> BarKind.Today
                    steps?.goalMet == true -> BarKind.GoalReached
                    else -> BarKind.Past
                },
            )
        },
    )
}
