package com.lymors.phonesecure.domain.repository

import com.lymors.phonesecure.domain.model.IntruderEvent
import com.lymors.phonesecure.domain.model.SecurityEvent
import com.lymors.phonesecure.domain.model.SecurityEventType
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for managing security events
 */
interface SecurityEventRepository {
    /**
     * Log a new security event
     */
    suspend fun logSecurityEvent(event: SecurityEvent): Boolean
    
    /**
     * Get all security events
     */
    suspend fun getAllSecurityEvents(): List<SecurityEvent>
    
    /**
     * Get security events by type
     */
    suspend fun getSecurityEventsByType(type: SecurityEventType): List<SecurityEvent>
    
    /**
     * Get security events between dates
     */
    suspend fun getSecurityEventsBetweenDates(startDate: Date, endDate: Date): List<SecurityEvent>
    
    /**
     * Mark a security event as handled
     */
    suspend fun markEventAsHandled(eventId: String): Boolean
    
    /**
     * Delete a security event
     */
    suspend fun deleteSecurityEvent(eventId: String): Boolean
    
    /**
     * Get security events as a Flow for reactive updates
     */
    fun getSecurityEventsFlow(): Flow<List<SecurityEvent>>
    
    /**
     * Get the latest security event
     */
    suspend fun getLatestSecurityEvent(): SecurityEvent?
    
    /**
     * Record an intruder detection event
     */
    suspend fun recordIntruderEvent(event: IntruderEvent): Boolean
    
    /**
     * Get all intruder events
     */
    suspend fun getIntruderEvents(): List<IntruderEvent>
    
    /**
     * Get intruder events as a Flow for reactive updates
     */
    fun getIntruderEventsFlow(): Flow<List<IntruderEvent>>
    
    /**
     * Delete an intruder event and its associated photo
     */
    suspend fun deleteIntruderEvent(eventId: String): Boolean
}
