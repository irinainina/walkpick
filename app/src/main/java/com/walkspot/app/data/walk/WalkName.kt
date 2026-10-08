package com.walkspot.app.data.walk

import com.walkspot.app.data.local.SyncJson
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.remote.webClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

private const val WEATHER_URL = "https://api.open-meteo.com/v1/forecast"

// The walk is over and its summary waits for the name: a slow answer is not worth the wait.
private const val WEATHER_WAIT_MS = 3_000L

private const val MORNING_FROM = 5
private const val AFTERNOON_FROM = 12
private const val EVENING_FROM = 17
private const val NIGHT_FROM = 22
private const val MONTHS = 12
private const val HALF_YEAR = 6
private const val SEASON_MONTHS = 3

private const val COLD_C = 8.0
private const val WARM_C = 18.0
private const val HOT_C = 25.0
private const val WINDY_MS = 10.0

// The weather codes of the World Meteorological Organization, as Open-Meteo gives them.
private val Clear = 0..1
private val Clouds = 2..3
private val Fog = setOf(45, 48)
private val Rain = (51..67) + (80..82)
private val Snow = (71..77) + (85..86)
private val Storm = 95..99

/** The weather at the place and the time of a walk. */
@Serializable
data class WalkWeather(
    @SerialName("weather_code") val code: Int,
    @SerialName("temperature_2m") val celsius: Double,
    @SerialName("wind_speed_10m") val windMs: Double,
)

@Serializable
private data class WeatherAnswer(val current: WalkWeather)

/** The weather in an answer of Open-Meteo; null when the answer is not one. */
internal fun parseWeather(answer: String): WalkWeather? = try {
    SyncJson.decodeFromString<WeatherAnswer>(answer).current
} catch (_: SerializationException) {
    null
}

internal fun timeOfDay(hour: Int): String = when (hour) {
    in MORNING_FROM until AFTERNOON_FROM -> "Morning"
    in AFTERNOON_FROM until EVENING_FROM -> "Afternoon"
    in EVENING_FROM until NIGHT_FROM -> "Evening"
    else -> "Night"
}

/** The season of a month, 1 to 12; south of the equator the year is half a year ahead. */
internal fun season(month: Int, south: Boolean): String {
    val northern = if (south) (month + HALF_YEAR - 1) % MONTHS + 1 else month
    return listOf("Winter", "Spring", "Summer", "Autumn")[northern % MONTHS / SEASON_MONTHS]
}

/** What the weather of a walk is remembered by; null when nothing happens in it. */
internal fun weatherEvent(weather: WalkWeather): String? = when {
    weather.code in Storm -> "Stormy"
    weather.code in Snow -> "Snowy"
    weather.code in Rain -> "Rainy"
    weather.code in Fog -> "Misty"
    weather.windMs >= WINDY_MS -> "Windy"
    else -> null
}

internal fun temperatureWord(celsius: Double): String = when {
    celsius <= 0 -> "Frosty"
    celsius < COLD_C -> "Cold"
    celsius < WARM_C -> "Cool"
    celsius < HOT_C -> "Warm"
    else -> "Hot"
}

/** The sky in a word; a clear one is sunny only while the sun is up. */
internal fun skyWord(weather: WalkWeather, hour: Int): String? = when (weather.code) {
    in Clear -> if (hour in MORNING_FROM until EVENING_FROM) "Sunny" else "Clear"
    in Clouds -> "Cloudy"
    else -> null
}

/**
 * The name a walk gets by itself: two words out of its weather, season, day of the week, time of
 * day and "Walk", picked by chance - "Sunny Sunday", "Autumn Walk". Rain, snow, mist, a storm or a
 * strong wind always come first: "Rainy Evening". The same walk always gets the same name.
 */
fun walkName(started: LocalDateTime, weather: WalkWeather?, south: Boolean = false): String {
    // In this order any two of the words read as English.
    val nouns = listOf(
        season(started.monthValue, south),
        started.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
        timeOfDay(started.hour),
        "Walk",
    )
    val event = weather?.let(::weatherEvent)
    val firsts = when {
        event != null -> listOf(event)
        weather != null -> listOfNotNull(temperatureWord(weather.celsius), skyWord(weather, started.hour))
        else -> emptyList()
    }
    val plain = if (event == null) nouns.dropLast(1).withIndex().toList() else emptyList()
    val names = firsts.flatMap { first -> nouns.map { "$first $it" } } +
        plain.flatMap { (at, first) -> nouns.drop(at + 1).map { "$first $it" } }
    return names.random(Random(started.toEpochSecond(ZoneOffset.UTC)))
}

/** The name of the walk; one that was never named is called by its date alone. */
fun WalkEntity.title(zone: ZoneId = ZoneId.systemDefault()): String =
    name ?: walkName(startedAt.atZone(zone).toLocalDateTime(), weather = null)

/** The sky of the weather in a word, for the screens: "Cloudy", "Rain". */
fun WalkWeather.sky(): String = when (code) {
    in Clear -> "Clear"
    in Clouds -> "Cloudy"
    in Fog -> "Fog"
    in Rain -> "Rain"
    in Snow -> "Snow"
    in Storm -> "Storm"
    else -> ""
}

/** The weather now at a place, from Open-Meteo: free and without a key. */
class Weather @Inject constructor() {

    /** Null when the service cannot be asked or does not answer. */
    suspend fun at(latitude: Double, longitude: Double): WalkWeather? = try {
        val answer = webClient.get(WEATHER_URL) {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("current", "temperature_2m,weather_code,wind_speed_10m")
            parameter("wind_speed_unit", "ms")
        }
        if (answer.status.isSuccess()) parseWeather(answer.bodyAsText()) else null
    } catch (_: IOException) {
        null
    }
}

/** Names a finished walk; without the network the name does without the weather. */
class WalkNamer @Inject constructor(private val weather: Weather) {

    suspend fun name(track: WalkTrack, zone: ZoneId = ZoneId.systemDefault()): String {
        val place = track.points.lastOrNull() ?: track.finds.lastOrNull()?.let {
            TrackPoint(it.latitude, it.longitude, it.takenAt, 0f)
        }
        val sky = place?.let { withTimeoutOrNull(WEATHER_WAIT_MS) { weather.at(it.latitude, it.longitude) } }
        val started = track.startedAt.atZone(zone).toLocalDateTime()
        return walkName(started, sky, south = (place?.latitude ?: 0.0) < 0)
    }
}
