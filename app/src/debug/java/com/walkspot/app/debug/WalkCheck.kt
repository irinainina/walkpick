package com.walkspot.app.debug

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walkspot.app.data.walk.WalkControl
import com.walkspot.app.data.walk.WalkRecorder
import com.walkspot.app.data.walk.paceSecondsPerKm
import com.walkspot.app.ui.components.AppButton
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import java.time.Instant

private const val TICK_MS = 1_000L

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WalkGraph {
    fun recorder(): WalkRecorder
    fun control(): WalkControl
}

/** The recording of a walk without its screen: permissions, start, pause, stop and what was recorded. */
@Composable
fun WalkCheck() {
    val context = LocalContext.current
    val graph = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, WalkGraph::class.java)
    }
    val track by graph.recorder().track.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(Instant.now()) }
    var last by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(TICK_MS)
        }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    val report = track?.let {
        val seconds = it.movingSeconds(now)
        val accuracy = it.points.lastOrNull()?.accuracyM
        "started ${it.startedAt}\npaused ${it.paused}\nlegs ${it.legs.size}, points ${it.points.size}\n" +
            "metres ${it.metres.toInt()}, moving $seconds s\n" +
            "pace ${paceSecondsPerKm(it.metres, seconds)} s/km, last accuracy $accuracy m"
    } ?: "no walk\n$last"

    Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(report)
        AppButton("Allow", onClick = {
            val location = Manifest.permission.ACCESS_FINE_LOCATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ask.launch(arrayOf(location, Manifest.permission.POST_NOTIFICATIONS))
            } else {
                ask.launch(arrayOf(location))
            }
        })
        AppButton("Start", onClick = { graph.control().start() })
        AppButton(
            text = if (track?.paused == true) "Resume" else "Pause",
            onClick = {
                val recorder = graph.recorder()
                if (track?.paused == true) recorder.resume(Instant.now()) else recorder.pause(Instant.now())
            },
        )
        AppButton("Stop", onClick = {
            val done = graph.control().stop()
            last = "last walk: ${done?.points?.size} points, ${done?.metres?.toInt()} m"
        })
    }
}
