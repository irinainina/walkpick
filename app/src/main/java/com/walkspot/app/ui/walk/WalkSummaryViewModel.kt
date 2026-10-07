package com.walkspot.app.ui.walk

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.settings.DistanceUnit
import com.walkspot.app.data.settings.SettingsStore
import com.walkspot.app.data.walk.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L

data class WalkSummaryState(val walk: WalkEntity, val distanceUnit: DistanceUnit = DistanceUnit.KM)

@HiltViewModel
class WalkSummaryViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val walks: WalkRepository,
    settings: SettingsStore,
) : ViewModel() {

    private val id: String = checkNotNull(saved[WALK_ID])

    /** Null before the walk is read and after it is deleted. */
    val state: StateFlow<WalkSummaryState?> =
        combine(walks.watch(id), settings.value) { walk, units ->
            walk?.let { WalkSummaryState(it, units.distance) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)

    fun saveNote(note: String) {
        val kept = state.value?.walk?.note.orEmpty()
        if (note.trim() != kept) viewModelScope.launch { walks.setNote(id, note) }
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
