package com.walkspot.app.data.walk

import com.walkspot.app.data.local.InstantIso
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A place the phone reported during a walk; [accuracyM] is the radius the phone vouches for. */
@Serializable
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    @Serializable(InstantIso::class) val at: Instant,
    val accuracyM: Float,
)

private const val EARTH_RADIUS_M = 6_371_000.0
private const val MILLIS_IN_SECOND = 1000.0
private const val SECONDS_IN_HOUR = 3600.0
private const val METRES_IN_KM = 1000.0

// A reading vaguer than this is noise; a shorter move is the signal trembling over somebody standing;
// a faster one is the signal jumping, not a person walking.
private const val WORST_ACCURACY_M = 30f
private const val SHORTEST_MOVE_M = 5.0
private const val FASTEST_MPS = 7.0

// A pace over the first metres jumps about; the weight of somebody who gave none.
private const val PACE_FROM_M = 50.0
private const val USUAL_WEIGHT_KG = 70.0

// What walking costs, in MET (kcal per kg per hour), by speed in km/h: the 2011 Compendium of
// Physical Activities, codes 17151-17230.
private val WalkingMet = listOf(3.2 to 2.8, 4.8 to 3.5, 6.4 to 4.3)
private const val FAST_WALK_MET = 7.0

/** Metres between two points over the globe. */
fun distanceM(from: TrackPoint, to: TrackPoint): Double {
    val lat = Math.toRadians(to.latitude - from.latitude)
    val lon = Math.toRadians(to.longitude - from.longitude)
    val a = sin(lat / 2).pow(2) +
        cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) * sin(lon / 2).pow(2)
    return 2 * EARTH_RADIUS_M * asin(sqrt(a))
}

/** Whether [next] continues the route after [last]; the first point only has to be accurate. */
fun continuesRoute(last: TrackPoint?, next: TrackPoint): Boolean = dropReason(last, next) == null

/** Why [next] does not continue the route after [last], for the log; null when it does. */
fun dropReason(last: TrackPoint?, next: TrackPoint): String? {
    if (next.accuracyM > WORST_ACCURACY_M) return "accuracy ${next.accuracyM} m"
    if (last == null) return null
    val seconds = Duration.between(last.at, next.at).toMillis() / MILLIS_IN_SECOND
    if (seconds <= 0) return "not later than the last point"
    val metres = distanceM(last, next)
    return when {
        metres < max(SHORTEST_MOVE_M, next.accuracyM / 2.0) ->
            "moved ${metres.toInt()} m, accuracy ${next.accuracyM} m"
        metres / seconds > FASTEST_MPS -> "${metres.toInt()} m in $seconds s"
        else -> null
    }
}

fun routeLengthM(points: List<TrackPoint>): Double = points.zipWithNext(::distanceM).sum()

/** Seconds a kilometre takes, or null until the walk is long enough to have a pace. */
fun paceSecondsPerKm(metres: Double, movingSeconds: Long): Int? =
    if (metres < PACE_FROM_M) null else (movingSeconds / (metres / METRES_IN_KM)).roundToInt()

/** Kilocalories of a walk by its average speed; pauses are not in [movingSeconds]. */
fun walkCalories(metres: Double, movingSeconds: Long, weightKg: Double?): Int {
    // Standing with the walk switched on burns nothing worth counting.
    if (movingSeconds <= 0 || metres <= 0) return 0
    val hours = movingSeconds / SECONDS_IN_HOUR
    val speed = metres / METRES_IN_KM / hours
    val met = WalkingMet.firstOrNull { (below, _) -> speed < below }?.second ?: FAST_WALK_MET
    return (met * (weightKg ?: USUAL_WEIGHT_KG) * hours).roundToInt()
}

/** A kept route back as (latitude, longitude) pairs; whatever cannot be read is left out. */
fun routePlaces(route: String?): List<Pair<Double, Double>> = route.orEmpty().split(",").mapNotNull { place ->
    val numbers = place.trim().split(" ").mapNotNull(String::toDoubleOrNull)
    if (numbers.size == 2) numbers[1] to numbers[0] else null
}

/** The route as the walks table keeps it: "lon lat,lon lat"; null when there is no line to draw. */
fun routeText(points: List<TrackPoint>): String? {
    if (points.size < 2) return null
    return points.joinToString(",") { String.format(Locale.US, "%.6f %.6f", it.longitude, it.latitude) }
}
