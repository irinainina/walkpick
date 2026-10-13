package com.walkpick.app.data.walk

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** The season outside: it picks the header photo and the accent colours. */
enum class Season { SPRING, SUMMER, AUTUMN, WINTER }

// Snow on the ground or a week of frost is winter; a warm week is summer. Between SUMMER_ENDS_C and
// SUMMER_C a summer goes on and a spring is not one yet, so one cool week does not flip the colours.
private const val SNOW_M = 0.01
private const val FROST_C = 0.0
private const val SUMMER_C = 15.0
private const val SUMMER_ENDS_C = 12.0

/** The season by the weather; the calendar only tells a spring from an autumn. */
internal fun seasonOf(weather: WalkWeather, month: Int, south: Boolean, last: Season?): Season {
    val week = weather.weekCelsius ?: weather.celsius
    return when {
        weather.snowM >= SNOW_M || week < FROST_C -> Season.WINTER
        week >= SUMMER_C || (week >= SUMMER_ENDS_C && last == Season.SUMMER) -> Season.SUMMER
        northernMonth(month, south) <= HALF_YEAR -> Season.SPRING
        else -> Season.AUTUMN
    }
}

/** The season the phone last saw, kept so the colours are there before the weather answers. */
class SeasonStore @Inject constructor(
    private val store: DataStore<Preferences>,
) {
    /** By the calendar of the northern half until the weather was asked once. */
    val now: Flow<Season> = store.data.map { saved ->
        Season.valueOf(saved[SEASON] ?: season(LocalDate.now().monthValue, south = false).uppercase())
    }

    suspend fun see(weather: WalkWeather, south: Boolean) {
        val last = store.data.first()[SEASON]?.let(Season::valueOf)
        val seen = seasonOf(weather, LocalDate.now().monthValue, south, last)
        if (seen != last) store.edit { it[SEASON] = seen.name }
    }

    private companion object {
        val SEASON = stringPreferencesKey("season")
    }
}
