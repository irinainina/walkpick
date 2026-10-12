package com.walkspot.app.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.find.FindRepository
import com.walkspot.app.data.find.FindStore
import com.walkspot.app.data.local.FindKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject

private const val MAX_ZOOM = 4

// How long the camera shows "Identifying" before it gives up waiting for a name.
private const val NAMING_WAIT_MS = 12_000L

/** The photo just taken: the find it became and its file. */
data class Shot(val find: String, val photo: File?)

data class CameraState(
    val flash: Boolean = false,
    /** How many times closer than the lens sees: a bird across the road is a dot without it. */
    val zoom: Int = 1,
    /** A photo was taken and is being kept. */
    val saving: Boolean = false,
    /** The kept photo, shown until the walker says what it is. */
    val shot: Shot? = null,
    /** The name of the photo is being looked up. */
    val identifying: Boolean = false,
    /** The last photo was not kept: the phone could not tell where it is. */
    val noPlace: Boolean = false,
    /** The find just named, until its card is opened. */
    val found: String? = null,
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val finds: FindRepository,
    private val store: FindStore,
) : ViewModel() {

    private val current = MutableStateFlow(CameraState())

    val state: StateFlow<CameraState> = current

    fun toggleFlash() = current.update { it.copy(flash = !it.flash) }

    /** One, two and four times, round and round. */
    fun nextZoom() = current.update { it.copy(zoom = if (it.zoom >= MAX_ZOOM) 1 else it.zoom * 2) }

    /** Keeps the photo the camera gave [turned] degrees off. */
    fun keep(photo: Bitmap, turned: Int) {
        if (current.value.saving) return
        current.update { it.copy(saving = true, noPlace = false) }
        viewModelScope.launch {
            val kept = finds.add(photo, turned)
            val shot = kept?.let { Shot(it.id, store.photo(it)) }
            current.update { it.copy(saving = false, noPlace = kept == null, shot = shot) }
        }
    }

    /** The walker says what the photo shows; anything but a plain photo is looked up and opened. */
    fun choose(kind: FindKind) {
        val shot = current.value.shot ?: return
        if (kind == FindKind.PHOTO) return leave()
        current.update { it.copy(identifying = true) }
        viewModelScope.launch {
            store.change(shot.find) { it.copy(kind = kind) }
            // Without an answer in time the find keeps its kind and gets its name with a later sync.
            withTimeoutOrNull(NAMING_WAIT_MS) { store.identify(shot.find) }
            current.update { it.copy(identifying = false, shot = null, found = shot.find) }
        }
    }

    /** Back to the viewfinder; the photo stays a plain photo. */
    fun leave() = current.update { it.copy(shot = null) }

    fun shown() = current.update { it.copy(found = null) }
}
