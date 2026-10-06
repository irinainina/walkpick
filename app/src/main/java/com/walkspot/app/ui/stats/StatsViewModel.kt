package com.walkspot.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.WalkDao
import com.walkspot.app.data.profile.ProfileRepository
import com.walkspot.app.data.settings.DistanceUnit
import com.walkspot.app.data.settings.SettingsStore
import com.walkspot.app.data.steps.DEFAULT_DAILY_GOAL
import com.walkspot.app.data.steps.DayRange
import com.walkspot.app.data.steps.MonthAverage
import com.walkspot.app.data.steps.PeriodStats
import com.walkspot.app.data.steps.StepsRepository
import com.walkspot.app.data.steps.goalMet
import com.walkspot.app.data.steps.longestStreak
import com.walkspot.app.data.steps.monthOf
import com.walkspot.app.data.steps.monthlyAverages
import com.walkspot.app.data.steps.stepsStats
import com.walkspot.app.data.steps.weekOf
import com.walkspot.app.data.steps.yearOf
import com.walkspot.app.ui.components.Bar
import com.walkspot.app.ui.components.BarKind
import com.walkspot.app.ui.grouped
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L
private const val DAYS_IN_WEEK = 7

// The goal stands at five sixths of the track, as the goal line of the design does.
private const val TRACK_OVER_GOAL = 1.2f

enum class StatsTab { Week, Month, Year, All }

data class CalendarDay(val day: LocalDate, val goalMet: Boolean)

data class StatsState(
    val tab: StatsTab,
    val today: LocalDate,
    val range: DayRange,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val stats: PeriodStats,
    val goal: Int,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    /** Days for Week, months for Year and All. */
    val bars: List<Bar> = emptyList(),
    /** Where the goal line crosses the track; Week only. */
    val goalLine: Float? = null,
    val selectedBar: Int? = null,
    /** Month only. */
    val calendar: List<CalendarDay> = emptyList(),
    val bestMonth: YearMonth? = null,
    val walks: Int = 0,
    val longestStreak: Int = 0,
)

/** What the user chose: the tab, a day inside the period shown and the bar picked by hand. */
data class StatsChoice(
    val tab: StatsTab = StatsTab.Week,
    val anchor: LocalDate = LocalDate.now(),
    val bar: Int? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    steps: StepsRepository,
    profiles: ProfileRepository,
    settings: SettingsStore,
    walks: WalkDao,
) : ViewModel() {

    private val choice = MutableStateFlow(StatsChoice())

    private val unit = settings.value.map { it.distance }

    private val goal = profiles.profile.map { it.dailyGoal }

    private val walkDays = walks.watchStarts().map { starts -> starts.map { it.localDay() } }

    val state: StateFlow<StatsState?> =
        combine(steps.watchDays(), walkDays, choice, goal, unit) { days, walked, choice, goal, unit ->
            statsState(days, walked, choice, LocalDate.now(), goal).copy(distanceUnit = unit)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)

    fun open(tab: StatsTab) = choice.update { StatsChoice(tab) }

    fun move(periods: Long) = choice.update {
        val anchor = when (it.tab) {
            StatsTab.Week -> it.anchor.plusWeeks(periods)
            StatsTab.Month -> it.anchor.plusMonths(periods)
            StatsTab.Year -> it.anchor.plusYears(periods)
            StatsTab.All -> it.anchor
        }
        StatsChoice(it.tab, anchor)
    }

    fun pick(bar: Int) = choice.update { it.copy(bar = bar) }
}

private fun Instant.localDay() = atZone(ZoneId.systemDefault()).toLocalDate()

internal fun statsState(
    days: List<DailyStepsEntity>,
    walkDays: List<LocalDate>,
    choice: StatsChoice,
    today: LocalDate,
    profileGoal: Int = DEFAULT_DAILY_GOAL,
): StatsState {
    val tab = choice.tab
    val first = days.minByOrNull { it.day.toEpochDay() }?.day ?: today
    val range = when (tab) {
        StatsTab.Week -> weekOf(choice.anchor)
        StatsTab.Month -> monthOf(YearMonth.from(choice.anchor))
        StatsTab.Year -> yearOf(choice.anchor)
        StatsTab.All -> DayRange(first, today)
    }
    val goal = days.lastOrNull { it.day == today }?.goal ?: profileGoal
    val byDay = days.associateBy { it.day }
    val months = when (tab) {
        StatsTab.Year, StatsTab.All -> monthlyAverages(days, range, today)
        else -> emptyList()
    }
    val bars = when (tab) {
        StatsTab.Week -> dayBars(byDay, range, goal, today)
        StatsTab.Month -> emptyList()
        else -> monthBars(months, years = tab == StatsTab.All, goal, YearMonth.from(today))
    }
    // The current day or month is picked until the user picks another; All shows no tip of its own.
    val current = bars.indexOfFirst { it.kind == BarKind.Today }.takeIf { it >= 0 && tab != StatsTab.All }
    return StatsState(
        tab = tab,
        today = today,
        range = range,
        canGoBack = tab != StatsTab.All && range.from > first,
        canGoForward = tab != StatsTab.All && range.to < today,
        stats = stepsStats(days, range, today),
        goal = goal,
        bars = bars,
        goalLine = (goal / trackTop(goal, byDay.steps(range))).takeIf { tab == StatsTab.Week },
        selectedBar = choice.bar ?: current,
        calendar = monthDays(byDay, range).takeIf { tab == StatsTab.Month }.orEmpty(),
        bestMonth = months.filter { it.steps > 0 }.maxByOrNull { it.steps }?.month,
        walks = walkDays.size,
        longestStreak = longestStreak(walkDays.toSet()),
    )
}

private fun trackTop(goal: Int, values: List<Int>) =
    maxOf(goal * TRACK_OVER_GOAL, values.maxOrNull()?.toFloat() ?: 0f)

private fun Map<LocalDate, DailyStepsEntity>.steps(week: DayRange) =
    List(DAYS_IN_WEEK) { this[week.from.plusDays(it.toLong())]?.steps ?: 0 }

private fun monthDays(byDay: Map<LocalDate, DailyStepsEntity>, month: DayRange) =
    List(month.to.dayOfMonth) { month.from.plusDays(it.toLong()) }
        .map { CalendarDay(it, byDay[it]?.goalMet == true) }

private fun dayBars(
    byDay: Map<LocalDate, DailyStepsEntity>,
    week: DayRange,
    goal: Int,
    today: LocalDate,
): List<Bar> {
    val values = byDay.steps(week)
    val top = trackTop(goal, values)
    return values.mapIndexed { index, steps ->
        val day = week.from.plusDays(index.toLong())
        Bar(
            fraction = steps / top,
            label = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
            kind = when {
                day == today -> BarKind.Today
                byDay[day]?.goalMet == true -> BarKind.GoalReached
                else -> BarKind.Past
            },
            tip = grouped(steps).takeIf { steps > 0 },
        )
    }
}

// [years] labels a bar with its year where a year begins on the chart, instead of the month letter.
private fun monthBars(
    months: List<MonthAverage>,
    years: Boolean,
    goal: Int,
    thisMonth: YearMonth,
): List<Bar> {
    val top = trackTop(goal, months.map { it.steps })
    return months.mapIndexed { index, (month, steps) ->
        Bar(
            fraction = steps / top,
            label = when {
                !years -> month.month.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
                index == 0 || month.monthValue == 1 -> month.year.toString()
                else -> ""
            },
            kind = if (month == thisMonth) BarKind.Today else BarKind.Past,
            tip = grouped(steps).takeIf { steps > 0 },
            accentLabel = month == thisMonth && !years,
        )
    }
}
