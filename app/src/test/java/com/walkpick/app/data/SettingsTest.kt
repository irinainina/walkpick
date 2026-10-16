package com.walkpick.app.data

import com.walkpick.app.data.settings.AppSettings
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.data.settings.GlucoseUnit
import com.walkpick.app.data.settings.WeightUnit
import com.walkpick.app.data.settings.nextReminder
import com.walkpick.app.data.settings.toRow
import com.walkpick.app.data.settings.withRow
import com.walkpick.app.ui.distanceText
import com.walkpick.app.ui.glucoseText
import com.walkpick.app.ui.screens.timeOfDay
import com.walkpick.app.ui.weightText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class SettingsTest {

    // Wednesday, as in the designs
    private val morning = LocalDateTime.of(2026, 9, 30, 9, 41)
    private val weekdays = AppSettings(remindersOn = true, reminderDays = DayOfWeek.entries.take(5).toSet())

    @Test
    fun theReminderComesTodayWhenItsHourIsStillAhead() {
        assertEquals(LocalDateTime.of(2026, 9, 30, 15, 0), nextReminder(weekdays, morning))
    }

    @Test
    fun afterItsHourTheReminderMovesToTheNextChosenDay() {
        val friday = LocalDateTime.of(2026, 10, 2, 15, 0)
        assertEquals(LocalDateTime.of(2026, 10, 5, 15, 0), nextReminder(weekdays, friday))
        val evening = morning.withHour(15).withMinute(1)
        assertEquals(LocalDateTime.of(2026, 10, 1, 15, 0), nextReminder(weekdays, evening))
    }

    @Test
    fun aReminderSwitchedOffIsNeverDue() {
        assertEquals(null, nextReminder(AppSettings(), morning))
    }

    @Test
    fun theLastDayStaysOn() {
        val one = AppSettings(reminderDays = setOf(DayOfWeek.MONDAY))
        assertEquals(one, one.withDay(DayOfWeek.MONDAY, on = false))
        val two = one.withDay(DayOfWeek.FRIDAY, on = true)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), two.reminderDays)
    }

    @Test
    fun settingsSurviveTheTripToTheServerAndBack() {
        val settings = AppSettings(
            remindersOn = true,
            reminderTime = LocalTime.of(7, 30),
            reminderDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            distance = DistanceUnit.MI,
            weight = WeightUnit.LB,
            glucose = GlucoseUnit.MGDL,
        )
        assertEquals(settings, AppSettings().withRow(settings.toRow("user")))
    }

    @Test
    fun valuesAreShownInTheChosenUnits() {
        assertEquals("4.8 km", distanceText(4800, DistanceUnit.KM))
        assertEquals("3.0 mi", distanceText(4800, DistanceUnit.MI))
        assertEquals("1\u00A0209 km", distanceText(1_209_400, DistanceUnit.KM))
        assertEquals("0 km", distanceText(0, DistanceUnit.KM))
        assertEquals("64.2 kg", weightText(64.2, WeightUnit.KG))
        assertEquals("141.5 lb", weightText(64.2, WeightUnit.LB))
        assertEquals("5.5 mmol/L", glucoseText(5.5, GlucoseUnit.MMOL))
        assertEquals("99 mg/dL", glucoseText(5.5, GlucoseUnit.MGDL))
    }

    @Test
    fun aTimeOfDayHasToExist() {
        assertEquals(LocalTime.of(7, 5), timeOfDay("7", "05"))
        assertEquals(null, timeOfDay("24", "00"))
        assertEquals(null, timeOfDay("7", ""))
    }
}
