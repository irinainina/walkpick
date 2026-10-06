package com.walkspot.app.data.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.resume

// The sensor answers at once; a phone that keeps silent is treated as one that could not be read.
private const val ANSWER_MS = 5_000L

enum class StepsAccess { NO_SENSOR, NOT_ALLOWED, COUNTING }

// The sensor counts from the last start of the phone, so a reading means nothing without its boot.
data class CounterReading(val boot: Int, val steps: Long, val at: Instant)

class StepCounter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    // Before Android 10 the sensor is open to every app.
    fun access(): StepsAccess = when {
        sensor == null -> StepsAccess.NO_SENSOR
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) !=
            PackageManager.PERMISSION_GRANTED -> StepsAccess.NOT_ALLOWED
        else -> StepsAccess.COUNTING
    }

    suspend fun read(): CounterReading? = withTimeoutOrNull(ANSWER_MS) {
        suspendCancellableCoroutine { waiting ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    sensors.unregisterListener(this)
                    val reading = CounterReading(boot(), event.values[0].toLong(), Instant.now())
                    if (waiting.isActive) waiting.resume(reading)
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }
            sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            waiting.invokeOnCancellation { sensors.unregisterListener(listener) }
        }
    }

    private fun boot() = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
}
