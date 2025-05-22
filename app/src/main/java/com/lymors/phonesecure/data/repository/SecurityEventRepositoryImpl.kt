package com.lymors.phonesecure.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lymors.phonesecure.PhoneSecureApp
import com.lymors.phonesecure.domain.model.SecurityEvent
import com.lymors.phonesecure.domain.model.SecurityEventType
import com.lymors.phonesecure.domain.repository.SecurityEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Implementation of SecurityEventRepository that uses SharedPreferences for storage
 */
class SecurityEventRepositoryImpl : SecurityEventRepository {
    
    private val context: Context = PhoneSecureApp.getAppContext()
    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _securityEventsFlow = MutableStateFlow<List<SecurityEvent>>(emptyList())
    
    init {
        // Initialize events flow with stored events
        val events = getAllSecurityEventsSync()
        _securityEventsFlow.value = events
    }
    
    override suspend fun logSecurityEvent(event: SecurityEvent): Boolean = withContext(Dispatchers.IO) {
        val events = getAllSecurityEventsSync().toMutableList()
        events.add(event)
        val success = saveSecurityEvents(events)
        if (success) {
            _securityEventsFlow.value = events
        }
        return@withContext success
    }
    
    override suspend fun getAllSecurityEvents(): List<SecurityEvent> = withContext(Dispatchers.IO) {
        return@withContext getAllSecurityEventsSync()
    }
    
    override suspend fun getSecurityEventsByType(type: SecurityEventType): List<SecurityEvent> = withContext(Dispatchers.IO) {
        val events = getAllSecurityEventsSync()
        return@withContext events.filter { it.type == type }
    }
    
    override suspend fun getSecurityEventsBetweenDates(startDate: Date, endDate: Date): List<SecurityEvent> = withContext(Dispatchers.IO) {
        val events = getAllSecurityEventsSync()
        return@withContext events.filter { it.timestamp in startDate..endDate }
    }
    
    override suspend fun markEventAsHandled(eventId: String): Boolean = withContext(Dispatchers.IO) {
        val events = getAllSecurityEventsSync().toMutableList()
        val index = events.indexOfFirst { it.id == eventId }
        if (index != -1) {
            val event = events[index]
            events[index] = event.copy(isHandled = true)
            val success = saveSecurityEvents(events)
            if (success) {
                _securityEventsFlow.value = events
            }
            return@withContext success
        }
        return@withContext false
    }
    
    override suspend fun deleteSecurityEvent(eventId: String): Boolean = withContext(Dispatchers.IO) {
        val events = getAllSecurityEventsSync().toMutableList()
        val filtered = events.filter { it.id != eventId }
        if (filtered.size != events.size) {
            val success = saveSecurityEvents(filtered)
            if (success) {
                _securityEventsFlow.value = filtered
            }
            return@withContext success
        }
        return@withContext false
    }
    
    override fun getSecurityEventsFlow(): Flow<List<SecurityEvent>> = _securityEventsFlow.asStateFlow()
    
    override suspend fun getLatestSecurityEvent(): SecurityEvent? = withContext(Dispatchers.IO) {
        val events = getAllSecurityEventsSync()
        return@withContext events.maxByOrNull { it.timestamp }
    }
    
    private fun getAllSecurityEventsSync(): List<SecurityEvent> {
        val eventsJson = prefs.getString(KEY_SECURITY_EVENTS, null)
        if (eventsJson != null) {
            val type = object : TypeToken<List<SecurityEvent>>() {}.type
            return gson.fromJson(eventsJson, type)
        }
        return emptyList()
    }
    
    private fun saveSecurityEvents(events: List<SecurityEvent>): Boolean {
        val eventsJson = gson.toJson(events)
        return prefs.edit().putString(KEY_SECURITY_EVENTS, eventsJson).commit()
    }
    
    companion object {
        private const val PREFS_NAME = "phone_secure_prefs"
        private const val KEY_SECURITY_EVENTS = "security_events"
    }
}
