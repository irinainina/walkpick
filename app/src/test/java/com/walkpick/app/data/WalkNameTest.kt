package com.walkpick.app.data

import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.walk.WalkWeather
import com.walkpick.app.data.walk.parseWeather
import com.walkpick.app.data.walk.season
import com.walkpick.app.data.walk.sky
import com.walkpick.app.data.walk.skyWord
import com.walkpick.app.data.walk.temperatureWord
import com.walkpick.app.data.walk.timeOfDay
import com.walkpick.app.data.walk.title
import com.walkpick.app.data.walk.walkName
import com.walkpick.app.data.walk.weatherEvent
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
    fun whatHappensInTheWeatherHasAWord() {
        assertEquals("Misty", weatherEvent(weather(45)))
        assertEquals("Rainy", weatherEvent(weather(61)))
        assertEquals("Rainy", weatherEvent(weather(80)))
        assertEquals("Snowy", weatherEvent(weather(73)))
        assertEquals("Stormy", weatherEvent(weather(95)))
        assertEquals("Windy", weatherEvent(weather(0, windMs = 11.0)))
        assertNull(weatherEvent(weather(3)))
    }

    @Test
    fun theTemperatureAndTheSkyHaveWords() {
        val words = listOf(-3.0, 5.0, 12.0, 20.0, 27.0).map(::temperatureWord)

        assertEquals(listOf("Frosty", "Cold", "Cool", "Warm", "Hot"), words)
        assertEquals("Sunny", skyWord(weather(1), hour = 9))
        assertEquals("Clear", skyWord(weather(1), hour = 20))
        assertEquals("Cloudy", skyWord(weather(3), hour = 9))
        assertNull(skyWord(weather(61), hour = 9))
    }

    @Test
    fun theSkyHasAWordForEveryKindOfWeather() {
        val codes = listOf(0, 3, 48, 61, 85, 96)

        val words = listOf("Clear", "Cloudy", "Fog", "Rain", "Snow", "Storm")
        assertEquals(words, codes.map { weather(it).sky() })
    }

    // Every minute of a month of evenings: enough starts to meet every name.
    private fun names(weather: WalkWeather?, south: Boolean = false) =
        (0L until 30 * 24 * 60).map { walkName(evening.plusMinutes(it).withHour(18), weather, south) }.toSet()

    @Test
    fun aWalkIsNamedByTwoOfItsWordsInOneOrder() {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val nouns = listOf("Autumn") + days + listOf("Evening", "Walk")
        val order = listOf("Cool", "Cloudy") + nouns

        val names = names(weather(3))

        val pairs = names.map { it.split(" ") }
        assertEquals(true, pairs.all { it.size == 2 && order.indexOf(it[0]) < order.indexOf(it[1]) })
        assertEquals(false, "Cool Cloudy" in names)
        val met = listOf("Cool Evening", "Cloudy Walk", "Autumn Evening", "Evening Walk")
        assertEquals(true, names.containsAll(met))
    }

    @Test
    fun whatHappensInTheWeatherAlwaysComesFirst() {
        assertEquals(setOf("Misty"), names(weather(48)).map { it.substringBefore(" ") }.toSet())
    }

    @Test
    fun withoutTheWeatherAWalkIsNamedByItsDate() {
        val words = names(weather = null, south = true).flatMap { it.split(" ") }.toSet()

        assertEquals(true, "Spring" in words && "Evening" in words && "Walk" in words)
        assertEquals(false, "Autumn" in words || "Cloudy" in words)
    }

    @Test
    fun theSameWalkAlwaysGetsTheSameName() {
        assertEquals(walkName(evening, weather(3)), walkName(evening, weather(3)))
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

        val local = LocalDateTime.of(2026, 10, 5, 18, 48)

        assertEquals(walkName(local.withHour(15), weather = null), walk.title(ZoneOffset.UTC))
        assertEquals(walkName(local, weather = null), walk.title(ZoneOffset.ofHours(3)))
        assertEquals("To the old mill", walk.copy(name = "To the old mill").title(ZoneOffset.UTC))
    }
}
