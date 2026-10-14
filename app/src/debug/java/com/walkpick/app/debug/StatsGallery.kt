package com.walkpick.app.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.steps.DayRange
import com.walkpick.app.data.steps.monthOf
import com.walkpick.app.data.steps.weekOf
import com.walkpick.app.data.steps.yearOf
import com.walkpick.app.ui.components.Bar
import com.walkpick.app.ui.components.BarKind
import com.walkpick.app.ui.screens.PeriodActions
import com.walkpick.app.ui.screens.WalkPeriods
import com.walkpick.app.ui.stats.CalendarDay
import com.walkpick.app.ui.stats.StatsState
import com.walkpick.app.ui.stats.StatsTab
import com.walkpick.app.ui.stats.WalkTotals
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.Spacing
import com.walkpick.app.ui.walk.DiaryWalk
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

private const val TRACK = 120f
private val today = LocalDate.of(2026, 9, 30)
private val september = YearMonth.of(2026, 9)

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
    totals = WalkTotals(3, 12200, 8700, 16610, LocalDate.of(2026, 9, 28), 11380),
    streak = 5,
    bars = bars(
        heights = listOf(104, 52, 0, 0, 0, 0, 0),
        labels = "MTWTFSS".map { it.toString() },
        kinds = mapOf(0 to BarKind.Walked, 1 to BarKind.Walked, 2 to BarKind.Today),
        tips = emptyMap(),
    ),
    selectedBar = 2,
)

private val month = week.copy(
    tab = StatsTab.Month,
    range = monthOf(september),
    totals = WalkTotals(10, 39200, 29100, 52300, LocalDate.of(2026, 9, 13), 8940),
    bars = emptyList(),
    calendar = List(september.lengthOfMonth()) { september.atDay(it + 1) }
        .map { CalendarDay(it, it.dayOfMonth in setOf(1, 8, 9, 13, 15, 18, 22, 25, 26, 28)) },
)

private val year = week.copy(
    tab = StatsTab.Year,
    range = yearOf(today),
    totals = WalkTotals(118, 463000, 345600, 618400),
    bars = bars(
        heights = listOf(56, 60, 68, 72, 84, 80, 76, 80, 84, 0, 0, 0),
        labels = "JFMAMJJASOND".map { it.toString() },
        kinds = mapOf(8 to BarKind.Today),
        tips = emptyMap(),
    ),
    selectedBar = null,
    bestMonth = september,
)

private val all = week.copy(
    tab = StatsTab.All,
    range = DayRange(LocalDate.of(2025, 6, 1), today),
    canGoBack = false,
    canGoForward = false,
    totals = WalkTotals(312, 1209000, 914400, 2790000, LocalDate.of(2026, 9, 28), 11380),
    bars = bars(
        heights = listOf(52, 56, 64, 68, 72, 60, 60, 56, 60, 68, 72, 84, 80, 76, 80, 84),
        labels = List(16) { if (it == 0) "2025" else if (it == 7) "2026" else "" },
        kinds = mapOf(15 to BarKind.Today),
        tips = emptyMap(),
    ),
    selectedBar = null,
    longestStreak = 21,
)

// The two walks of Monday in the frame of the day sheet, the latest first.
private fun dayWalk(
    id: String,
    name: String,
    at: String,
    minutes: Int,
    steps: Int,
    metres: Int,
    photos: Int,
): DiaryWalk {
    val start = LocalDateTime.parse(at).atZone(ZoneId.systemDefault()).toInstant()
    val walk = WalkEntity(
        id = id,
        number = 1,
        startedAt = start,
        endedAt = start.plusSeconds(minutes * 60L),
        durationS = minutes * 60,
        steps = steps,
        distanceM = metres,
        name = name,
        updatedAt = start,
    )
    val finds = List(photos) { FindEntity("$id$it", id, start, 50.58, 34.48, "none", updatedAt = start) }
    return DiaryWalk(walk, photos, ringSketch, finds)
}

private val dayWalks = listOf(
    dayWalk("evening", "Autumn Evening", "2026-09-28T17:30", 35, 4340, 3100, 0),
    dayWalk("morning", "Sunny Morning", "2026-09-28T09:15", 62, 7040, 5200, 3),
)

/** The periods of the Walk tab alone, as they stand under the last walk in the Figma frames. */
@Composable
fun PeriodsGallery(page: String) {
    val state = when (page) {
        "periods-day" -> month.copy(day = LocalDate.of(2026, 9, 28), dayWalks = dayWalks)
        "periods-month" -> month
        "periods-year" -> year
        "periods-all" -> all
        else -> week
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.canvas)
            .statusBarsPadding()
            .padding(top = Spacing.ml),
    ) {
        WalkPeriods(state, PeriodActions({}, {}, {}, {}, {}, {}))
    }
}
