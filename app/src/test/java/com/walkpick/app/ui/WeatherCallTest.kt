package com.walkpick.app.ui

import com.walkpick.app.R
import com.walkpick.app.data.walk.WalkWeather
import com.walkpick.app.ui.today.weatherCall
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeatherCallTest {

    private fun call(code: Int, celsius: Double = 12.0, windMs: Double = 2.0, hour: Int = 10) =
        weatherCall(WalkWeather(code, celsius, windMs), hour, walking = false)

    @Test
    fun eachSkyHasItsLine() {
        assertEquals(R.string.call_clear, call(0))
        assertEquals(R.string.call_partly_cloudy, call(2))
        assertEquals(R.string.call_overcast, call(3))
        assertEquals(R.string.call_fog, call(45))
        assertEquals(R.string.call_drizzle, call(53))
        assertEquals(R.string.call_rain, call(63))
        assertEquals(R.string.call_rain, call(80))
        assertEquals(R.string.call_downpour, call(82))
        assertEquals(R.string.call_snow, call(73))
        assertEquals(R.string.call_storm, call(95))
    }

    @Test
    fun aStormAndADownpourComeBeforeHeatFrostAndWind() {
        assertEquals(R.string.call_storm, call(95, celsius = 30.0, windMs = 15.0))
        assertEquals(R.string.call_downpour, call(65, celsius = -12.0))
    }

    @Test
    fun heatAndFrostComeBeforeWindAndWindBeforeRainAndClouds() {
        assertEquals(R.string.call_heat, call(0, celsius = 28.0, windMs = 12.0))
        assertEquals(R.string.call_frost, call(73, celsius = -11.0))
        assertEquals(R.string.call_wind, call(63, windMs = 10.0))
        assertEquals(R.string.call_wind, call(3, windMs = 11.0))
    }

    @Test
    fun theEveningLineIsOnlyForAClearSkyAndTheNightHasItsOwn() {
        assertEquals(R.string.call_evening, call(0, hour = 18))
        assertEquals(R.string.call_overcast, call(3, hour = 18))
        assertEquals(R.string.call_night, call(0, hour = 21))
        assertEquals(R.string.call_night, call(63, hour = 4))
        assertEquals(R.string.call_storm, call(95, hour = 23))
    }

    @Test
    fun aWalkUnderWayAndNoWeatherHaveTheirLines() {
        assertEquals(R.string.call_walking, weatherCall(WalkWeather(95, 12.0, 2.0), 10, walking = true))
        assertEquals(R.string.call_no_weather, weatherCall(null, 10, walking = false))
    }
}
