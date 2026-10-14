package com.walkpick.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.data.settings.SettingsStore
import com.walkpick.app.data.steps.DayRange
import com.walkpick.app.data.steps.currentStreak
import com.walkpick.app.data.steps.longestStreak
import com.walkpick.app.data.steps.monthOf
import com.walkpick.app.data.steps.weekOf
import com.walkpick.app.data.steps.yearOf
import com.walkpick.app.data.walk.WalkRepository
import com.walkpick.app.ui.components.Bar
import com.walkpick.app.ui.components.BarKind
import com.walkpick.app.ui.grouped
import com.walkpick.app.ui.localDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

/** The walks of a period added up; the best day is the one with the most steps walked. */
data class WalkTotals(
    val walks: Int = 0,
    val distanceM: Int = 0,
    val durationS: Int = 0,
    val steps: Int = 0,
    val bestDay: LocalDate? = null,
    val bestDaySteps: Int = 0,
)

data class StatsState(
    val tab: StatsTab,
    val today: LocalDate,
    val range: DayRange,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val totals: WalkTotals,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    /** Days for Week, months for Year and All: the steps walked in each. */
    val bars: List<Bar> = emptyList(),
    val selectedBar: Int? = null,
    /** Month only. */
    val calendar: List<CalendarDay> = emptyList(),
    val bestMonth: YearMonth? = null,
    /** Days in a row with a walk up to today. */
    val streak: Int = 0,
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
    settings: SettingsStore,
    walks: WalkRepository,
) : ViewModel() {

    private val choice = MutableStateFlow(StatsChoice())

    private val all = walks.watchAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<StatsState?> =
        combine(all, choice, settings.value) { walks, choice, units ->
            statsState(walks, choice, LocalDate.now()).copy(
                distanceUnit = units.distance,
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

    /** A bar is tapped: in Week it is picked, in Year and All its month opens with its calendar. */
    fun pick(bar: Int) = choice.update {
        val state = state.value
        val month = state?.takeIf { it.tab != StatsTab.Week }?.let { monthOfBar(it, bar) }
        if (month == null) it.copy(bar = bar) else StatsChoice(StatsTab.Month, month.atDay(1))
    }

    /** A day of the calendar is tapped: its only walk opens at once, several are offered in a sheet. */
    fun pickDay(day: LocalDate, onWalk: (String) -> Unit) {
        val ofDay = all.value.filter { it.startedAt.localDay() == day }
        if (ofDay.size == 1) onWalk(ofDay.first().id) else choice.update { it.copy(day = day) }
    }

    fun closeDay() = choice.update { it.copy(day = null) }
}

// Year shows the months of its year, All the months from the first walk on.
private fun monthOfBar(state: StatsState, bar: Int): YearMonth? =
    YearMonth.from(state.range.from).plusMonths(bar.toLong()).takeIf { bar in state.bars.indices }

internal fun statsState(walks: List<WalkEntity>, choice: StatsChoice, today: LocalDate): StatsState {
    val tab = choice.tab
    val byDay = walks.groupBy { it.startedAt.localDay() }
    val first = byDay.keys.minOrNull() ?: today
    val range = when (tab) {
        StatsTab.Week -> weekOf(choice.anchor)
        StatsTab.Month -> monthOf(YearMonth.from(choice.anchor))
        StatsTab.Year -> yearOf(choice.anchor)
        StatsTab.All -> DayRange(first.withDayOfMonth(1), today)
    }
    val daySteps = byDay.mapValues { (_, ofDay) -> ofDay.sumOf { it.steps } }
    val months = when (tab) {
        StatsTab.Year, StatsTab.All -> monthSteps(daySteps, range)
        else -> emptyList()
    }
    val bars = when (tab) {
        StatsTab.Week -> dayBars(daySteps, range, today)
        StatsTab.Month -> emptyList()
        else -> monthBars(months, years = tab == StatsTab.All, YearMonth.from(today))
    }
    // Today is picked until the user picks another day; a month is not picked, a tap opens it.
    val current = bars.indexOfFirst { it.kind == BarKind.Today }.takeIf { it >= 0 && tab == StatsTab.Week }
    val walked = byDay.keys
    return StatsState(
        tab = tab,
        today = today,
        range = range,
        canGoBack = tab != StatsTab.All && range.from > first,
        canGoForward = tab != StatsTab.All && range.to < today,
        totals = totals(byDay, daySteps, range),
        bars = bars,
        selectedBar = choice.bar ?: current,
        calendar = monthDays(walked, range).takeIf { tab == StatsTab.Month }.orEmpty(),
        bestMonth = months.filter { it.second > 0 }.maxByOrNull { it.second }?.first,
        streak = currentStreak(walked, today),
        longestStreak = longestStreak(walked),
    )
}

private fun totals(
    byDay: Map<LocalDate, List<WalkEntity>>,
    daySteps: Map<LocalDate, Int>,
    range: DayRange,
): WalkTotals {
    val inRange = byDay.filterKeys { it in range }.values.flatten()
    val best = daySteps.filterKeys { it in range }.maxByOrNull { it.value }
    return WalkTotals(
        walks = inRange.size,
        distanceM = inRange.sumOf { it.distanceM },
        durationS = inRange.sumOf { it.durationS },
        steps = inRange.sumOf { it.steps },
        bestDay = best?.key,
        bestDaySteps = best?.value ?: 0,
    )
}

private fun monthSteps(daySteps: Map<LocalDate, Int>, range: DayRange): List<Pair<YearMonth, Int>> =
    generateSequence(YearMonth.from(range.from)) { it.plusMonths(1) }
        .takeWhile { it <= YearMonth.from(range.to) }
        .map { month -> month to daySteps.filterKeys { YearMonth.from(it) == month }.values.sum() }
        .toList()

// The tallest bar fills its track.
private fun top(values: List<Int>) = (values.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()

private fun monthDays(walked: Set<LocalDate>, month: DayRange) =
    List(month.to.dayOfMonth) { month.from.plusDays(it.toLong()) }.map { CalendarDay(it, it in walked) }

private fun dayBars(daySteps: Map<LocalDate, Int>, week: DayRange, today: LocalDate): List<Bar> {
    val days = List(DAYS_IN_WEEK) { week.from.plusDays(it.toLong()) }
    val top = top(days.map { daySteps[it] ?: 0 })
    return days.map { day ->
        val steps = daySteps[day] ?: 0
        Bar(
            fraction = steps / top,
            label = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
            kind = when {
                day == today -> BarKind.Today
                steps > 0 -> BarKind.Walked
                else -> BarKind.Past
            },
            tip = grouped(steps).takeIf { steps > 0 },
        )
    }
}

// [years] labels a bar with its year where a year begins on the chart, instead of the month letter.
private fun monthBars(months: List<Pair<YearMonth, Int>>, years: Boolean, thisMonth: YearMonth): List<Bar> {
    val top = top(months.map { it.second })
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
