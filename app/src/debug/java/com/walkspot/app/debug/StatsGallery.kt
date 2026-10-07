package com.walkspot.app.debug

import androidx.compose.runtime.Composable
import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.steps.DayRange
import com.walkspot.app.data.steps.PeriodStats
import com.walkspot.app.data.steps.monthOf
import com.walkspot.app.data.steps.weekOf
import com.walkspot.app.data.steps.yearOf
import com.walkspot.app.ui.components.Bar
import com.walkspot.app.ui.components.BarKind
import com.walkspot.app.ui.screens.StatsScreen
import com.walkspot.app.ui.stats.CalendarDay
import com.walkspot.app.ui.stats.StatsState
import com.walkspot.app.ui.stats.StatsTab
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

private const val TRACK = 120f
private val today = LocalDate.of(2026, 9, 30)
private val september = YearMonth.of(2026, 9)

private fun best(day: LocalDate, steps: Int) = DailyStepsEntity(day, steps, 0, 0, 10000, Instant.EPOCH)

private fun bars(heights: List<Int>, labels: List<String>, kinds: Map<Int, BarKind>, tips: Map<Int, String>) =
    heights.mapIndexed { index, height ->
        Bar(height / TRACK, labels[index], kinds[index] ?: BarKind.Past, tips[index])
    }

private val week = StatsState(
    tab = StatsTab.Week,
    today = today,
    range = weekOf(today),
    canGoBack = true,
    canGoForward = true,
    stats = PeriodStats(22860, 7620, 16800, best(LocalDate.of(2026, 9, 28), 10240)),
    bars = bars(
        heights = listOf(104, 64, 64, 0, 0, 0, 0),
        labels = "MTWTFSS".map { it.toString() },
        kinds = mapOf(2 to BarKind.Today),
        tips = mapOf(2 to "6,420"),
    ),
    selectedBar = 2,
)

private val month = week.copy(
    tab = StatsTab.Month,
    range = monthOf(september),
    stats = PeriodStats(214300, 7143, 0, best(LocalDate.of(2026, 9, 13), 12480)),
    bars = emptyList(),
    walkDays = 10,
    calendar = List(september.lengthOfMonth()) { september.atDay(it + 1) }
        .map { CalendarDay(it, it.dayOfMonth in setOf(1, 8, 9, 13, 15, 18, 22, 25, 26, 28)) },
)

private val year = week.copy(
    tab = StatsTab.Year,
    range = yearOf(today),
    stats = PeriodStats(1680000, 6150, 1209000, null),
    bars = bars(
        heights = listOf(56, 60, 68, 72, 84, 80, 76, 80, 84, 0, 0, 0),
        labels = "JFMAMJJASOND".map { it.toString() },
        kinds = mapOf(8 to BarKind.Today),
        tips = mapOf(8 to "7,140"),
    ),
    selectedBar = 8,
    walkDays = 96,
    bestMonth = september,
)

private val all = week.copy(
    tab = StatsTab.All,
    range = DayRange(LocalDate.of(2025, 6, 1), today),
    canGoBack = false,
    canGoForward = false,
    stats = PeriodStats(2790000, 5740, 2009000, best(LocalDate.of(2026, 5, 3), 18240)),
    bars = bars(
        heights = listOf(52, 56, 64, 68, 72, 60, 60, 56, 60, 68, 72, 84, 80, 76, 80, 84),
        labels = List(16) { if (it == 0) "2025" else if (it == 7) "2026" else "" },
        kinds = mapOf(15 to BarKind.Today),
        tips = emptyMap(),
    ),
    selectedBar = null,
    walks = 312,
    longestStreak = 21,
)

/** Stats with the data of the Figma frames, for the comparison with the design. */
@Composable
fun StatsGallery(page: String) {
    val state = when (page) {
        "stats-month" -> month
        "stats-year" -> year
        "stats-all" -> all
        else -> week
    }
    StatsScreen(
        state = state,
        onBack = {},
        onTab = {},
        onMove = {},
        onPickBar = {},
        onPickDay = {},
        onWalk = {},
        onCloseDay = {},
    )
}
