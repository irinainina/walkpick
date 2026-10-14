package com.walkpick.app.data.profile

import com.walkpick.app.data.account.AccountStore
import com.walkpick.app.data.local.HealthKind
import com.walkpick.app.data.sync.RemoteTables
import com.walkpick.app.data.sync.SyncedPart
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import javax.inject.Inject

private const val PROFILES = "profiles"
private const val SETTINGS = "settings"

private val TabColumns = mapOf(
    HealthKind.MOOD to "mood_tab",
    HealthKind.WEIGHT to "weight_tab",
    HealthKind.PRESSURE to "pressure_tab",
    HealthKind.GLUCOSE to "glucose_tab",
)

/** The profile is one row of `profiles` and the tab switches of one row of `settings`. */
class ProfileSync @Inject constructor(
    private val store: ProfileStore,
    private val account: AccountStore,
) : SyncedPart {

    override suspend fun push(remote: RemoteTables) {
        val profile = store.unsynced() ?: return
        val userId = account.lastUserId() ?: return
        remote.upsert(PROFILES, listOf(profile.toProfileRow(userId)), "id", insertOnly = false)
        remote.upsert(SETTINGS, listOf(profile.toSettingsRow(userId)), "user_id", insertOnly = false)
        store.markSynced(profile)
    }

    // What was entered on this phone is not replaced by the server's row.
    override suspend fun pull(remote: RemoteTables) {
        if (store.edited()) return
        val profile = remote.page(PROFILES, "id", 0, 0).firstOrNull()
        val settings = remote.page(SETTINGS, "user_id", 0, 0).firstOrNull()
        store.restore { it.withProfileRow(profile).withSettingsRow(settings) }
    }
}

internal fun Profile.toProfileRow(userId: String) = JsonObject(
    mapOf(
        "id" to JsonPrimitive(userId),
        "sex" to JsonPrimitive(sex?.name?.lowercase()),
        "birth_date" to JsonPrimitive(birthDate?.toString()),
        "height_cm" to JsonPrimitive(heightCm),
        "daily_goal" to JsonPrimitive(dailyGoal),
        "target_weight_kg" to JsonPrimitive(ownTargetWeightKg),
        "target_systolic" to JsonPrimitive(targetSystolic),
        "target_diastolic" to JsonPrimitive(targetDiastolic),
        "target_glucose_mmol" to JsonPrimitive(targetGlucose),
    ),
)

internal fun Profile.toSettingsRow(userId: String): JsonObject {
    val tabs = TabColumns.map { (tab, column) -> column to JsonPrimitive(tab in healthTabs) }
    return JsonObject(mapOf("user_id" to JsonPrimitive(userId)) + tabs)
}

internal fun Profile.withProfileRow(row: JsonObject?): Profile {
    if (row == null) return this
    fun text(column: String) = row[column]?.jsonPrimitive?.contentOrNull
    fun whole(column: String) = row[column]?.jsonPrimitive?.intOrNull
    fun number(column: String) = row[column]?.jsonPrimitive?.doubleOrNull
    return copy(
        sex = text("sex")?.let { Sex.valueOf(it.uppercase()) },
        birthDate = text("birth_date")?.let(LocalDate::parse),
        heightCm = whole("height_cm"),
        dailyGoal = whole("daily_goal") ?: dailyGoal,
        ownTargetWeightKg = number("target_weight_kg"),
        targetSystolic = whole("target_systolic"),
        targetDiastolic = whole("target_diastolic"),
        targetGlucose = number("target_glucose_mmol"),
    )
}

internal fun Profile.withSettingsRow(row: JsonObject?): Profile {
    if (row == null) return this
    val tabs = TabColumns.filterValues { row[it]?.jsonPrimitive?.booleanOrNull == true }.keys
    return if (tabs.isEmpty()) this else copy(healthTabs = tabs)
}
