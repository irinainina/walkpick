package com.walkspot.app.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.find.FindRepository
import com.walkspot.app.data.local.FindKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_ZOOM = 4

data class CameraState(
    /** What the next photo will show: it decides which service is asked for the name. */
    val kind: FindKind = FindKind.PLANT,
    val flash: Boolean = false,
    /** How many times closer than the lens sees: a bird across the road is a dot without it. */
    val zoom: Int = 1,
    /** A photo was taken and is being kept and looked up. */
    val identifying: Boolean = false,
    /** The last photo was not kept: the phone could not tell where it is. */
    val noPlace: Boolean = false,
    /** The find just made, until its card is opened. */
    val found: String? = null,
)

@HiltViewModel
class CameraViewModel @Inject constructor(private val finds: FindRepository) : ViewModel() {

    private val current = MutableStateFlow(CameraState())

    val state: StateFlow<CameraState> = current

    fun choose(kind: FindKind) = current.update { it.copy(kind = kind) }

    fun toggleFlash() = current.update { it.copy(flash = !it.flash) }

    /** One, two and four times, round and round. */
    fun nextZoom() = current.update { it.copy(zoom = if (it.zoom >= MAX_ZOOM) 1 else it.zoom * 2) }

    /** Keeps the photo the camera gave [turned] degrees off. */
    fun keep(photo: Bitmap, turned: Int) {
        current.update { it.copy(identifying = true, noPlace = false) }
        viewModelScope.launch {
            val kept = finds.add(photo, turned, current.value.kind)
            current.update { it.copy(identifying = false, noPlace = kept == null, found = kept?.id) }
        }
    }

    fun shown() = current.update { it.copy(found = null) }
}
