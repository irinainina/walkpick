package com.walkspot.app.data.steps

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "StepsWorker"
private const val STEPS_WORK = "steps_refresh"
private const val INTERVAL_HOURS = 1L

@HiltWorker
class StepsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val steps: StepsRepository,
) : CoroutineWorker(context, parameters) {

    // A reading that failed is not an error: the sensor keeps counting and the next hour reads it.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result {
        try {
            steps.refresh()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "steps not read", error)
        }
        return Result.success()
    }
}

class StepsSchedule @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun start() {
        val request = PeriodicWorkRequestBuilder<StepsWorker>(INTERVAL_HOURS, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(STEPS_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
