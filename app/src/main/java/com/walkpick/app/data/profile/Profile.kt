package com.walkpick.app.data.profile

import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.steps.DEFAULT_DAILY_GOAL
import java.time.LocalDate
import java.time.Period

enum class Sex { FEMALE, MALE }

data class Profile(
    val sex: Sex? = null,
    val birthDate: LocalDate? = null,
    val heightCm: Int? = null,
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    /** The targets are the walker's own or none: nothing is worked out for them. */
    val ownTargetWeightKg: Double? = null,
    val targetSystolic: Int? = null,
    val targetDiastolic: Int? = null,
    val targetGlucose: Double? = null,
    /** The tabs shown on Health and on Today; never empty. */
    val healthTabs: Set<HealthKind> = setOf(HealthKind.MOOD, HealthKind.WEIGHT),
) {
    fun age(today: LocalDate): Int? = birthDate?.let { Period.between(it, today).years }

    /** The last tab left on stays on. */
    fun withTab(tab: HealthKind, on: Boolean): Profile {
        val tabs = if (on) healthTabs + tab else healthTabs - tab
        return if (tabs.isEmpty()) this else copy(healthTabs = tabs)
    }
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
