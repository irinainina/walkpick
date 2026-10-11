package com.walkspot.app.data.settings

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

// Values are kept in metres, kilograms and mmol/L; a unit only changes what the screens show.
enum class DistanceUnit(val symbol: String, val perMetric: Double) {
    KM("km", 1.0),
    MI("mi", 0.621371),
}

enum class WeightUnit(val symbol: String, val perMetric: Double) {
    KG("kg", 1.0),
    LB("lb", 2.20462),
}

enum class GlucoseUnit(val symbol: String, val perMetric: Double) {
    MMOL("mmol/L", 1.0),
    MGDL("mg/dL", 18.016),
}

data class AppSettings(
    val remindersOn: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(18, 0),
    /** Never empty. */
    val reminderDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val distance: DistanceUnit = DistanceUnit.KM,
    val weight: WeightUnit = WeightUnit.KG,
    val glucose: GlucoseUnit = GlucoseUnit.MMOL,
    /** A walk asks how the walker feels when it begins and when it ends. Kept on this phone only. */
    val walkMood: Boolean = true,
) {
    /** The last day left on stays on. */
    fun withDay(day: DayOfWeek, on: Boolean): AppSettings {
        val days = if (on) reminderDays + day else reminderDays - day
        return if (days.isEmpty()) this else copy(reminderDays = days)
    }
}

/** When the walk reminder is due next, or null when it is switched off. */
fun nextReminder(settings: AppSettings, now: LocalDateTime): LocalDateTime? {
    if (!settings.remindersOn) return null
    return generateSequence(now.toLocalDate()) { it.plusDays(1) }
        .map { it.atTime(settings.reminderTime) }
        .first { it.isAfter(now) && it.dayOfWeek in settings.reminderDays }
}
