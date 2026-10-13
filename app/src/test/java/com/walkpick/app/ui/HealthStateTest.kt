package com.walkpick.app.ui

import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.ui.health.HealthChoice
import com.walkpick.app.ui.health.healthState
import com.walkpick.app.ui.stats.StatsTab
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HealthStateTest {

    @Test
    fun theTabsAreThoseTurnedOnInProfileInTheirFixedOrder() {
        val state = healthState(setOf(HealthKind.GLUCOSE, HealthKind.MOOD), HealthChoice())
        assertEquals(listOf(HealthKind.MOOD, HealthKind.GLUCOSE), state.tabs)
        assertEquals(HealthKind.MOOD, state.tab)
        assertEquals(StatsTab.Month, state.period)
    }

    @Test
    fun theChosenTabAndPeriodStay() {
        val choice = HealthChoice(HealthKind.WEIGHT, StatsTab.Year)
        val state = healthState(setOf(HealthKind.MOOD, HealthKind.WEIGHT), choice)
        assertEquals(HealthKind.WEIGHT, state.tab)
        assertEquals(StatsTab.Year, state.period)
    }

    @Test
    fun aTabTurnedOffGivesWayToTheFirstOneLeft() {
        val state = healthState(setOf(HealthKind.WEIGHT, HealthKind.PRESSURE), HealthChoice(HealthKind.MOOD))
        assertEquals(HealthKind.WEIGHT, state.tab)
    }
}
