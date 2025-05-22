package com.lymors.phonesecure.domain.repository

import com.lymors.phonesecure.domain.model.AntiTheftSettings
import com.lymors.phonesecure.domain.model.EmergencyContact
import com.lymors.phonesecure.domain.model.PasswordSettings
import com.lymors.phonesecure.domain.model.SecuritySettings
import com.lymors.phonesecure.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user data
 */
interface UserRepository {
    /**
     * Get the current user profile
     */
    suspend fun getCurrentUser(): User?
    
    /**
     * Save or update user profile
     */
    suspend fun saveUser(user: User): Boolean
    
    /**
     * Get user as a Flow for reactive updates
     */
    fun getUserFlow(): Flow<User?>
    
    /**
     * Get security settings
     */
    suspend fun getSecuritySettings(): SecuritySettings
    
    /**
     * Update security settings
     */
    suspend fun updateSecuritySettings(settings: SecuritySettings): Boolean
    
    /**
     * Get all emergency contacts
     */
    suspend fun getEmergencyContacts(): List<EmergencyContact>
    
    /**
     * Add a new emergency contact
     */
    suspend fun addEmergencyContact(contact: EmergencyContact): Boolean
    
    /**
     * Update an existing emergency contact
     */
    suspend fun updateEmergencyContact(contact: EmergencyContact): Boolean
    
    /**
     * Delete an emergency contact by ID
     */
    suspend fun deleteEmergencyContact(contactId: String): Boolean
    
    /**
     * Get the current anti-theft settings
     */
    suspend fun getAntiTheftSettings(): AntiTheftSettings
    
    /**
     * Save anti-theft settings
     */
    suspend fun saveAntiTheftSettings(settings: AntiTheftSettings): Boolean
    
    /**
     * Get a Flow of anti-theft settings updates
     */
    fun getAntiTheftSettingsFlow(): Flow<AntiTheftSettings>
    
    /**
     * Get the current password settings
     */
    suspend fun getPasswordSettings(): PasswordSettings
    
    /**
     * Password protection methods
     */
    suspend fun savePasswordSettings(settings: PasswordSettings)
    suspend fun getPasswordSettings(): PasswordSettings
    suspend fun verifyPassword(password: String): Boolean
    suspend fun incrementFailedPasswordAttempts(): Int
    suspend fun resetFailedPasswordAttempts()
    suspend fun getMaxFailedPasswordAttempts(): Int
    
    /**
     * Get a Flow of password settings updates
     */
    fun getPasswordSettingsFlow(): Flow<PasswordSettings>
    
    /**
    /**
     * Record a failed password attempt
     */
    suspend fun recordFailedPasswordAttempt(): Boolean
    
    /**
     * Reset failed password attempts counter
     */
    suspend fun resetFailedPasswordAttempts(): Boolean
}
