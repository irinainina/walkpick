package com.walkspot.app.debug

import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.data.local.HealthEntryEntity
import com.walkspot.app.data.local.HealthKind
import com.walkspot.app.data.local.WalkEntity
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Made-up history for looking at the screens: steps, walks, finds and health entries from June 2025.
 * Everything here can be told from real data and removed: ids start with 5eed, days are before [SEED_END].
 */
class SeedData(
    val days: List<DailyStepsEntity>,
    val walks: List<WalkEntity>,
    val finds: List<FindEntity>,
    val health: List<HealthEntryEntity>,
)

val SEED_START: LocalDate = LocalDate.of(2025, 6, 1)

// The day the app began to count real steps; seeded days stop before it.
val SEED_END: LocalDate = LocalDate.of(2026, 10, 6)

private const val WALKS = 1
private const val FINDS = 2
private const val HEALTH = 3
private const val STRIDE_M = 0.69
private const val STEPS_PER_MINUTE = 40
private const val WALK_STEPS_PER_MINUTE = 105
private const val HOME_LAT = 53.9045
private const val HOME_LON = 27.5615
private const val ROUTE_POINTS = 12
private const val TURN = 2 * Math.PI

private val known = listOf(
    Triple(FindKind.PLANT, "Common lilac", "Syringa vulgaris"),
    Triple(FindKind.PLANT, "Norway maple", "Acer platanoides"),
    Triple(FindKind.PLANT, "Dandelion", "Taraxacum officinale"),
    Triple(FindKind.PLANT, "Horse chestnut", "Aesculus hippocastanum"),
    Triple(FindKind.BIRD, "Great tit", "Parus major"),
    Triple(FindKind.BIRD, "Mallard", "Anas platyrhynchos"),
    Triple(FindKind.BIRD, "Hooded crow", "Corvus cornix"),
    Triple(FindKind.PLACE, "Old water tower", null),
    Triple(FindKind.PLACE, "City park fountain", null),
    Triple(FindKind.PLACE, "Stone bridge", null),
)

private fun id(kind: Int, number: Int) =
    String.format(Locale.ROOT, "5eed%04d-0000-4000-8000-%012d", kind, number)

@Suppress("MagicNumber", "LongMethod")
fun seedData(zone: ZoneId = ZoneId.systemDefault()): SeedData {
    val random = Random(312)
    val days = mutableListOf<DailyStepsEntity>()
    val walks = mutableListOf<WalkEntity>()
    val finds = mutableListOf<FindEntity>()
    val health = mutableListOf<HealthEntryEntity>()
    val total = (SEED_END.toEpochDay() - SEED_START.toEpochDay()).toInt()
    var day = SEED_START
    repeat(total) { index ->
        val stamp = day.plusDays(1).atStartOfDay(zone).toInstant()
        // The last five days all have a walk, so that Today opens on a streak.
        val lastDays = index >= total - 5
        val walked = lastDays || random.nextInt(100) < 58
        val base = if (day.dayOfWeek.value >= 6) 9500 else 6800
        var steps = (base + random.nextInt(-3200, 3400) + index * 2).coerceAtLeast(900)
        repeat(if (walked && random.nextInt(100) < 12) 2 else if (walked) 1 else 0) { nth ->
            val minutes = random.nextInt(22, 75)
            val start = day.atTime(if (nth == 0) 18 else 8, random.nextInt(0, 50)).atZone(zone).toInstant()
            val walkSteps = minutes * WALK_STEPS_PER_MINUTE + random.nextInt(-200, 200)
            val route = route(random, walkSteps * STRIDE_M)
            val walk = WalkEntity(
                id = id(WALKS, walks.size + 1),
                number = walks.size + 1,
                startedAt = start,
                endedAt = start.plusSeconds(minutes * 60L),
                durationS = minutes * 60,
                steps = walkSteps,
                distanceM = (walkSteps * STRIDE_M).roundToInt(),
                calories = (walkSteps * 0.04).roundToInt(),
                route = route.joinToString(",") { (lat, lon) -> "$lon $lat" },
                updatedAt = stamp,
            )
            walks += walk
            steps = maxOf(steps, walkSteps + 1500)
            repeat(random.nextInt(0, 4)) {
                val (lat, lon) = route[random.nextInt(route.size)]
                val what = known[random.nextInt(known.size)].takeIf { random.nextInt(100) < 80 }
                finds += FindEntity(
                    id = id(FINDS, finds.size + 1),
                    walkId = walk.id,
                    takenAt = start.plusSeconds(random.nextLong(60, minutes * 60L)),
                    latitude = lat,
                    longitude = lon,
                    photoPath = "seed/${finds.size + 1}.jpg",
                    kind = what?.first ?: FindKind.UNKNOWN,
                    name = what?.second,
                    latinName = what?.third,
                    match = what?.let { random.nextInt(62, 99) / 100f },
                    isBest = random.nextInt(100) < 15,
                    updatedAt = stamp,
                )
            }
        }
        days += DailyStepsEntity(
            day = day,
            steps = steps,
            distanceM = (steps * STRIDE_M).roundToInt(),
            activeMinutes = steps / STEPS_PER_MINUTE,
            goal = 10000,
            updatedAt = stamp,
        )
        val morning = day.atTime(8, 30).atZone(zone).toInstant()
        fun entry(kind: HealthKind, value: Double, diastolic: Int? = null) {
            health += HealthEntryEntity(id(HEALTH, health.size + 1), kind, morning, value, diastolic)
        }
        val mood = if (walked) random.nextInt(3, 6) else random.nextInt(2, 5)
        if (random.nextInt(100) < 70) entry(HealthKind.MOOD, mood.toDouble())
        if (index % 3 == 0) {
            val trend = 66.4 - 2.2 * index / total
            entry(HealthKind.WEIGHT, ((trend + random.nextDouble(-0.4, 0.4)) * 10).roundToInt() / 10.0)
        }
        val systolic = random.nextInt(114, 131).toDouble()
        if (index % 4 == 1) entry(HealthKind.PRESSURE, systolic, random.nextInt(72, 86))
        if (index % 7 == 2) entry(HealthKind.GLUCOSE, random.nextInt(48, 61) / 10.0)
        day = day.plusDays(1)
    }
    return SeedData(days, walks, finds, health)
}

// A loop around home about [metres] long: walks usually end where they began.
private fun route(random: Random, metres: Double): List<Pair<Double, Double>> {
    val radiusDeg = metres / TURN / 111_000
    val centreAngle = random.nextDouble(TURN)
    val centreLat = HOME_LAT + radiusDeg * sin(centreAngle)
    val centreLon = HOME_LON + radiusDeg * cos(centreAngle) / cos(Math.toRadians(HOME_LAT))
    return List(ROUTE_POINTS + 1) { point ->
        val angle = centreAngle + Math.PI + TURN * point / ROUTE_POINTS
        val wobble = if (point % ROUTE_POINTS == 0) 1.0 else random.nextDouble(0.85, 1.15)
        val lat = centreLat + radiusDeg * wobble * sin(angle)
        val lon = centreLon + radiusDeg * wobble * cos(angle) / cos(Math.toRadians(HOME_LAT))
        (lat * 1e6).roundToInt() / 1e6 to (lon * 1e6).roundToInt() / 1e6
    }
}
