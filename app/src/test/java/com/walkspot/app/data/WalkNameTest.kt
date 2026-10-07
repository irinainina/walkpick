package com.walkspot.app.data

import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.walk.WalkWeather
import com.walkspot.app.data.walk.parseWeather
import com.walkspot.app.data.walk.season
import com.walkspot.app.data.walk.sky
import com.walkspot.app.data.walk.timeOfDay
import com.walkspot.app.data.walk.title
import com.walkspot.app.data.walk.walkName
import com.walkspot.app.data.walk.weatherWord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class WalkNameTest {

    private val evening = LocalDateTime.of(2026, 10, 7, 18, 30)

    private fun weather(code: Int, celsius: Double = 12.0, windMs: Double = 2.0) =
        WalkWeather(code, celsius, windMs)

    @Test
    fun theDayIsCutIntoMorningAfternoonEveningAndNight() {
        assertEquals("Night", timeOfDay(4))
        assertEquals("Morning", timeOfDay(5))
        assertEquals("Afternoon", timeOfDay(12))
        assertEquals("Evening", timeOfDay(17))
        assertEquals("Night", timeOfDay(22))
    }

    @Test
    fun theSeasonFollowsTheMonthAndTurnsOverSouthOfTheEquator() {
        assertEquals("Winter", season(12, south = false))
        assertEquals("Winter", season(2, south = false))
        assertEquals("Spring", season(3, south = false))
        assertEquals("Summer", season(8, south = false))
        assertEquals("Autumn", season(10, south = false))
        assertEquals("Spring", season(10, south = true))
        assertEquals("Summer", season(1, south = true))
    }

    @Test
    fun theWeatherGivesOneWordOrNone() {
        assertEquals("Misty", weatherWord(weather(45)))
        assertEquals("Rainy", weatherWord(weather(61)))
        assertEquals("Rainy", weatherWord(weather(80)))
        assertEquals("Snowy", weatherWord(weather(73)))
        assertEquals("Stormy", weatherWord(weather(95)))
        assertEquals("Frosty", weatherWord(weather(0, celsius = -3.0)))
        assertEquals("Sunny", weatherWord(weather(1, celsius = 27.0)))
        assertEquals("Cloudy", weatherWord(weather(3)))
        assertEquals("Windy", weatherWord(weather(0, windMs = 11.0)))
        assertNull(weatherWord(weather(0)))
    }

    @Test
    fun theSkyHasAWordForEveryKindOfWeather() {
        val codes = listOf(0, 3, 48, 61, 85, 96)

        val words = listOf("Clear", "Cloudy", "Fog", "Rain", "Snow", "Storm")
        assertEquals(words, codes.map { weather(it).sky() })
    }

    @Test
    fun aWalkIsNamedByItsWeatherOrItsSeasonAndItsTimeOfDay() {
        assertEquals("Misty Evening Walk", walkName(evening, weather(48)))
        assertEquals("Autumn Evening Walk", walkName(evening, weather(0)))
        assertEquals("Autumn Evening Walk", walkName(evening, weather = null))
        assertEquals("Spring Morning Walk", walkName(evening.withHour(8), null, south = true))
    }

    @Test
    fun theAnswerOfTheWeatherServiceIsRead() {
        val answer = """{"latitude":50.5,"current_units":{"temperature_2m":"°C"},
            "current":{"temperature_2m":9.6,"weather_code":3,"wind_speed_10m":1.06}}"""

        assertEquals(WalkWeather(3, 9.6, 1.06), parseWeather(answer))
        assertNull(parseWeather("<html>busy</html>"))
    }

    @Test
    fun aWalkWithoutANameIsCalledByItsStartAndANamedOneByItsName() {
        val start = Instant.parse("2026-10-05T15:48:00Z")
        val walk = WalkEntity("walk", 337, start, start.plusSeconds(3240), 3240, updatedAt = start)

        assertEquals("Autumn Afternoon Walk", walk.title(ZoneOffset.UTC))
        assertEquals("Autumn Evening Walk", walk.title(ZoneOffset.ofHours(3)))
        assertEquals("To the old mill", walk.copy(name = "To the old mill").title(ZoneOffset.UTC))
    }
}
