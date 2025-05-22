package com.lymors.phonesecure.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lymors.phonesecure.PhoneSecureApp
import com.lymors.phonesecure.domain.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Implementation of LocationRepository that uses FusedLocationProviderClient for location tracking
 */
class LocationRepositoryImpl : LocationRepository {
    
    private val context: Context = PhoneSecureApp.getAppContext()
    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationRequest = LocationRequest.Builder(LOCATION_UPDATE_INTERVAL)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
        .build()
    
    private var isTracking = false
    private val _lastLocation = MutableStateFlow<Location?>(null)
    private var locationCallback: LocationCallback? = null
    
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        try {
            val locationTask = fusedLocationClient.lastLocation
            val location = locationTask.await()
            if (location != null) {
                _lastLocation.value = location
            }
            return@withContext location
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun startLocationTracking(): Boolean = withContext(Dispatchers.IO) {
        if (isTracking) return@withContext true
        
        try {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        _lastLocation.value = location
                    }
                }
            }
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            
            isTracking = true
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
    
    override suspend fun stopLocationTracking(): Boolean = withContext(Dispatchers.IO) {
        if (!isTracking) return@withContext true
        
        try {
            locationCallback?.let {
                fusedLocationClient.removeLocationUpdates(it)
            }
            locationCallback = null
            isTracking = false
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
    
    override suspend fun isLocationTrackingActive(): Boolean = withContext(Dispatchers.IO) {
        return@withContext isTracking
    }
    
    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<Location> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    _lastLocation.value = location
                    trySend(location)
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )
        
        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
    
    override suspend fun saveLocationHistory(location: Location, timestamp: Date): Boolean = withContext(Dispatchers.IO) {
        val locationHistory = getLocationHistorySync().toMutableList()
        locationHistory.add(Pair(location, timestamp))
        return@withContext saveLocationHistory(locationHistory)
    }
    
    override suspend fun getLocationHistoryBetweenDates(startDate: Date, endDate: Date): List<Pair<Location, Date>> = withContext(Dispatchers.IO) {
        val locationHistory = getLocationHistorySync()
        return@withContext locationHistory.filter { it.second in startDate..endDate }
    }
    
    override suspend fun clearLocationHistory(): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveLocationHistory(emptyList())
    }
    
    private fun getLocationHistorySync(): List<Pair<Location, Date>> {
        val historyJson = prefs.getString(KEY_LOCATION_HISTORY, null)
        if (historyJson != null) {
            val type = object : TypeToken<List<LocationHistoryEntry>>() {}.type
            val entries: List<LocationHistoryEntry> = gson.fromJson(historyJson, type)
            return entries.map { Pair(it.location, it.timestamp) }
        }
        return emptyList()
    }
    
    private fun saveLocationHistory(history: List<Pair<Location, Date>>): Boolean {
        val entries = history.map { LocationHistoryEntry(it.first, it.second) }
        val historyJson = gson.toJson(entries)
        return prefs.edit().putString(KEY_LOCATION_HISTORY, historyJson).commit()
    }
    
    /**
     * Helper class for serializing location history
     */
    private data class LocationHistoryEntry(
        val location: Location,
        val timestamp: Date
    )
    
    companion object {
        private const val PREFS_NAME = "phone_secure_prefs"
        private const val KEY_LOCATION_HISTORY = "location_history"
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds
        private const val LOCATION_FASTEST_INTERVAL = 5000L // 5 seconds
        
        /**
         * Extension function to await the result of a Task<T>
         */
        private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T? {
            return try {
                if (isComplete) {
                    if (isSuccessful) result else null
                } else {
                    withContext(Dispatchers.IO) {
                        var result: T? = null
                        addOnSuccessListener { result = it }
                        while (result == null && !isComplete) {
                            kotlinx.coroutines.delay(10)
                        }
                        if (isSuccessful) result else null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
