package com.walkspot.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.find.FindStore
import com.walkspot.app.data.local.FindDao
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.places.Place
import com.walkspot.app.data.places.PlaceCell
import com.walkspot.app.data.places.PlacesRepository
import com.walkspot.app.data.places.Wikipedia
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.cos

private const val KEEP_AFTER_LEAVING_MS = 5_000L

/** More routes at once turn the map into a tangle of lines. */
const val ROUTES_ON_MAP = 5

enum class MapTab { Map, Diary }

/** A walk in the diary: what its card shows. */
data class DiaryWalk(
    val walk: WalkEntity,
    val photos: Int,
    val sketch: List<Pair<Float, Float>>,
    /** The finds of the walk in the order they were taken, for the thumbnails of its card. */
    val finds: List<FindEntity> = emptyList(),
)

data class DiaryDay(val day: LocalDate, val walks: List<DiaryWalk>)

data class MapState(
    val tab: MapTab = MapTab.Map,
    val today: LocalDate = LocalDate.now(),
    /** The walks have been read: until then an empty diary is not yet an empty one. */
    val loaded: Boolean = false,
    /** The routes of the latest walks as (latitude, longitude), the latest first. */
    val routes: List<List<Pair<Double, Double>>> = emptyList(),
    val days: List<DiaryDay> = emptyList(),
    /** Set by the "my location" button; [located] grows with every answer, so the map moves again. */
    val me: TrackPoint? = null,
    val located: Int = 0,
    /** Places from Wikipedia around the middle of the map. */
    val places: List<Place> = emptyList(),
    /** Every find, for the pins of the map. */
    val finds: List<FindEntity> = emptyList(),
    val filter: MapFilter = MapFilter.All,
    /** The pin tapped last; the card under the map shows it. */
    val pick: MapPick? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    walks: WalkRepository,
    finds: FindDao,
    settings: SettingsStore,
    private val locations: LocationFinder,
    private val places: PlacesRepository,
    private val store: FindStore,
    private val wikipedia: Wikipedia,
) : ViewModel() {

    private val chosen = MutableStateFlow(MapState())
    private var looked: PlaceCell? = null

    // The diary of a year is hundreds of routes to redraw: off the main thread.
    private val diary = combine(walks.watchAll(), finds.watchAll()) { walks, finds ->
        val routes = walks.take(ROUTES_ON_MAP).map { routePlaces(it.route) }.filter { it.size > 1 }
        routes to diaryDays(walks, finds, ZoneId.systemDefault())
    }.flowOn(Dispatchers.Default)

    // A photo is looked for once in the life of the screen, found or not.
    private val photos = mutableMapOf<String, File?>()

    suspend fun photo(find: FindEntity): File? =
        if (find.id in photos) photos[find.id] else store.photo(find).also { photos[find.id] = it }

    val state: StateFlow<MapState> =
        combine(diary, settings.value, chosen, finds.watchAll()) { (routes, days), units, chosen, finds ->
            chosen.copy(
                today = LocalDate.now(),
                loaded = true,
                routes = routes,
                days = days,
                finds = finds,
                distanceUnit = units.distance,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), MapState())

    init {
        // Before the first walk there is no route to open the map on: it opens where the phone is.
        viewModelScope.launch { if (diary.first().first.isEmpty() && canLocate) locate() }
    }

    fun open(tab: MapTab) = chosen.update { it.copy(tab = tab) }

    fun filter(filter: MapFilter) = chosen.update { it.copy(filter = filter, pick = null) }

    /** Shows the card of a pin, or takes the card away with null; its photo and text come when found. */
    fun pick(pick: MapPick?) {
        chosen.update { it.copy(pick = pick) }
        if (pick == null) return
        viewModelScope.launch {
            val shown = try {
                pick.find?.let { pick.copy(photo = store.photo(it)) } ?: described(pick)
            } catch (_: IOException) {
                pick
            }
            // The answer may come after another pin was tapped.
            chosen.update { if (it.pick == pick) it.copy(pick = shown) else it }
        }
    }

    // A place of Wikipedia brings the first lines of its article and its picture.
    private suspend fun described(pick: MapPick): MapPick {
        val article = pick.title?.let { wikipedia.summary(it) } ?: return pick
        return pick.copy(text = article.extract, photo = wikipedia.picture(article))
    }

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
internal fun diaryDays(walks: List<WalkEntity>, finds: List<FindEntity>, zone: ZoneId): List<DiaryDay> {
    val byWalk = finds.groupBy { it.walkId }
    return walks
        .sortedByDescending { it.startedAt }
        .groupBy { it.startedAt.localDay(zone) }
        .map { (day, ofDay) ->
            val cards = ofDay.map { walk ->
                val taken = byWalk[walk.id].orEmpty().sortedBy { it.takenAt }
                DiaryWalk(walk, taken.size, routeSketch(routePlaces(walk.route)), taken)
            }
            DiaryDay(day, cards)
        }
}

/**
 * The route as shares 0..1 of the width and the height of a tile [aspect] times wider than high, with
 * the north up, the whole route inside it and its proportions kept: the picture of a walk on a card.
 */
internal fun routeSketch(places: List<Pair<Double, Double>>, aspect: Float = 1f): List<Pair<Float, Float>> {
    if (places.size < 2) return emptyList()
    // A degree of longitude is shorter than a degree of latitude away from the equator.
    val squeeze = cos(Math.toRadians(places.first().first))
    val xs = places.map { it.second * squeeze }
    val ys = places.map { it.first }
    val (left, bottom) = xs.min() to ys.min()
    val (wide, high) = xs.max() - left to ys.max() - bottom
    // The tile in the units of the route: as high as the route, or higher when the route is too wide.
    val tileHigh = maxOf(high, wide / aspect).takeIf { it > 0 } ?: return emptyList()
    val tileWide = tileHigh * aspect
    val (padX, padY) = (tileWide - wide) / 2 to (tileHigh - high) / 2
    return places.indices.map { index ->
        val x = (xs[index] - left + padX) / tileWide
        val y = 1 - (ys[index] - bottom + padY) / tileHigh
        x.toFloat() to y.toFloat()
    }
}
