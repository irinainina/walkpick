package com.walkspot.app.ui.find

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.find.FindStore
import com.walkspot.app.data.find.choosing
import com.walkspot.app.data.find.renamed
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.data.local.WalkEntity
import com.walkspot.app.data.walk.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L

data class FindCardState(
    val find: FindEntity,
    /** Null while the photo is fetched and when it cannot be had. */
    val photo: File? = null,
    /** Null while the walk of the find is still going on. */
    val walk: WalkEntity? = null,
)

@HiltViewModel
class FindCardViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val finds: FindStore,
    walks: WalkRepository,
) : ViewModel() {

    private val id: String = checkNotNull(saved[FIND_ID])
    private val photo = MutableStateFlow<File?>(null)

    /** Null before the find is read and after it is deleted. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<FindCardState?> = finds.watch(id)
        .flatMapLatest { find ->
            if (find == null) {
                flowOf(null)
            } else {
                combine(photo, walks.watch(find.walkId)) { file, walk -> FindCardState(find, file, walk) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)

    init {
        viewModelScope.launch { photo.value = finds.photo(finds.watch(id).filterNotNull().first()) }
    }

    fun toggleBest() {
        viewModelScope.launch { finds.change(id) { it.copy(isBest = !it.isBest) } }
    }

    fun choose(option: Int) = change { it.choosing(option) }

    fun rename(name: String) = change { it.renamed(name) }

    /** Asks the services again, as [kind] when the walker says anew what the find is. */
    fun retry(kind: FindKind?) = change { it.copy(kind = kind ?: it.kind) }

    // A new name wants its own description, a new kind its own service: both are asked at once.
    private fun change(transform: (FindEntity) -> FindEntity) {
        viewModelScope.launch {
            finds.change(id, transform)
            finds.identify(id)
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            finds.delete(id)
            onDone()
        }
    }

    companion object {
        /** The name of the route argument that carries the find. */
        const val FIND_ID = "id"
    }
}
