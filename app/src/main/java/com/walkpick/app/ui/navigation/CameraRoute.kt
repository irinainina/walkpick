package com.walkpick.app.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walkpick.app.ui.camera.CameraViewModel
import com.walkpick.app.ui.screens.CameraScreen
import kotlinx.coroutines.flow.first

private const val CAMERA = Manifest.permission.CAMERA

internal fun NavGraphBuilder.cameraRoute(navController: NavHostController) {
    composable(Screen.Camera.route) {
        val model: CameraViewModel = hiltViewModel()
        val state by model.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val owner = LocalLifecycleOwner.current
        val camera = remember {
            LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_CAPTURE) }
        }
        var allowed by remember {
            val answer = ContextCompat.checkSelfPermission(context, CAMERA)
            mutableStateOf(answer == PackageManager.PERMISSION_GRANTED)
        }
        // Without the camera there is nothing to do here: a refusal leads back to the walk.
        val ask = rememberPermissionRequest(CAMERA) { granted ->
            if (granted) allowed = true else navController.popBackStack()
        }
        LaunchedEffect(allowed) { if (allowed) camera.bindToLifecycle(owner) else ask() }
        LaunchedEffect(state.flash) {
            camera.imageCaptureFlashMode =
                if (state.flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        }
        // The controller is made anew after the find card, so the zoom waits for its camera to open.
        // A lens that cannot come as close as asked comes as close as it can.
        LaunchedEffect(camera, state.zoom) {
            val closest = camera.zoomState.asFlow().first().maxZoomRatio
            camera.setZoomRatio(state.zoom.toFloat().coerceAtMost(closest))
        }
        // The card of the new find opens over the camera: back leads to the next shot.
        LaunchedEffect(state.found) {
            state.found?.let {
                navController.navigate(findCardRoute(it))
                model.shown()
            }
        }
        val taken = object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                model.keep(image.toBitmap(), image.imageInfo.rotationDegrees)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) = model.failed()
        }
        CameraScreen(
            state = state,
            onClose = {
                if (state.shot != null && !state.identifying) model.leave() else navController.popBackStack()
            },
            onKind = model::choose,
            onFlash = model::toggleFlash,
            onZoom = model::nextZoom,
            onFlip = {
                val back = camera.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA
                camera.cameraSelector =
                    if (back) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            },
            onShutter = {
                if (model.shoot()) camera.takePicture(ContextCompat.getMainExecutor(context), taken)
            },
        ) {
            if (allowed) {
                AndroidView({ PreviewView(it).apply { controller = camera } }, Modifier.fillMaxSize())
            }
        }
    }
}
