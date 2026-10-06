package com.walkspot.app.data.profile

import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.steps.DEFAULT_DAILY_GOAL
import java.time.LocalDate
import java.time.Period
import kotlin.math.roundToInt

enum class Sex { FEMALE, MALE }

data class Profile(
    val sex: Sex? = null,
    val birthDate: LocalDate? = null,
    val heightCm: Int? = null,
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    /** Set by hand; until then the target is [idealWeightKg]. */
    val ownTargetWeightKg: Double? = null,
    val targetSystolic: Int = 120,
    val targetDiastolic: Int = 80,
    val targetGlucose: Double = 5.5,
    /** The tabs shown on Health and on Today; never empty. */
    val healthTabs: Set<HealthKind> = setOf(HealthKind.MOOD, HealthKind.WEIGHT),
) {
    val targetWeightKg: Double? get() = ownTargetWeightKg ?: idealWeightKg(sex, heightCm)

    fun age(today: LocalDate): Int? = birthDate?.let { Period.between(it, today).years }

    /** The last tab left on stays on. */
    fun withTab(tab: HealthKind, on: Boolean): Profile {
        val tabs = if (on) healthTabs + tab else healthTabs - tab
        return if (tabs.isEmpty()) this else copy(healthTabs = tabs)
    }
}

private const val CM_IN_INCH = 2.54
private const val FIVE_FEET_IN = 60
private const val TENTHS = 10.0

// Robinson, 1983: kg at five feet and kg for every inch above, by sex.
private val RobinsonFemale = 49.0 to 1.7
private val RobinsonMale = 52.0 to 1.9

fun idealWeightKg(sex: Sex?, heightCm: Int?): Double? {
    if (sex == null || heightCm == null) return null
    val (base, perInch) = if (sex == Sex.FEMALE) RobinsonFemale else RobinsonMale
    val kg = base + perInch * (heightCm / CM_IN_INCH - FIVE_FEET_IN)
    return (kg * TENTHS).roundToInt() / TENTHS
}

private const val DEFAULT_STRIDE_M = 0.7
private const val CM_IN_M = 100
private const val STRIDE_OF_HEIGHT_FEMALE = 0.413
private const val STRIDE_OF_HEIGHT_MALE = 0.415

/** A walking step is about 41 % of the height; 0.7 m until the height is known. */
fun strideM(sex: Sex?, heightCm: Int?): Double {
    if (heightCm == null) return DEFAULT_STRIDE_M
    val share = if (sex == Sex.MALE) STRIDE_OF_HEIGHT_MALE else STRIDE_OF_HEIGHT_FEMALE
    return heightCm * share / CM_IN_M
}
