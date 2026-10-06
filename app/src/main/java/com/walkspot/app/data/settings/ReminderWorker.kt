package com.walkspot.app.data.settings

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.walkspot.app.MainActivity
import com.walkspot.app.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val CHANNEL_ID = "walk_reminders"
private const val NOTIFICATION_ID = 1
private const val REMINDER_TAG = "walk_reminder"

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val settings: SettingsStore,
    private val reminders: Reminders,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val now = settings.value.first()
        // The reminder may have been switched off while this one waited.
        if (now.remindersOn) {
            ring()
            // replacing = false: this worker carries the tag and would cancel itself.
            reminders.schedule(now, replacing = false, justRang = true)
        }
        return Result.success()
    }

    private fun ring() {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return

        val manager = NotificationManagerCompat.from(applicationContext)
        // Android keeps the channel once it exists.
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.reminder_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val open = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(applicationContext.getString(R.string.reminder_title))
            .setContentText(applicationContext.getString(R.string.reminder_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}

class Reminders @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Sets the next reminder by [settings], or clears it when they say none. */
    fun schedule(settings: AppSettings, replacing: Boolean = true, justRang: Boolean = false) {
        val work = WorkManager.getInstance(context)
        if (replacing) work.cancelAllWorkByTag(REMINDER_TAG)
        val now = LocalDateTime.now()
        // Android may start the work a moment early, and the minute it rang for must not be picked again.
        val from = if (justRang) now.plusMinutes(1) else now
        val at = nextReminder(settings, from) ?: return
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(Duration.between(now, at).toMillis(), TimeUnit.MILLISECONDS)
            .addTag(REMINDER_TAG)
            .build()
        work.enqueue(request)
    }
}
