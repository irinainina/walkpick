package com.walkspot.app.debug

import android.Manifest
import android.os.Build
import android.util.Log
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.walkspot.app.data.local.DailyStepsDao
import com.walkspot.app.data.steps.StepsRepository
import com.walkspot.app.ui.components.AppButton
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val TAG = "StepsCheck"
private const val SHOWN_DAYS = 7L

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StepsGraph {
    fun steps(): StepsRepository
    fun days(): DailyStepsDao
}

/** Step counting without the Today screen: its state, the permission request and the days counted. */
@Composable
fun StepsCheck() {
    val context = LocalContext.current
    val graph = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, StepsGraph::class.java)
    }
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf("") }

    // The point of the check is to print why a step did not work.
    @Suppress("TooGenericExceptionCaught")
    suspend fun refresh() {
        report = try {
            val access = graph.steps().access()
            graph.steps().refresh()
            val days = graph.days().since(LocalDate.now().minusDays(SHOWN_DAYS))
            val lines = days.map {
                "${it.day}: ${it.steps} steps, ${it.distanceM} m, ${it.activeMinutes} min, sent ${it.synced}"
            }
            (listOf("access: $access") + lines).joinToString("\n")
        } catch (e: Exception) {
            Log.w(TAG, e)
            "failed: $e"
        }
        Log.i(TAG, report)
    }

    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        scope.launch { refresh() }
    }
    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(report)
        AppButton("Allow", onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ask.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        })
        AppButton("Refresh", onClick = { scope.launch { refresh() } })
    }
}
