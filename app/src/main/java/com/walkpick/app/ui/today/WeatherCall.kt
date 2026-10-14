package com.walkpick.app.ui.today

import androidx.annotation.StringRes
import com.walkpick.app.R
import com.walkpick.app.data.walk.EVENING_FROM
import com.walkpick.app.data.walk.Fog
import com.walkpick.app.data.walk.Rain
import com.walkpick.app.data.walk.Snow
import com.walkpick.app.data.walk.Storm
import com.walkpick.app.data.walk.WINDY_MS
import com.walkpick.app.data.walk.WalkWeather

// Thresholds of the author's phrases, 2026-10-11; called preliminary there.
private const val HEAT_C = 28.0
private const val FROST_C = -10.0
internal const val NIGHT_END = 5
internal const val DARK_FROM = 21

private const val PARTLY_CLOUDY = 2
private const val OVERCAST = 3
private val Drizzle = 51..57
private val Downpour = setOf(65, 67, 82)

/**
 * The line after the temperature in the header of Walk. In a storm or a downpour it says to wait,
 * otherwise it calls out; the evening line is only for a clear sky.
 */
@StringRes
fun weatherCall(weather: WalkWeather?, hour: Int, walking: Boolean): Int = when {
    walking -> R.string.call_walking
    weather == null -> R.string.call_no_weather
    weather.code in Storm -> R.string.call_storm
    weather.code in Downpour -> R.string.call_downpour
    hour !in NIGHT_END until DARK_FROM -> R.string.call_night
    weather.celsius >= HEAT_C -> R.string.call_heat
    weather.celsius < FROST_C -> R.string.call_frost
    weather.windMs >= WINDY_MS -> R.string.call_wind
    weather.code in Snow -> R.string.call_snow
    weather.code in Drizzle -> R.string.call_drizzle
    weather.code in Rain -> R.string.call_rain
    weather.code in Fog -> R.string.call_fog
    weather.code == OVERCAST -> R.string.call_overcast
    weather.code == PARTLY_CLOUDY -> R.string.call_partly_cloudy
    hour >= EVENING_FROM -> R.string.call_evening
    else -> R.string.call_clear
}
