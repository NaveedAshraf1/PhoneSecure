package com.lymors.phonesecure.presentation.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lymors.phonesecure.R
import com.lymors.phonesecure.domain.model.SecurityEvent
import com.lymors.phonesecure.domain.model.SecurityEventType
import com.lymors.phonesecure.domain.repository.LocationRepository
import com.lymors.phonesecure.domain.repository.SecurityEventRepository
import com.lymors.phonesecure.presentation.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

/**
 * Service for tracking device location in the background
 * This is a foreground service to ensure it keeps running even when the app is in the background
 */
class LocationTrackingService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var locationJob: Job? = null
    
    private lateinit var locationRepository: LocationRepository
    private lateinit var securityEventRepository: SecurityEventRepository
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize repositories
        locationRepository = (application as com.lymors.phonesecure.PhoneSecureApp).getLocationRepository()
        securityEventRepository = (application as com.lymors.phonesecure.PhoneSecureApp).getSecurityEventRepository()
        
        // Create notification channel for foreground service
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOCATION_TRACKING -> startLocationTracking()
            ACTION_STOP_LOCATION_TRACKING -> stopLocationTracking()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startLocationTracking() {
        // Start as foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start location tracking
        serviceScope.launch {
            locationRepository.startLocationTracking()
            
            // Log security event
            securityEventRepository.logSecurityEvent(
                SecurityEvent(
                    id = UUID.randomUUID().toString(),
                    type = SecurityEventType.DEVICE_LOCATION_CHANGED,
                    timestamp = Date(),
                    description = "Location tracking was started"
                )
            )
        }
        
        // Collect location updates
        locationJob = locationRepository.getLocationUpdates()
            .onEach { location ->
                handleLocationUpdate(location)
            }
            .catch { e ->
                e.printStackTrace()
            }
            .launchIn(serviceScope)
    }
    
    private fun stopLocationTracking() {
        // Cancel location updates
        locationJob?.cancel()
        locationJob = null
        
        // Stop location tracking
        serviceScope.launch {
            locationRepository.stopLocationTracking()
            
            // Log security event
            securityEventRepository.logSecurityEvent(
                SecurityEvent(
                    id = UUID.randomUUID().toString(),
                    type = SecurityEventType.DEVICE_LOCATION_CHANGED,
                    timestamp = Date(),
                    description = "Location tracking was stopped"
                )
            )
        }
        
        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun handleLocationUpdate(location: Location) {
        // Save location to history
        serviceScope.launch {
            locationRepository.saveLocationHistory(location, Date())
        }
    }
    
    private fun createNotificationChannel() {
        // Notification channels are now created in the PhoneSecureApp class
        // using NotificationUtils.createNotificationChannels()
    }
    
    private fun createNotification(): Notification {
        return com.lymors.phonesecure.presentation.utils.NotificationUtils
            .createLocationTrackingNotification(this)
            .build()
    }
    
    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1001
        
        // Intent actions for controlling the service
        const val ACTION_START_LOCATION_TRACKING = "com.lymors.phonesecure.ACTION_START_LOCATION_TRACKING"
        const val ACTION_STOP_LOCATION_TRACKING = "com.lymors.phonesecure.ACTION_STOP_LOCATION_TRACKING"
    }
}
