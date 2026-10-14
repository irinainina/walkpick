package com.walkpick.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkpick.app.data.find.FindStore
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.walk.LocationFinder
import com.walkpick.app.data.walk.TrackPoint
import com.walkpick.app.data.walk.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L

data class PhotoMapState(
    val today: LocalDate = LocalDate.now(),
    /** Every find, for the pins of the map. */
    val finds: List<FindEntity> = emptyList(),
    /** The finds of the latest walk that has any: the map opens on them. */
    val latest: List<FindEntity> = emptyList(),
    val filter: MapFilter = MapFilter.All,
    /** The pin tapped last; the card under the map shows it. */
    val pick: MapPick? = null,
    /** Set by the "my location" button; [located] grows with every answer, so the map moves again. */
    val me: TrackPoint? = null,
    val located: Int = 0,
)

@HiltViewModel
class PhotoMapViewModel @Inject constructor(
    private val walks: WalkRepository,
    private val store: FindStore,
    private val locations: LocationFinder,
) : ViewModel() {

    private val chosen = MutableStateFlow(PhotoMapState())

    val state: StateFlow<PhotoMapState> =
        combine(store.watchAll(), chosen) { finds, chosen ->
            val latest = finds.maxByOrNull { it.takenAt }?.walkId
            chosen.copy(today = LocalDate.now(), finds = finds, latest = finds.filter { it.walkId == latest })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), PhotoMapState())

    init {
        // Before the first photo there is nothing to open the map on: it opens where the phone is.
        viewModelScope.launch { if (store.watchAll().first().isEmpty() && canLocate) locate() }
    }

    fun filter(filter: MapFilter) = chosen.update { it.copy(filter = filter, pick = null) }

    /** Shows the card of a find, or takes the card away with null; its photo and walk come when found. */
    fun pick(find: FindEntity?) {
        val pick = find?.let { MapPick(it) }
        chosen.update { it.copy(pick = pick) }
        if (pick == null) return
        viewModelScope.launch {
            val walk: WalkEntity? = walks.watch(pick.find.walkId).first()
            val photo = try {
                store.photo(pick.find)
            } catch (_: IOException) {
                null
            }
            // The answer may come after another pin was tapped.
            val shown = pick.copy(walk = walk, photo = photo)
            chosen.update { if (it.pick == pick) it.copy(pick = shown) else it }
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
