package com.walkspot.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.walkspot.app.data.account.AccountStore
import com.walkspot.app.data.sync.RemoteTables
import com.walkspot.app.data.sync.SyncedPart
import com.walkspot.app.data.sync.SyncedPreferences
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

/** Reminders and units as the phone keeps them. */
class SettingsStore @Inject constructor(
    store: DataStore<Preferences>,
) : SyncedPreferences<AppSettings>(store, "settings") {

    override fun read(saved: Preferences): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            remindersOn = saved[REMINDERS] ?: defaults.remindersOn,
            reminderTime = saved[TIME]?.let(LocalTime::parse) ?: defaults.reminderTime,
            reminderDays = saved[DAYS]?.map(DayOfWeek::valueOf)?.toSet() ?: defaults.reminderDays,
            distance = saved[DISTANCE]?.let(DistanceUnit::valueOf) ?: defaults.distance,
            weight = saved[WEIGHT]?.let(WeightUnit::valueOf) ?: defaults.weight,
            glucose = saved[GLUCOSE]?.let(GlucoseUnit::valueOf) ?: defaults.glucose,
            walkMood = saved[WALK_MOOD] ?: defaults.walkMood,
        )
    }

    override fun write(saved: MutablePreferences, value: AppSettings) {
        saved[REMINDERS] = value.remindersOn
        saved[TIME] = value.reminderTime.toString()
        saved[DAYS] = value.reminderDays.map { it.name }.toSet()
        saved[DISTANCE] = value.distance.name
        saved[WEIGHT] = value.weight.name
        saved[GLUCOSE] = value.glucose.name
        saved[WALK_MOOD] = value.walkMood
    }

    private companion object {
        val REMINDERS = booleanPreferencesKey("settings_reminders")
        val TIME = stringPreferencesKey("settings_reminder_time")
        val DAYS = stringSetPreferencesKey("settings_reminder_days")
        val DISTANCE = stringPreferencesKey("settings_distance_unit")
        val WEIGHT = stringPreferencesKey("settings_weight_unit")
        val GLUCOSE = stringPreferencesKey("settings_glucose_unit")
        val WALK_MOOD = booleanPreferencesKey("settings_walk_mood")
    }
}

private const val SETTINGS = "settings"

/** Columns of the user's row of `settings`; the tab switches of that row go with the profile. */
class SettingsSync @Inject constructor(
    private val store: SettingsStore,
    private val account: AccountStore,
) : SyncedPart {

    override suspend fun push(remote: RemoteTables) {
        val settings = store.unsynced() ?: return
        val userId = account.lastUserId() ?: return
        remote.upsert(SETTINGS, listOf(settings.toRow(userId)), "user_id", insertOnly = false)
        store.markSynced(settings)
    }

    // What was chosen on this phone is not replaced by the server's row.
    override suspend fun pull(remote: RemoteTables) {
        if (store.edited()) return
        val row = remote.page(SETTINGS, "user_id", 0, 0).firstOrNull() ?: return
        store.restore { it.withRow(row) }
    }
}

// The server spells the glucose units without punctuation.
private val GlucoseNames = mapOf(GlucoseUnit.MMOL to "mmol", GlucoseUnit.MGDL to "mgdl")

internal fun AppSettings.toRow(userId: String) = JsonObject(
    mapOf(
        "user_id" to JsonPrimitive(userId),
        "reminders_enabled" to JsonPrimitive(remindersOn),
        "reminder_time" to JsonPrimitive(reminderTime.toString()),
        "reminder_days" to JsonArray(reminderDays.map { JsonPrimitive(it.value) }.sortedBy { it.int }),
        "distance_unit" to JsonPrimitive(distance.symbol),
        "weight_unit" to JsonPrimitive(weight.symbol),
        "glucose_unit" to JsonPrimitive(GlucoseNames.getValue(glucose)),
    ),
)

internal fun AppSettings.withRow(row: JsonObject): AppSettings {
    fun text(column: String) = row[column]?.jsonPrimitive?.contentOrNull
    val days = row["reminder_days"]?.jsonArray?.map { DayOfWeek.of(it.jsonPrimitive.int) }?.toSet()
    return copy(
        remindersOn = row["reminders_enabled"]?.jsonPrimitive?.booleanOrNull ?: remindersOn,
        reminderTime = text("reminder_time")?.let(LocalTime::parse) ?: reminderTime,
        reminderDays = days?.takeIf { it.isNotEmpty() } ?: reminderDays,
        distance = DistanceUnit.entries.firstOrNull { it.symbol == text("distance_unit") } ?: distance,
        weight = WeightUnit.entries.firstOrNull { it.symbol == text("weight_unit") } ?: weight,
        glucose = GlucoseNames.entries.firstOrNull { it.value == text("glucose_unit") }?.key ?: glucose,
    )
}
