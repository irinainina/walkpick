package com.walkpick.app.debug

import androidx.compose.runtime.Composable
import com.walkpick.app.data.local.HealthEntryEntity
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.ui.health.ChartGoal
import com.walkpick.app.ui.health.HealthChart
import com.walkpick.app.ui.health.HealthCurrent
import com.walkpick.app.ui.health.HealthInsight
import com.walkpick.app.ui.health.HealthState
import com.walkpick.app.ui.screens.HealthScreen
import com.walkpick.app.ui.stats.StatsTab
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val CHART_HEIGHT = 140f
private val today = LocalDate.of(2026, 9, 30)
private val january = LocalDate.of(2026, 1, 1)

private fun entry(kind: HealthKind, time: String, value: Double, diastolic: Int? = null) = HealthEntryEntity(
    id = kind.name,
    kind = kind,
    measuredAt = today.atTime(LocalTime.parse(time)).atZone(ZoneId.systemDefault()).toInstant(),
    value = value,
    diastolic = diastolic,
)

// Heights and distances from the top of the chart, as the frames draw them.
private val steps = listOf(75, 79, 87, 93, 107, 104, 98, 101, 111).map { it / CHART_HEIGHT }

private fun line(vararg fromTop: Int): List<Float?> = fromTop.map { 1 - it / CHART_HEIGHT }

private fun goal(fromTop: Int, value: Double) = ChartGoal(1 - fromTop / CHART_HEIGHT, value)

private fun yearChart(lines: List<List<Float?>>, goals: List<ChartGoal>) =
    HealthChart(12, "JFMAMJJASOND".map { it.toString() }, 8, steps, lines, goals)

private val moodChart = HealthChart(
    columns = 30,
    labels = listOf("Sep 1", "Sep 15", "Sep 30"),
    bars = listOf(
        65, 43, 75, 108, 54, 65, 32, 86, 118, 97, 43, 75, 97, 129, 54,
        22, 65, 86, 108, 118, 75, 97, 43, 86, 129, 97, 108, 54, 86, 97,
    ).map { it / CHART_HEIGHT },
    lines = emptyList(),
    moods = listOf(4, 3, 4, 5, 3, 4, 2, 4, 5, 4, 3, 4, 4, 5, 3, 1, 3, 4, 4, 5, 4, 4, 3, 4, 5, 4, 4, 2, 4, 4),
)

private class Page(val current: HealthCurrent, val chart: HealthChart, val insight: HealthInsight)

private val pages = mapOf(
    "health-mood" to Page(
        HealthCurrent(entry(HealthKind.MOOD, "09:30", 4.0), usualMood = 4),
        moodChart,
        HealthInsight.BetterMoodOnActiveDays,
    ),
    "health-weight" to Page(
        HealthCurrent(entry(HealthKind.WEIGHT, "08:10", 64.2), -2.1, true),
        yearChart(listOf(line(15, 17, 24, 28, 35, 39, 33, 48, 62)), listOf(goal(111, 62.0))),
        HealthInsight.WalkMore(2300, january),
    ),
    "health-pressure" to Page(
        HealthCurrent(entry(HealthKind.PRESSURE, "08:10", 122.0, 78), -6.0, true),
        yearChart(
            lines = listOf(
                line(20, 20, 22, 24, 24, 26, 28, 29, 31),
                line(101, 103, 103, 104, 104, 106, 106, 108, 110),
            ),
            goals = listOf(goal(35, 120.0), goal(106, 80.0)),
        ),
        HealthInsight.Changed(-6.0, january),
    ),
    "health-glucose" to Page(
        HealthCurrent(entry(HealthKind.GLUCOSE, "07:50", 5.4), -0.5, true),
        yearChart(listOf(line(22, 22, 37, 37, 51, 66, 51, 81, 95)), listOf(goal(81, 5.5))),
        HealthInsight.Changed(-0.5, january),
    ),
)

/** Health with the data of the Figma frames, for the comparison with the design. */
@Composable
fun HealthGallery(page: String) {
    val shown = pages[page]
    HealthScreen(
        state = HealthState(
            tabs = HealthKind.entries,
            tab = shown?.current?.latest?.kind ?: HealthKind.WEIGHT,
            period = if (page == "health-mood") StatsTab.Month else StatsTab.Year,
            today = today,
            current = shown?.current,
            chart = shown?.chart,
            insight = shown?.insight,
        ),
        onDelete = {},
        onSave = { _, _, _, _ -> },
        onTab = {},
        onPeriod = {},
    )
}
