package com.walkpick.app.data

import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.profile.Profile
import com.walkpick.app.data.profile.Sex
import com.walkpick.app.data.profile.strideM
import com.walkpick.app.data.profile.toProfileRow
import com.walkpick.app.data.profile.toSettingsRow
import com.walkpick.app.data.profile.withProfileRow
import com.walkpick.app.data.profile.withSettingsRow
import com.walkpick.app.ui.screens.birthDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ProfileTest {

    private val today = LocalDate.of(2026, 9, 30)

    @Test
    fun theStrideComesFromTheHeight() {
        assertEquals(0.7, strideM(null, null))
        assertEquals(0.69384, strideM(Sex.FEMALE, 168), 1e-9)
        assertEquals(0.747, strideM(Sex.MALE, 180), 1e-9)
    }

    @Test
    fun theAgeTurnsOnTheBirthday() {
        val profile = Profile(birthDate = LocalDate.of(1992, 9, 30))
        assertEquals(34, profile.age(today))
        assertEquals(33, profile.age(today.minusDays(1)))
        assertEquals(null, Profile().age(today))
    }

    @Test
    fun theLastTabStaysOn() {
        val one = Profile().withTab(HealthKind.MOOD, on = false)
        assertEquals(setOf(HealthKind.WEIGHT), one.healthTabs)
        assertEquals(one, one.withTab(HealthKind.WEIGHT, on = false))
        val two = one.withTab(HealthKind.GLUCOSE, on = true)
        assertEquals(setOf(HealthKind.WEIGHT, HealthKind.GLUCOSE), two.healthTabs)
    }

    @Test
    fun aProfileSurvivesTheTripToTheServerAndBack() {
        val profile = Profile(
            sex = Sex.FEMALE,
            birthDate = LocalDate.of(1992, 3, 14),
            heightCm = 168,
            dailyGoal = 8000,
            ownTargetWeightKg = 62.0,
            targetSystolic = 118,
            targetDiastolic = 76,
            targetGlucose = 5.2,
            healthTabs = setOf(HealthKind.MOOD, HealthKind.PRESSURE),
        )
        val back = Profile()
            .withProfileRow(profile.toProfileRow("user"))
            .withSettingsRow(profile.toSettingsRow("user"))
        assertEquals(profile, back)
    }

    @Test
    fun aDateOfBirthHasToBeARealDayInThePast() {
        assertEquals(LocalDate.of(1992, 3, 14), birthDate("14", "3", "1992", today))
        assertEquals(null, birthDate("31", "2", "1992", today))
        assertEquals(null, birthDate("1", "10", "2026", today))
        assertEquals(null, birthDate("1", "1", "1900", today))
        assertEquals(null, birthDate("", "3", "1992", today))
    }
}
