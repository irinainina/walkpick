package com.walkspot.app.ui.find

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.places.Wikipedia
import com.walkspot.app.data.settings.DistanceUnit
import com.walkspot.app.data.settings.SettingsStore
import com.walkspot.app.data.walk.LocationFinder
import com.walkspot.app.data.walk.TrackPoint
import com.walkspot.app.data.walk.distanceM
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

/** A place from Wikipedia the walker has not been to; what is not known yet stays null. */
data class PlaceCardState(
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val url: String? = null,
    val photo: File? = null,
    val me: TrackPoint? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
) {
    /** How far the place is from the phone, in metres. */
    val away: Int?
        get() = me?.let { distanceM(it, TrackPoint(latitude, longitude, Instant.EPOCH, 0f)).toInt() }
}

@HiltViewModel
class PlaceCardViewModel @Inject constructor(
    saved: SavedStateHandle,
    wikipedia: Wikipedia,
    locations: LocationFinder,
    settings: SettingsStore,
) : ViewModel() {

    private val current = MutableStateFlow(
        PlaceCardState(
            title = checkNotNull(saved[TITLE]),
            latitude = checkNotNull(saved.get<String>(LATITUDE)).toDouble(),
            longitude = checkNotNull(saved.get<String>(LONGITUDE)).toDouble(),
        ),
    )

    val state: StateFlow<PlaceCardState> = current

    init {
        // The three answers come when they come; the card shows each as it arrives.
        viewModelScope.launch {
            val unit = settings.value.first().distance
            current.update { it.copy(distanceUnit = unit) }
        }
        viewModelScope.launch {
            val me = locations.find()
            current.update { it.copy(me = me) }
        }
        viewModelScope.launch {
            try {
                val article = wikipedia.summary(current.value.title) ?: return@launch
                current.update { it.copy(description = article.extract, url = article.url) }
                val photo = wikipedia.picture(article)
                current.update { it.copy(photo = photo) }
            } catch (_: IOException) {
                // Offline the card keeps the name and the map.
            }
        }
    }

    companion object {
        const val TITLE = "title"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
    }
}
