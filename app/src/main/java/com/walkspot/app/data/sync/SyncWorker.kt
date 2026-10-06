package com.walkspot.app.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "SyncWorker"
private const val RESTORE = "restore"

// One name: a restore and a push never run at once over the same rows.
private const val SYNC_WORK = "cloud_sync"

// A minute, then two, four, eight; the fifth try gives up until the next write.
private const val ATTEMPTS = 5
private const val BACKOFF_MINUTES = 1L

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val sync: CloudSync,
) : CoroutineWorker(context, parameters) {

    // A failed sync is retried, never rethrown: it would be reported as a crash of an app nobody is using.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = try {
        if (inputData.getBoolean(RESTORE, false)) sync.restore() else sync.push()
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.w(TAG, "sync failed", error)
        if (runAttemptCount >= ATTEMPTS) Result.failure() else Result.retry()
    }
}

class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // KEEP: a push already waiting will carry whatever this one would have carried.
    fun pushSoon() = enqueue(restoring = false, policy = ExistingWorkPolicy.KEEP)

    // Asked for once, at sign-in, and must not be dropped because a push is queued.
    fun restoreFromCloud() = enqueue(restoring = true, policy = ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun enqueue(restoring: Boolean, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MINUTES, TimeUnit.MINUTES)
            .setInputData(Data.Builder().putBoolean(RESTORE, restoring).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(SYNC_WORK, policy, request)
    }
}
