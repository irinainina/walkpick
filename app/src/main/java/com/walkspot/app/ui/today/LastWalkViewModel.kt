package com.walkspot.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.walk.WalkRepository
import com.walkspot.app.data.walk.routePlaces
import com.walkspot.app.ui.map.DiaryWalk
import com.walkspot.app.ui.map.routeSketch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L

/** The tile of the route on Today is this many times wider than high, inside its edges. */
const val LAST_WALK_ASPECT = 3.75f

/** The latest walk with its photos counted and its route drawn, for the card on Today. */
@HiltViewModel
class LastWalkViewModel @Inject constructor(repository: WalkRepository) : ViewModel() {

    /** Null until there is a walk. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val walk: StateFlow<DiaryWalk?> = repository.watchLatest()
        .flatMapLatest { latest ->
            if (latest == null) {
                flowOf(null)
            } else {
                val sketch = routeSketch(routePlaces(latest.route), LAST_WALK_ASPECT)
                repository.watchFinds(latest.id).map { DiaryWalk(latest, it.size, sketch) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)
}
