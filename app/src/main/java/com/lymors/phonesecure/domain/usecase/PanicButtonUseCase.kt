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
 * Use case for managing panic button functionality
 */
class PanicButtonUseCase(
    private val userRepository: UserRepository,
    private val securityEventRepository: SecurityEventRepository,
    private val locationRepository: LocationRepository
) {
    /**
     * Enable panic button
     */
    suspend fun enablePanicButton(): Boolean {
        val settings = userRepository.getSecuritySettings()
        val updatedSettings = settings.copy(isPanicButtonEnabled = true)
        return userRepository.updateSecuritySettings(updatedSettings)
    }
    
    /**
     * Disable panic button
     */
    suspend fun disablePanicButton(): Boolean {
        val settings = userRepository.getSecuritySettings()
        val updatedSettings = settings.copy(isPanicButtonEnabled = false)
        return userRepository.updateSecuritySettings(updatedSettings)
    }
    
    /**
     * Check if panic button is enabled
     */
    suspend fun isPanicButtonEnabled(): Boolean {
        val settings = userRepository.getSecuritySettings()
        return settings.isPanicButtonEnabled
    }
    
    /**
     * Trigger panic button
     */
    suspend fun triggerPanicButton(audioPath: String? = null): Boolean {
        // Get current location if available
        val location = locationRepository.getCurrentLocation()
        
        // Log security event
        val eventLogged = securityEventRepository.logSecurityEvent(
            SecurityEvent(
                id = UUID.randomUUID().toString(),
                type = SecurityEventType.PANIC_BUTTON_PRESSED,
                timestamp = Date(),
                latitude = location?.latitude,
                longitude = location?.longitude,
                audioPath = audioPath,
                description = "Panic button was triggered"
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
     * Get emergency contacts to notify on panic button trigger
     */
    suspend fun getEmergencyContactsToNotify(): List<EmergencyContact> {
        val allContacts = userRepository.getEmergencyContacts()
        return allContacts.filter { it.notifyOnPanicButton }
    }
    
    /**
     * Get user information to include in emergency notifications
     */
    suspend fun getUserInfoForEmergencyNotification(): Pair<String, String>? {
        val user = userRepository.getCurrentUser() ?: return null
        return Pair(user.name, user.phone)
    }
}
