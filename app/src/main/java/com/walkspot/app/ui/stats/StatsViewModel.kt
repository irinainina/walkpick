package com.walkspot.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.profile.ProfileRepository
import com.walkspot.app.data.settings.DistanceUnit
import com.walkspot.app.data.settings.SettingsStore
import com.walkspot.app.data.steps.DayRange
import com.walkspot.app.data.steps.MonthAverage
import com.walkspot.app.data.steps.PeriodStats
import com.walkspot.app.data.steps.StepsRepository
import com.walkspot.app.data.steps.longestStreak
import com.walkspot.app.data.steps.monthOf
import com.walkspot.app.data.steps.monthlyAverages
import com.walkspot.app.data.steps.stepsStats
import com.walkspot.app.data.steps.weekOf
import com.walkspot.app.data.steps.yearOf
import com.walkspot.app.data.walk.WalkRepository
import com.walkspot.app.ui.components.Bar
import com.walkspot.app.ui.components.BarKind
import com.walkspot.app.ui.grouped
import com.walkspot.app.ui.localDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L
private const val DAYS_IN_WEEK = 7

enum class StatsTab { Week, Month, Year, All }

data class CalendarDay(val day: LocalDate, val walked: Boolean)

data class StatsState(
    val tab: StatsTab,
    val today: LocalDate,
    val range: DayRange,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val stats: PeriodStats,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    /** Days for Week, months for Year and All. */
    val bars: List<Bar> = emptyList(),
    val selectedBar: Int? = null,
    /** Month only. */
    val calendar: List<CalendarDay> = emptyList(),
    val bestMonth: YearMonth? = null,
    val walks: Int = 0,
    /** The days of the period shown that have a walk. */
    val walkDays: Int = 0,
    val longestStreak: Int = 0,
    /** The day of the calendar tapped last and its walks, when it has several: a sheet offers them. */
    val day: LocalDate? = null,
    val dayWalks: List<WalkEntity> = emptyList(),
)

/** What the user chose: the tab, a day inside the period shown and the bar picked by hand. */
data class StatsChoice(
    val tab: StatsTab = StatsTab.Week,
    val anchor: LocalDate = LocalDate.now(),
    val bar: Int? = null,
    val day: LocalDate? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    steps: StepsRepository,
    settings: SettingsStore,
    profiles: ProfileRepository,
    walks: WalkRepository,
) : ViewModel() {

    private val choice = MutableStateFlow(StatsChoice())

    private val mine =
        combine(settings.value, profiles.profile) { units, me -> units.distance to me.dailyGoal }

    private val all = walks.watchAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<StatsState?> =
        combine(steps.watchDays(), all, choice, mine) { days, walks, choice, (unit, dailySteps) ->
            val walkDays = walks.map { it.startedAt.localDay() }
            statsState(days, walkDays, choice, LocalDate.now(), dailySteps).copy(
                distanceUnit = unit,
                day = choice.day,
                dayWalks = walks.filter { it.startedAt.localDay() == choice.day },
            )
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

    /** A day of the calendar is tapped: its only walk opens at once, several are offered in a sheet. */
    fun pickDay(day: LocalDate, onWalk: (String) -> Unit) {
        val ofDay = all.value.filter { it.startedAt.localDay() == day }
        if (ofDay.size == 1) onWalk(ofDay.first().id) else choice.update { it.copy(day = day) }
    }

    fun closeDay() = choice.update { it.copy(day = null) }
}

internal fun statsState(
    days: List<DailyStepsEntity>,
    walkDays: List<LocalDate>,
    choice: StatsChoice,
    today: LocalDate,
    dailySteps: Int,
): StatsState {
    val tab = choice.tab
    val first = days.minByOrNull { it.day.toEpochDay() }?.day ?: today
    val range = when (tab) {
        StatsTab.Week -> weekOf(choice.anchor)
        StatsTab.Month -> monthOf(YearMonth.from(choice.anchor))
        StatsTab.Year -> yearOf(choice.anchor)
        StatsTab.All -> DayRange(first, today)
    }
    val walked = walkDays.toSet()
    val byDay = days.associateBy { it.day }
    val months = when (tab) {
        StatsTab.Year, StatsTab.All -> monthlyAverages(days, range, today)
        else -> emptyList()
    }
    val bars = when (tab) {
        StatsTab.Week -> dayBars(byDay, range, today, dailySteps)
        StatsTab.Month -> emptyList()
        else -> monthBars(months, years = tab == StatsTab.All, YearMonth.from(today), dailySteps)
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
        bars = bars,
        selectedBar = choice.bar ?: current,
        calendar = monthDays(walked, range).takeIf { tab == StatsTab.Month }.orEmpty(),
        bestMonth = months.filter { it.steps > 0 }.maxByOrNull { it.steps }?.month,
        walks = walkDays.size,
        walkDays = walked.count { it in range },
        longestStreak = longestStreak(walked),
    )
}

// A track holds a full day of steps, or the best value shown when that is more.
private fun trackTop(values: List<Int>, dailySteps: Int) =
    maxOf(dailySteps, values.maxOrNull() ?: 0).toFloat()

private fun Map<LocalDate, DailyStepsEntity>.steps(week: DayRange) =
    List(DAYS_IN_WEEK) { this[week.from.plusDays(it.toLong())]?.steps ?: 0 }

private fun monthDays(walked: Set<LocalDate>, month: DayRange) =
    List(month.to.dayOfMonth) { month.from.plusDays(it.toLong()) }.map { CalendarDay(it, it in walked) }

private fun dayBars(
    byDay: Map<LocalDate, DailyStepsEntity>,
    week: DayRange,
    today: LocalDate,
    dailySteps: Int,
): List<Bar> {
    val values = byDay.steps(week)
    val top = trackTop(values, dailySteps)
    return values.mapIndexed { index, steps ->
        val day = week.from.plusDays(index.toLong())
        Bar(
            fraction = steps / top,
            label = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
            kind = if (day == today) BarKind.Today else BarKind.Past,
            tip = grouped(steps).takeIf { steps > 0 },
        )
    }
}

// [years] labels a bar with its year where a year begins on the chart, instead of the month letter.
private fun monthBars(
    months: List<MonthAverage>,
    years: Boolean,
    thisMonth: YearMonth,
    dailySteps: Int,
): List<Bar> {
    val top = trackTop(months.map { it.steps }, dailySteps)
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
