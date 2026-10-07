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
import javax.inject.Inject

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

private const val HOT_C = 25.0
private const val SCORCHING_C = 30.0
private const val BITTER_C = -10.0
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

/** The one word a walk takes from its weather; null when the weather is nothing to speak of. */
internal fun weatherWord(weather: WalkWeather): String? = when {
    weather.code in Fog -> "Misty"
    weather.code in Rain -> "Rainy"
    weather.code in Snow -> "Snowy"
    weather.code in Storm -> "Stormy"
    weather.code in Clear && weather.celsius <= 0 -> "Frosty"
    weather.code in Clear && weather.celsius >= HOT_C -> "Sunny"
    weather.code in Clouds -> "Cloudy"
    weather.windMs >= WINDY_MS -> "Windy"
    weather.celsius <= BITTER_C -> "Frosty"
    weather.celsius <= 0 -> "Icy"
    weather.celsius >= SCORCHING_C -> "Scorching"
    weather.celsius >= HOT_C -> "Sunny"
    else -> null
}

/**
 * The name a walk gets by itself: its weather, or its season when the weather says nothing, and its
 * time of day - "Misty Evening Walk", "Autumn Morning Walk".
 */
fun walkName(started: LocalDateTime, weather: WalkWeather?, south: Boolean = false): String {
    val first = weather?.let(::weatherWord) ?: season(started.monthValue, south)
    return "$first ${timeOfDay(started.hour)} Walk"
}

/** The name of the walk; one that was never named is called by its season and time of day. */
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
