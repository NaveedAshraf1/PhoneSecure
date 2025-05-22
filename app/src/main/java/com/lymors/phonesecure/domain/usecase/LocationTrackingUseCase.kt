package com.lymors.phonesecure.domain.usecase

import android.location.Location
import com.lymors.phonesecure.domain.model.SecurityEvent
import com.lymors.phonesecure.domain.model.SecurityEventType
import com.lymors.phonesecure.domain.repository.LocationRepository
import com.lymors.phonesecure.domain.repository.SecurityEventRepository
import com.lymors.phonesecure.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID

/**
 * Use case for managing location tracking functionality
 */
class LocationTrackingUseCase(
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository,
    private val securityEventRepository: SecurityEventRepository
) {
    /**
     * Enable location tracking
     */
    suspend fun enableLocationTracking(): Boolean {
        val settings = userRepository.getSecuritySettings()
        val updatedSettings = settings.copy(isLocationTrackingEnabled = true)
        val success = userRepository.updateSecuritySettings(updatedSettings)
        
        if (success) {
            locationRepository.startLocationTracking()
            
            // Log security event
            securityEventRepository.logSecurityEvent(
                SecurityEvent(
                    id = UUID.randomUUID().toString(),
                    type = SecurityEventType.DEVICE_LOCATION_CHANGED,
                    timestamp = Date(),
                    description = "Location tracking was enabled"
                )
            )
        }
        
        return success
    }
    
    /**
     * Disable location tracking
     */
    suspend fun disableLocationTracking(): Boolean {
        val settings = userRepository.getSecuritySettings()
        val updatedSettings = settings.copy(isLocationTrackingEnabled = false)
        val success = userRepository.updateSecuritySettings(updatedSettings)
        
        if (success) {
            locationRepository.stopLocationTracking()
            
            // Log security event
            securityEventRepository.logSecurityEvent(
                SecurityEvent(
                    id = UUID.randomUUID().toString(),
                    type = SecurityEventType.DEVICE_LOCATION_CHANGED,
                    timestamp = Date(),
                    description = "Location tracking was disabled"
                )
            )
        }
        
        return success
    }
    
    /**
     * Check if location tracking is enabled
     */
    suspend fun isLocationTrackingEnabled(): Boolean {
        val settings = userRepository.getSecuritySettings()
        return settings.isLocationTrackingEnabled
    }
    
    /**
     * Get the current location
     */
    suspend fun getCurrentLocation(): Location? {
        return locationRepository.getCurrentLocation()
    }
    
    /**
     * Get location updates as a Flow
     */
    fun getLocationUpdates(): Flow<Location> {
        return locationRepository.getLocationUpdates()
    }
    
    /**
     * Save current location with timestamp
     */
    suspend fun saveCurrentLocation(): Boolean {
        val location = locationRepository.getCurrentLocation() ?: return false
        return locationRepository.saveLocationHistory(location, Date())
    }
    
    /**
     * Get location history between dates
     */
    suspend fun getLocationHistoryBetweenDates(startDate: Date, endDate: Date): List<Pair<Location, Date>> {
        return locationRepository.getLocationHistoryBetweenDates(startDate, endDate)
    }
    
    /**
     * Clear all location history
     */
    suspend fun clearLocationHistory(): Boolean {
        return locationRepository.clearLocationHistory()
    }
}
