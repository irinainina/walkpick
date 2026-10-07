package com.walkspot.app.ui.find

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.find.FindStore
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.walk.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L

/** The photos of one walk in the order they were taken, opened on [start]. */
data class PhotoViewerState(
    val finds: List<FindEntity>,
    val start: Int,
    /** Null for a walk still going on. */
    val walk: WalkEntity? = null,
)

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val store: FindStore,
    walks: WalkRepository,
) : ViewModel() {

    private val id: String = checkNotNull(saved[FIND_ID])
    private val photos = mutableMapOf<String, File?>()

    /** Null before the photos are read and when the photo it was opened on is gone. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<PhotoViewerState?> = store.watch(id)
        .map { it?.walkId }
        .flatMapLatest { walkId ->
            if (walkId == null) {
                flowOf(null)
            } else {
                combine(walks.watchFinds(walkId), walks.watch(walkId)) { finds, walk ->
                    // The photos of a walk going on are not in the database yet: the one opened stands alone.
                    val all = finds.ifEmpty { listOfNotNull(store.watchNow(id)) }
                    val at = all.indexOfFirst { it.id == id }
                    if (at < 0) null else PhotoViewerState(all, at, walk)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)

    suspend fun photo(find: FindEntity): File? =
        if (find.id in photos) photos[find.id] else store.photo(find).also { photos[find.id] = it }

    fun toggleBest(find: FindEntity) {
        viewModelScope.launch { store.change(find.id) { it.copy(isBest = !it.isBest) } }
    }

    companion object {
        /** The name of the route argument that carries the find the viewer opens on. */
        const val FIND_ID = "id"
    }
}
