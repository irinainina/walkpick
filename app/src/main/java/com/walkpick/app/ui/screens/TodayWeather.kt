package com.walkpick.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walkpick.app.R
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.data.walk.WalkWeather
import com.walkpick.app.data.walk.sky
import com.walkpick.app.ui.theme.AppTheme
import com.walkpick.app.ui.theme.PillShape
import com.walkpick.app.ui.theme.Sizes
import com.walkpick.app.ui.theme.Spacing
import com.walkpick.app.ui.today.DARK_FROM
import com.walkpick.app.ui.today.NIGHT_END
import com.walkpick.app.ui.today.weatherCall
import kotlin.math.roundToInt

private val PillHeight = 32.dp
private const val FAHRENHEIT_PER_C = 1.8
private const val FREEZING_F = 32

/** The weather now in the header of Walk: its sky, its degrees and what it calls for. */
@Composable
internal fun WeatherPill(weather: WalkWeather?, unit: DistanceUnit, hour: Int, walking: Boolean) {
    val colors = AppTheme.colors
    val call = stringResource(weatherCall(weather, hour, walking))
    Row(
        modifier = Modifier
            .padding(horizontal = Spacing.ml)
            .height(PillHeight)
            .background(colors.onAccentOverlay, PillShape)
            .padding(horizontal = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (weather != null) {
            Icon(
                painter = painterResource(weather.iconRes(dark = hour !in NIGHT_END until DARK_FROM)),
                contentDescription = weather.sky(),
                modifier = Modifier.size(Sizes.iconSmall),
                tint = colors.onAccent,
            )
        }
        Text(
            text = weather
                ?.let { stringResource(R.string.today_weather_line, degreesText(it, unit), call) }
                ?: call,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@DrawableRes
private fun WalkWeather.iconRes(dark: Boolean) = when (sky()) {
    "Clear" -> if (dark) R.drawable.ic_weather_night else R.drawable.ic_weather_clear
    "Fog" -> R.drawable.ic_weather_fog
    "Rain" -> R.drawable.ic_weather_rain
    "Snow" -> R.drawable.ic_weather_snow
    "Storm" -> R.drawable.ic_weather_storm
    else -> R.drawable.ic_weather_cloud
}

// Miles go with Fahrenheit: the app has no setting of its own for the temperature.
private fun degreesText(weather: WalkWeather, unit: DistanceUnit): String {
    val fahrenheit = unit == DistanceUnit.MI
    val degrees = if (fahrenheit) weather.celsius * FAHRENHEIT_PER_C + FREEZING_F else weather.celsius
    return "${degrees.roundToInt()}°"
}
