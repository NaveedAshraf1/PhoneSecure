package com.lymors.phonesecure.domain.usecase

import com.lymors.phonesecure.domain.model.SecurityEvent
import com.lymors.phonesecure.domain.model.SecurityEventType
import com.lymors.phonesecure.domain.repository.LocationRepository
import com.lymors.phonesecure.domain.repository.SecurityEventRepository
import com.lymors.phonesecure.domain.repository.UserRepository
import java.util.Date
import java.util.UUID

/**
 * Use case for managing intruder detection functionality
 */
class IntruderDetectionUseCase(
    private val userRepository: UserRepository,
    private val securityEventRepository: SecurityEventRepository,
    private val locationRepository: LocationRepository
) {
    /**
     * Enable intruder detection
     */
    suspend fun enableIntruderDetection(): Boolean {
        val settings = userRepository.getSecuritySettings()
        val updatedSettings = settings.copy(isIntruderDetectionEnabled = true)
        return userRepository.updateSecuritySettings(updatedSettings)
    }
    
    /**
     * Disable intruder detection
     */
    suspend fun disableIntruderDetection(): Boolean {
        val settings = userRepository.getSecuritySettings()
        val updatedSettings = settings.copy(isIntruderDetectionEnabled = false)
        return userRepository.updateSecuritySettings(updatedSettings)
    }
    
    /**
     * Check if intruder detection is enabled
     */
    suspend fun isIntruderDetectionEnabled(): Boolean {
        val settings = userRepository.getSecuritySettings()
        return settings.isIntruderDetectionEnabled
    }
    
    /**
     * Record an intruder detection event with photo
     */
    suspend fun recordIntruderEvent(photoPath: String?, description: String): Boolean {
        // Get current location if available
        val location = locationRepository.getCurrentLocation()
        
        return securityEventRepository.logSecurityEvent(
            SecurityEvent(
                id = UUID.randomUUID().toString(),
                type = SecurityEventType.INTRUDER_DETECTED,
                timestamp = Date(),
                latitude = location?.latitude,
                longitude = location?.longitude,
                photoPath = photoPath,
                description = description
            )
        )
    }
    
    /**
     * Get the wrong password attempt threshold
     */
    suspend fun getWrongPasswordAttemptThreshold(): Int {
        val settings = userRepository.getSecuritySettings()
        return settings.wrongPasswordAttempts
    }
    
    /**
     * Update the wrong password attempt threshold
     */
    suspend fun updateWrongPasswordAttemptThreshold(attempts: Int): Boolean {
        val settings = userRepository.getSecuritySettings()
        val updatedSettings = settings.copy(wrongPasswordAttempts = attempts)
        return userRepository.updateSecuritySettings(updatedSettings)
    }
    
    /**
     * Check if photo capture on wrong password is enabled
     */
    suspend fun isCapturePhotoOnWrongPasswordEnabled(): Boolean {
        val settings = userRepository.getSecuritySettings()
        return settings.capturePhotoOnWrongPassword
    }
    
    /**
     * Get all intruder events
     */
    suspend fun getAllIntruderEvents(): List<SecurityEvent> {
        return securityEventRepository.getSecurityEventsByType(SecurityEventType.INTRUDER_DETECTED)
    }
}
