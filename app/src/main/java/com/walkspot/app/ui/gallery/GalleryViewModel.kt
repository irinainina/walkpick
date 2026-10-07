package com.walkspot.app.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.find.FindStore
import com.walkspot.app.data.local.FindDao
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.WalkDao
import com.walkspot.app.data.local.WalkEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L

enum class GalleryTab { All, Best }

/** The photos of one walk, in the order they were taken. */
data class GalleryGroup(val walk: WalkEntity, val finds: List<FindEntity>)

data class GalleryState(
    val tab: GalleryTab = GalleryTab.All,
    val today: LocalDate = LocalDate.now(),
    /** The walks that have photos, the latest first. */
    val groups: List<GalleryGroup> = emptyList(),
    /** The photos marked with a star, the latest first. */
    val best: List<FindEntity> = emptyList(),
    /** The photos picked in the selection mode; null outside it. */
    val selected: Set<String>? = null,
)

private data class Choice(val tab: GalleryTab = GalleryTab.All, val selected: Set<String>? = null)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    finds: FindDao,
    walks: WalkDao,
    private val store: FindStore,
) : ViewModel() {

    private val choice = MutableStateFlow(Choice())

    // A photo is looked for once in the life of the screen, found or not: a photo the server does
    // not have would be asked for again with every scroll.
    private val photos = mutableMapOf<String, File?>()

    val state: StateFlow<GalleryState?> =
        combine(finds.watchAll(), walks.watchAll(), choice) { finds, walks, choice ->
            GalleryState(
                tab = choice.tab,
                groups = galleryGroups(walks, finds),
                best = finds.filter { it.isBest },
                selected = choice.selected,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)

    suspend fun photo(find: FindEntity): File? =
        if (find.id in photos) photos[find.id] else store.photo(find).also { photos[find.id] = it }

    fun open(tab: GalleryTab) = choice.update { it.copy(tab = tab) }

    /** Starts the selection, or puts a photo into it or takes it out. */
    fun toggle(id: String) = choice.update {
        val picked = it.selected.orEmpty()
        it.copy(selected = if (id in picked) picked - id else picked + id)
    }

    /** Starts the picking of photos with none picked, or ends it. */
    fun selecting(on: Boolean) = choice.update { it.copy(selected = if (on) emptySet() else null) }

    fun addSelectedToBest() = onSelected { id -> store.change(id) { it.copy(isBest = true) } }

    fun deleteSelected() = onSelected { store.delete(it) }

    private fun onSelected(act: suspend (String) -> Unit) {
        val picked = choice.value.selected.orEmpty()
        selecting(false)
        viewModelScope.launch { picked.forEach { act(it) } }
    }
}

/** The finds by their walks: the latest walk first, its photos in the order they were taken. */
internal fun galleryGroups(walks: List<WalkEntity>, finds: List<FindEntity>): List<GalleryGroup> {
    val byWalk = finds.groupBy { it.walkId }
    return walks.sortedByDescending { it.startedAt }.mapNotNull { walk ->
        byWalk[walk.id]?.let { GalleryGroup(walk, it.sortedBy { find -> find.takenAt }) }
    }
}
