package com.lymors.phonesecure.domain.usecase

import com.lymors.phonesecure.domain.model.EmergencyContact
import com.lymors.phonesecure.domain.model.SecurityEvent
import com.lymors.phonesecure.domain.model.SecurityEventType
import com.lymors.phonesecure.domain.repository.LocationRepository
import com.lymors.phonesecure.domain.repository.SecurityEventRepository
import com.lymors.phonesecure.domain.repository.UserRepository
import java.util.Date
import java.util.UUID

/**
 * Use case for managing SIM change detection functionality
 */
class SimChangeUseCase(
    private val userRepository: UserRepository,
    private val securityEventRepository: SecurityEventRepository,
    private val locationRepository: LocationRepository
) {
    /**
     * Enable SIM change detection
     */
    suspend fun enableSimChangeDetection(): Boolean {
        val settings = userRepository.getSecuritySettings()
        val updatedSettings = settings.copy(isSIMChangeAlertEnabled = true)
        return userRepository.updateSecuritySettings(updatedSettings)
    }
    
    /**
     * Disable SIM change detection
     */
    suspend fun disableSimChangeDetection(): Boolean {
        val settings = userRepository.getSecuritySettings()
        val updatedSettings = settings.copy(isSIMChangeAlertEnabled = false)
        return userRepository.updateSecuritySettings(updatedSettings)
    }
    
    /**
     * Check if SIM change detection is enabled
     */
    suspend fun isSimChangeDetectionEnabled(): Boolean {
        val settings = userRepository.getSecuritySettings()
        return settings.isSIMChangeAlertEnabled
    }
    
    /**
     * Handle SIM change event
     */
    suspend fun handleSimChange(newSimNumber: String, photoPath: String? = null): Boolean {
        // Get current location if available
        val location = locationRepository.getCurrentLocation()
        
        // Log security event
        val eventLogged = securityEventRepository.logSecurityEvent(
            SecurityEvent(
                id = UUID.randomUUID().toString(),
                type = SecurityEventType.SIM_CHANGE,
                timestamp = Date(),
                latitude = location?.latitude,
                longitude = location?.longitude,
                photoPath = photoPath,
                description = "SIM card changed to number: $newSimNumber"
            )
        )
        
        // Start location tracking if it's not already active
        val settings = userRepository.getSecuritySettings()
        if (settings.isLocationTrackingEnabled && !locationRepository.isLocationTrackingActive()) {
            locationRepository.startLocationTracking()
        }
        
        return eventLogged
    }
    
    /**
     * Get emergency contacts to notify on SIM change
     */
    suspend fun getEmergencyContactsToNotify(): List<EmergencyContact> {
        val allContacts = userRepository.getEmergencyContacts()
        return allContacts.filter { it.notifyOnSIMChange }
    }
    
    /**
     * Check if device should be locked on SIM change
     */
    suspend fun shouldLockDeviceOnSimChange(): Boolean {
        val settings = userRepository.getSecuritySettings()
        return settings.lockDeviceOnSIMChange
    }
    
    /**
     * Check if photo should be captured on SIM change
     */
    suspend fun shouldCapturePhotoOnSimChange(): Boolean {
        val settings = userRepository.getSecuritySettings()
        return settings.capturePhotoOnSIMChange
    }
    
    /**
     * Check if SMS should be sent on SIM change
     */
    suspend fun shouldSendSmsOnSimChange(): Boolean {
        val settings = userRepository.getSecuritySettings()
        return settings.sendSMSOnSIMChange
    }
    
    /**
     * Check if email should be sent on SIM change
     */
    suspend fun shouldSendEmailOnSimChange(): Boolean {
        val settings = userRepository.getSecuritySettings()
        return settings.sendEmailOnSIMChange
    }
}
