package com.walkspot.app.data

import com.walkspot.app.data.walk.Season
import com.walkspot.app.data.walk.WalkWeather
import com.walkspot.app.data.walk.parseWeather
import com.walkspot.app.data.walk.seasonOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SeasonTest {

    private fun season(
        week: Double,
        month: Int,
        snowM: Double = 0.0,
        south: Boolean = false,
        last: Season? = null,
    ) = seasonOf(WalkWeather(3, 20.0, 2.0, snowM, week), month, south, last)

    @Test
    fun snowOnTheGroundOrAWeekOfFrostIsWinter() {
        assertEquals(Season.WINTER, season(week = 3.0, month = 11, snowM = 0.05))
        assertEquals(Season.WINTER, season(week = -2.0, month = 3))
    }

    @Test
    fun withoutSnowTheFirstHalfOfTheYearIsSpringAndTheSecondAutumn() {
        assertEquals(Season.SPRING, season(week = 2.0, month = 1))
        assertEquals(Season.AUTUMN, season(week = 2.0, month = 12))
        assertEquals(Season.AUTUMN, season(week = 9.0, month = 4, south = true))
    }

    @Test
    fun aWarmWeekIsSummerAndOneCoolWeekDoesNotEndIt() {
        assertEquals(Season.SUMMER, season(week = 16.0, month = 5))
        assertEquals(Season.SUMMER, season(week = 13.0, month = 8, last = Season.SUMMER))
        assertEquals(Season.SPRING, season(week = 13.0, month = 5, last = Season.SPRING))
        assertEquals(Season.AUTUMN, season(week = 11.0, month = 9, last = Season.SUMMER))
    }

    @Test
    fun withoutTheWeekTheHourTellsTheSeason() {
        assertEquals(Season.SUMMER, seasonOf(WalkWeather(0, 22.0, 1.0), 7, south = false, last = null))
    }

    @Test
    fun theSnowAndTheWeekAreReadFromTheAnswer() {
        val answer = """{"current":{"temperature_2m":1.5,"weather_code":71,
            "wind_speed_10m":3.0,"snow_depth":0.04},
            "daily":{"temperature_2m_mean":[2.0,null,4.0]}}"""

        assertEquals(WalkWeather(71, 1.5, 3.0, snowM = 0.04, weekCelsius = 3.0), parseWeather(answer))
    }
}
