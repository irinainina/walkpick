package com.walkspot.app.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkspot.app.data.find.FindRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraState(
    val flash: Boolean = false,
    /** A photo was taken and is being kept and looked up. */
    val identifying: Boolean = false,
    /** The last photo was not kept: the phone could not tell where it is. */
    val noPlace: Boolean = false,
)

@HiltViewModel
class CameraViewModel @Inject constructor(private val finds: FindRepository) : ViewModel() {

    private val current = MutableStateFlow(CameraState())

    val state: StateFlow<CameraState> = current

    fun toggleFlash() = current.update { it.copy(flash = !it.flash) }

    /** Keeps the photo the camera gave [turned] degrees off. */
    fun keep(photo: Bitmap, turned: Int) {
        current.update { it.copy(identifying = true, noPlace = false) }
        viewModelScope.launch {
            val kept = finds.add(photo, turned)
            current.update { it.copy(identifying = false, noPlace = kept == null) }
        }
    }
}
