package com.walkspot.app.data.walk

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.walkspot.app.BuildConfig
import com.walkspot.app.MainActivity
import com.walkspot.app.R
import com.walkspot.app.data.steps.StepCounter
import com.walkspot.app.data.steps.StepsAccess
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

private const val TAG = "WalkService"
private const val CHANNEL_ID = "walk_recording"
private const val NOTIFICATION_ID = 2

// A walker moves a few metres in this time; asking more often only costs battery.
private const val UPDATE_MS = 2_000L
private const val FASTEST_UPDATE_MS = 1_000L

/**
 * Listens to GPS while a walk is recorded. A foreground service with a notification: Android stops
 * giving the location to an app that has left the screen without one.
 */
@AndroidEntryPoint
class WalkService : Service() {

    @Inject
    lateinit var recorder: WalkRecorder

    @Inject
    lateinit var steps: StepCounter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var counting: Job? = null
    private lateinit var locations: FusedLocationProviderClient

    private val listener = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach {
                val point = TrackPoint(it.latitude, it.longitude, Instant.ofEpochMilli(it.time), it.accuracy)
                // A gap in a route is either a silent receiver or points left out: the log tells which.
                if (BuildConfig.DEBUG) {
                    val last = recorder.track.value?.legs?.lastOrNull()?.lastOrNull()
                    dropReason(last, point)?.let { why -> Log.d(TAG, "point left out: $why") }
                }
                recorder.report(point)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val allowed = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Android 10 and later want to be told what the service is for.
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type)
        recorder.start(Instant.now())
        locations = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_MS)
            .setMinUpdateIntervalMillis(FASTEST_UPDATE_MS)
            .build()
        locations.requestLocationUpdates(request, listener, Looper.getMainLooper())
        countSteps()
        return START_STICKY
    }

    override fun onDestroy() {
        if (::locations.isInitialized) locations.removeLocationUpdates(listener)
        scope.cancel()
        super.onDestroy()
    }

    // The sensor is asked while the walk goes on; without the permission the walk has no steps.
    private fun countSteps() {
        if (counting?.isActive == true || steps.access() != StepsAccess.COUNTING) return
        counting = scope.launch { steps.watch().collect(recorder::reportSteps) }
    }

    private fun notification() = NotificationCompat.Builder(this, channel())
        .setSmallIcon(R.drawable.ic_logo_mark)
        .setContentTitle(getString(R.string.walk_notification_title))
        .setContentText(getString(R.string.walk_notification_text))
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(MainActivity.EXTRA_OPEN_WALK, true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()

    // Android keeps the channel once it exists. Low importance: the notification is there, but silent.
    private fun channel(): String {
        val manager = NotificationManagerCompat.from(this)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val name = getString(R.string.walk_channel)
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        return CHANNEL_ID
    }
}

/** Starts and stops the recording from the screens. */
class WalkControl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recorder: WalkRecorder,
) {
    /** The location permission must be granted already; without it the service ends at once. */
    fun start() = ContextCompat.startForegroundService(context, Intent(context, WalkService::class.java))

    /**
     * Stops listening to GPS and returns the walk as it was recorded. The walk stays in its draft,
     * paused, until [done] is called: the app may be ended before the walk is kept.
     */
    fun stop(): WalkTrack? {
        context.stopService(Intent(context, WalkService::class.java))
        recorder.pause(Instant.now())
        return recorder.track.value
    }

    /** The stopped walk is kept or not worth keeping: its draft goes away. */
    fun done() {
        recorder.finish()
    }
}
