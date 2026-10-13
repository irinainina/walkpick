package com.walkpick.app.data.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

enum class StepsAccess { NO_SENSOR, NOT_ALLOWED, COUNTING }

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

    /**
     * Every reading of the sensor for as long as the flow is collected. On some phones the sensor
     * counts only while somebody listens to it: a walk listens all the way, or its steps are lost.
     */
    fun watch(): Flow<Long> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values[0].toLong())
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensors.unregisterListener(listener) }
    }
}
