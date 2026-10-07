package com.walkspot.app.data.walk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.resume

/** Where the phone is right now, asked once: for the "my location" button of the map. */
class LocationFinder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun allowed(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Null without the permission, with the location switched off or when the phone cannot tell. */
    suspend fun find(): TrackPoint? {
        // Checked here in full, not through allowed(): lint does not look inside another function.
        val refused = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        if (refused) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { waiting ->
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { found ->
                    val point = found?.let {
                        TrackPoint(it.latitude, it.longitude, Instant.ofEpochMilli(it.time), it.accuracy)
                    }
                    if (waiting.isActive) waiting.resume(point)
                }
                .addOnFailureListener { if (waiting.isActive) waiting.resume(null) }
        }
    }
}
