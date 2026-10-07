package com.walkspot.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant
import java.time.LocalDate

// One class is both the Room row and the row sent to Supabase: the serial names are the
// server columns. `synced` stays on the phone; a local write always leaves it false.

@Serializable
@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey @Serializable(LocalDateIso::class) val day: LocalDate,
    val steps: Int,
    @SerialName("distance_m") val distanceM: Int = 0,
    @SerialName("active_minutes") val activeMinutes: Int = 0,
    val goal: Int,
    @SerialName("updated_at") @Serializable(InstantIso::class) val updatedAt: Instant,
    @Transient val synced: Boolean = false,
)

@Serializable
@Entity(tableName = "walks")
data class WalkEntity(
    @PrimaryKey val id: String,
    val number: Int,
    @SerialName("started_at") @Serializable(InstantIso::class) val startedAt: Instant,
    @SerialName("ended_at") @Serializable(InstantIso::class) val endedAt: Instant,
    @SerialName("duration_s") val durationS: Int,
    val steps: Int = 0,
    @SerialName("distance_m") val distanceM: Int = 0,
    val calories: Int = 0,
    // "lon lat,lon lat": the inside of a WKT line
    @Serializable(RouteWkb::class) val route: String? = null,
    val note: String? = null,
    // Null for a walk recorded before walks had names.
    val name: String? = null,
    @SerialName("updated_at") @Serializable(InstantIso::class) val updatedAt: Instant,
    @Transient val synced: Boolean = false,
)

@Serializable
enum class FindKind {
    @SerialName("plant")
    PLANT,

    @SerialName("bird")
    BIRD,

    @SerialName("place")
    PLACE,

    @SerialName("unknown")
    UNKNOWN,
}

@Serializable
data class FindAlternative(
    val name: String,
    @SerialName("latin_name") val latinName: String? = null,
    val match: Float,
)

@Serializable
@Entity(
    tableName = "finds",
    foreignKeys = [
        ForeignKey(
            entity = WalkEntity::class,
            parentColumns = ["id"],
            childColumns = ["walkId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("walkId")],
)
data class FindEntity(
    @PrimaryKey val id: String,
    @SerialName("walk_id") val walkId: String,
    @SerialName("taken_at") @Serializable(InstantIso::class) val takenAt: Instant,
    val latitude: Double,
    val longitude: Double,
    @SerialName("photo_path") val photoPath: String,
    val kind: FindKind = FindKind.UNKNOWN,
    val name: String? = null,
    @SerialName("latin_name") val latinName: String? = null,
    val match: Float? = null,
    val alternatives: List<FindAlternative> = emptyList(),
    val description: String? = null,
    @SerialName("wikipedia_url") val wikipediaUrl: String? = null,
    @SerialName("is_best") val isBest: Boolean = false,
    @SerialName("updated_at") @Serializable(InstantIso::class) val updatedAt: Instant,
    @Transient val synced: Boolean = false,
)

@Serializable
enum class HealthKind {
    @SerialName("mood")
    MOOD,

    @SerialName("weight")
    WEIGHT,

    @SerialName("pressure")
    PRESSURE,

    @SerialName("glucose")
    GLUCOSE,
}

@Serializable
@Entity(tableName = "health_entries", indices = [Index("kind", "measuredAt")])
data class HealthEntryEntity(
    @PrimaryKey val id: String,
    val kind: HealthKind,
    @SerialName("measured_at") @Serializable(InstantIso::class) val measuredAt: Instant,
    // mood 1 (awful) .. 5 (great), weight in kg, systolic, glucose in mmol/L
    val value: Double,
    val diastolic: Int? = null,
    @Transient val synced: Boolean = false,
)

// A row removed on the phone and not yet removed on the server.
@Entity(tableName = "pending_deletes", primaryKeys = ["remoteTable", "id"])
data class PendingDeleteEntity(
    val remoteTable: String,
    val id: String,
)
