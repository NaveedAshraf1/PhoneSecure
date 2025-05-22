package com.lymors.phonesecure.domain.repository

import android.location.Location
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for managing device location data
 */
interface LocationRepository {
    /**
     * Get the current location of the device
     */
    suspend fun getCurrentLocation(): Location?
    
    /**
     * Start tracking the device location
     */
    suspend fun startLocationTracking(): Boolean
    
    /**
     * Stop tracking the device location
     */
    suspend fun stopLocationTracking(): Boolean
    
    /**
     * Check if location tracking is currently active
     */
    suspend fun isLocationTrackingActive(): Boolean
    
    /**
     * Get location updates as a Flow for reactive updates
     */
    fun getLocationUpdates(): Flow<Location>
    
    /**
     * Save a location with timestamp
     */
    suspend fun saveLocationHistory(location: Location, timestamp: Date): Boolean
    
    /**
     * Get location history between dates
     */
    suspend fun getLocationHistoryBetweenDates(startDate: Date, endDate: Date): List<Pair<Location, Date>>
    
    /**
     * Clear all location history
     */
    suspend fun clearLocationHistory(): Boolean
}
