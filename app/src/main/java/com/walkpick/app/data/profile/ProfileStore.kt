package com.walkpick.app.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.sync.SyncedPreferences
import java.time.LocalDate
import javax.inject.Inject

/** The profile as the phone keeps it. */
class ProfileStore @Inject constructor(
    store: DataStore<Preferences>,
) : SyncedPreferences<Profile>(store, "profile") {

    override fun read(saved: Preferences): Profile {
        val defaults = Profile()
        // Until the first save the targets are read from the keys of 0.1.
        val old = oldTargets(saved[OLD_SYSTOLIC], saved[OLD_DIASTOLIC], saved[OLD_GLUCOSE])
        return Profile(
            sex = saved[SEX]?.let(Sex::valueOf),
            birthDate = saved[BIRTH_DATE]?.let(LocalDate::parse),
            heightCm = saved[HEIGHT],
            dailyGoal = saved[GOAL] ?: defaults.dailyGoal,
            ownTargetWeightKg = saved[TARGET_WEIGHT],
            targetSystolic = saved[TARGET_SYSTOLIC] ?: old.targetSystolic,
            targetDiastolic = saved[TARGET_DIASTOLIC] ?: old.targetDiastolic,
            targetGlucose = saved[TARGET_GLUCOSE] ?: old.targetGlucose,
            healthTabs = saved[TABS]?.map(HealthKind::valueOf)?.toSet() ?: defaults.healthTabs,
        )
    }

    override fun write(saved: MutablePreferences, value: Profile) {
        saved.put(SEX, value.sex?.name)
        saved.put(BIRTH_DATE, value.birthDate?.toString())
        saved.put(HEIGHT, value.heightCm)
        saved.put(GOAL, value.dailyGoal)
        saved.put(TARGET_WEIGHT, value.ownTargetWeightKg)
        saved.put(TARGET_SYSTOLIC, value.targetSystolic)
        saved.put(TARGET_DIASTOLIC, value.targetDiastolic)
        saved.put(TARGET_GLUCOSE, value.targetGlucose)
        listOf(OLD_SYSTOLIC, OLD_DIASTOLIC).forEach { saved.remove(it) }
        saved.remove(OLD_GLUCOSE)
        saved.put(TABS, value.healthTabs.map { it.name }.toSet())
    }

    private companion object {
        val SEX = stringPreferencesKey("profile_sex")
        val BIRTH_DATE = stringPreferencesKey("profile_birth_date")
        val HEIGHT = intPreferencesKey("profile_height_cm")
        val GOAL = intPreferencesKey("profile_daily_goal")
        val TARGET_WEIGHT = doublePreferencesKey("profile_target_weight_kg")
        val TARGET_SYSTOLIC = intPreferencesKey("profile_own_systolic")
        val TARGET_DIASTOLIC = intPreferencesKey("profile_own_diastolic")
        val TARGET_GLUCOSE = doublePreferencesKey("profile_own_glucose")
        val OLD_SYSTOLIC = intPreferencesKey("profile_target_systolic")
        val OLD_DIASTOLIC = intPreferencesKey("profile_target_diastolic")
        val OLD_GLUCOSE = doublePreferencesKey("profile_target_glucose")
        val TABS = stringSetPreferencesKey("profile_health_tabs")
    }
}

private val OldDefaultPressure = 120 to 80
private const val OLD_DEFAULT_GLUCOSE = 5.5

/**
 * The targets the phone kept in 0.1, where 120/80 and 5.5 were defaults nobody chose: those mean
 * "not set", as migration 0014 made them on the server. A pressure is a pair or nothing.
 */
internal fun oldTargets(systolic: Int?, diastolic: Int?, glucose: Double?): Profile {
    val chosen = systolic != null && diastolic != null && (systolic to diastolic) != OldDefaultPressure
    return Profile(
        targetSystolic = systolic.takeIf { chosen },
        targetDiastolic = diastolic.takeIf { chosen },
        targetGlucose = glucose?.takeIf { it != OLD_DEFAULT_GLUCOSE },
    )
}
