package com.lymors.phonesecure.presentation.utils

import android.content.Context
import android.content.Intent
import android.location.Location
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import com.lymors.phonesecure.domain.model.EmergencyContact
import com.lymors.phonesecure.presentation.services.IntruderDetectionService
import com.lymors.phonesecure.presentation.services.LocationTrackingService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for security-related functions in the PhoneSecure app
 */
object SecurityUtils {
    
    /**
     * Get the current SIM card number
     */
    fun getCurrentSimNumber(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.line1Number ?: "Unknown"
    }
    
    /**
     * Start location tracking service
     */
    fun startLocationTracking(context: Context) {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_LOCATION_TRACKING
        }
        context.startService(intent)
    }
    
    /**
     * Stop location tracking service
     */
    fun stopLocationTracking(context: Context) {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_LOCATION_TRACKING
        }
        context.startService(intent)
    }
    
    /**
     * Capture intruder photo
     */
    fun captureIntruderPhoto(context: Context, description: String) {
        val intent = Intent(context, IntruderDetectionService::class.java).apply {
            action = IntruderDetectionService.ACTION_CAPTURE_INTRUDER
            putExtra(IntruderDetectionService.EXTRA_DESCRIPTION, description)
        }
        context.startService(intent)
    }
    
    /**
     * Send SMS to emergency contacts
     */
    fun sendSmsToEmergencyContacts(
        context: Context,
        contacts: List<EmergencyContact>,
        message: String,
        location: Location? = null
    ) {
        if (PermissionUtils.hasSmsPermission(context)) {
            val smsManager = SmsManager.getDefault()
            
            // Add location information if available
            val fullMessage = if (location != null) {
                "$message\nLocation: https://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else {
                message
            }
            
            // Send SMS to each emergency contact
            contacts.forEach { contact ->
                try {
                    smsManager.sendTextMessage(
                        contact.phone,
                        null,
                        fullMessage,
                        null,
                        null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * Format a date for display
     */
    fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(date)
    }
    
    /**
     * Format a location for display
     */
    fun formatLocation(location: Location): String {
        return "Lat: ${location.latitude}, Lng: ${location.longitude}"
    }
}
