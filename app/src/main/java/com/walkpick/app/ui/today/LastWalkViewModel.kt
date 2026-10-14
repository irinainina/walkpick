package com.walkpick.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkpick.app.data.find.FindStore
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.walk.WalkRepository
import com.walkpick.app.ui.walk.DiaryWalk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

private const val KEEP_AFTER_LEAVING_MS = 5_000L

/** The latest walk with its photos, for the card on Walk. */
@HiltViewModel
class LastWalkViewModel @Inject constructor(
    repository: WalkRepository,
    private val store: FindStore,
) : ViewModel() {

    /** Null until there is a walk. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val walk: StateFlow<DiaryWalk?> = repository.watchLatest()
        .flatMapLatest { latest ->
            if (latest == null) {
                flowOf(null)
            } else {
                repository.watchFinds(latest.id).map { DiaryWalk(latest, it.size, emptyList(), it) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_AFTER_LEAVING_MS), null)

    suspend fun photo(find: FindEntity): File? = store.photo(find)
}
