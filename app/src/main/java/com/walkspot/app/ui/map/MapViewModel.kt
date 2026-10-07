package com.walkspot.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.local.FindDao
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.local.WalkPhotos
import com.walkspot.app.data.places.Place
import com.walkspot.app.data.places.PlaceCell
import com.walkspot.app.data.places.PlacesRepository
import com.walkspot.app.data.places.placeCell
import com.walkspot.app.data.settings.DistanceUnit
import com.walkspot.app.data.settings.SettingsStore
import com.walkspot.app.data.walk.LocationFinder
import com.walkspot.app.data.walk.TrackPoint
import com.walkspot.app.data.walk.WalkRepository
import com.walkspot.app.data.walk.routePlaces
import com.walkspot.app.ui.localDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.cos

private const val KEEP_AFTER_LEAVING_MS = 5_000L

/** More routes at once turn the map into a tangle of lines. */
const val ROUTES_ON_MAP = 5

enum class MapTab { Map, Diary }

/** A walk in the diary: what its card shows. */
data class DiaryWalk(val walk: WalkEntity, val photos: Int, val sketch: List<Pair<Float, Float>>)

data class DiaryDay(val day: LocalDate, val walks: List<DiaryWalk>)

data class MapState(
    val tab: MapTab = MapTab.Map,
    val today: LocalDate = LocalDate.now(),
    /** The routes of the latest walks as (latitude, longitude), the latest first. */
    val routes: List<List<Pair<Double, Double>>> = emptyList(),
    val days: List<DiaryDay> = emptyList(),
    /** Set by the "my location" button; [located] grows with every answer, so the map moves again. */
    val me: TrackPoint? = null,
    val located: Int = 0,
    /** Places from Wikipedia around the middle of the map. */
    val places: List<Place> = emptyList(),
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    walks: WalkRepository,
    finds: FindDao,
    settings: SettingsStore,
    private val locations: LocationFinder,
    private val places: PlacesRepository,
) : ViewModel() {

    private val chosen = MutableStateFlow(MapState())
    private var looked: PlaceCell? = null

    // The diary of a year is hundreds of routes to redraw: off the main thread.
    private val diary = combine(walks.watchAll(), finds.watchPhotoCounts()) { walks, photos ->
        val routes = walks.take(ROUTES_ON_MAP).map { routePlaces(it.route) }.filter { it.size > 1 }
        routes to diaryDays(walks, photos, ZoneId.systemDefault())
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<MapState> =
        combine(diary, settings.value, chosen) { (routes, days), units, chosen ->
            chosen.copy(today = LocalDate.now(), routes = routes, days = days, distanceUnit = units.distance)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), MapState())

    fun open(tab: MapTab) = chosen.update { it.copy(tab = tab) }

    /** The map has stopped at this point: shows the places around it. */
    fun lookAround(latitude: Double, longitude: Double) {
        val cell = placeCell(latitude, longitude)
        if (cell == looked) return
        looked = cell
        viewModelScope.launch {
            val found = places.near(cell)
            if (cell == looked) chosen.update { it.copy(places = found) }
        }
    }

    val canLocate: Boolean get() = locations.allowed()

    /** Asks the phone where it is; the location permission must be granted already. */
    fun locate() {
        viewModelScope.launch {
            val found = locations.find() ?: return@launch
            chosen.update { it.copy(me = found, located = it.located + 1) }
        }
    }
}

/** The walks by the day they began on, the latest day and the latest walk of a day first. */
internal fun diaryDays(walks: List<WalkEntity>, photos: List<WalkPhotos>, zone: ZoneId): List<DiaryDay> {
    val count = photos.associate { it.walkId to it.photos }
    return walks
        .sortedByDescending { it.startedAt }
        .groupBy { it.startedAt.localDay(zone) }
        .map { (day, ofDay) ->
            DiaryDay(day, ofDay.map { DiaryWalk(it, count[it.id] ?: 0, routeSketch(routePlaces(it.route))) })
        }
}

/**
 * The route as points of a square 0..1 x 0..1 with the north up, the whole route inside it and its
 * proportions kept: the picture of a walk on its diary card.
 */
internal fun routeSketch(places: List<Pair<Double, Double>>): List<Pair<Float, Float>> {
    if (places.size < 2) return emptyList()
    // A degree of longitude is shorter than a degree of latitude away from the equator.
    val squeeze = cos(Math.toRadians(places.first().first))
    val xs = places.map { it.second * squeeze }
    val ys = places.map { it.first }
    val (left, bottom) = xs.min() to ys.min()
    val side = maxOf(xs.max() - left, ys.max() - bottom).takeIf { it > 0 } ?: return emptyList()
    val (padX, padY) = (side - (xs.max() - left)) / 2 to (side - (ys.max() - bottom)) / 2
    return places.indices.map { index ->
        ((xs[index] - left + padX) / side).toFloat() to (1 - (ys[index] - bottom + padY) / side).toFloat()
    }
}
