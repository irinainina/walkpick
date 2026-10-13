package com.walkpick.app.ui.walk

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkpick.app.data.find.FindStore
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.WalkEntity
import com.walkpick.app.data.settings.DistanceUnit
import com.walkpick.app.data.settings.SettingsStore
import com.walkpick.app.data.walk.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L

data class WalkSummaryState(
    val walk: WalkEntity,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val finds: List<FindEntity> = emptyList(),
    /** The photos of the finds by the ids of the finds, as far as they are on the phone. */
    val photos: Map<String, File> = emptyMap(),
)

@HiltViewModel
class WalkSummaryViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val walks: WalkRepository,
    private val store: FindStore,
    settings: SettingsStore,
) : ViewModel() {

    private val id: String = checkNotNull(saved[WALK_ID])
    private val photos = MutableStateFlow<Map<String, File>>(emptyMap())

    /** Null before the walk is read and after it is deleted. */
    val state: StateFlow<WalkSummaryState?> =
        combine(walks.watch(id), settings.value, walks.watchFinds(id), photos) { walk, units, finds, photos ->
            walk?.let { WalkSummaryState(it, units.distance, finds, photos) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)

    init {
        // Every photo is looked for once; one this phone never had is fetched from the server.
        viewModelScope.launch {
            walks.watchFinds(id).collect { finds ->
                finds.filterNot { it.id in photos.value }.forEach { find ->
                    store.photo(find)?.let { file -> photos.update { it + (find.id to file) } }
                }
            }
        }
    }

    fun saveNote(note: String) {
        val kept = state.value?.walk?.note.orEmpty()
        if (note.trim() != kept) viewModelScope.launch { walks.setNote(id, note) }
    }

    fun rename(name: String) {
        viewModelScope.launch { walks.setName(id, name) }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            walks.delete(id)
            onDone()
        }
    }

    companion object {
        /** The name of the route argument that carries the walk. */
        const val WALK_ID = "id"
    }
}
