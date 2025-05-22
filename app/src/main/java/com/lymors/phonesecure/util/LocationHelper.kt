package com.lymors.phonesecure.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.getSystemService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class LocationHelper @Inject constructor(
    private val context: Context
) {
    private val tag = "LocationHelper"
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCoroutine { continuation ->
        try {
            // Check if location is enabled
            val locationManager = context.getSystemService<LocationManager>()
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) != true &&
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) != true) {
                continuation.resume(null)
                return@suspendCoroutine
            }

            // Create location request
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(15000)
                .build()

            // Create location callback
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    continuation.resume(result.lastLocation)
                }
            }

            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                Log.e(tag, "Error getting location", e)
                continuation.resumeWithException(e)
            }

            // Also try to get last known location as fallback
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                        continuation.resume(location)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Error getting last known location", e)
                    // Don't resume here, wait for location updates
                }

        } catch (e: Exception) {
            Log.e(tag, "Error in getCurrentLocation", e)
            continuation.resumeWithException(e)
        }
    }
}
