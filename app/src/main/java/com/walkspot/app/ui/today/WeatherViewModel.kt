package com.walkspot.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.walk.LocationFinder
import com.walkspot.app.data.walk.SeasonStore
import com.walkspot.app.data.walk.WalkWeather
import com.walkspot.app.data.walk.Weather
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

// The weather outside does not change faster than this.
private val KeepFor = Duration.ofMinutes(30)

/** The weather where the phone is, for the walker deciding whether to go out. */
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val locations: LocationFinder,
    private val weather: Weather,
    private val seasons: SeasonStore,
) : ViewModel() {

    private val current = MutableStateFlow<WalkWeather?>(null)
    private var asked: Instant? = null

    /** Null until it is known; it stays null without the location permission or the network. */
    val now: StateFlow<WalkWeather?> = current

    /** Asks again when the last answer has grown old. */
    fun refresh() {
        val last = asked
        if (last != null && Duration.between(last, Instant.now()) < KeepFor) return
        asked = Instant.now()
        viewModelScope.launch {
            val place = locations.find()
            val answer = place?.let { weather.at(it.latitude, it.longitude) }
            if (answer == null) {
                asked = null
            } else {
                current.value = answer
                seasons.see(answer, south = place.latitude < 0)
            }
        }
    }
}
