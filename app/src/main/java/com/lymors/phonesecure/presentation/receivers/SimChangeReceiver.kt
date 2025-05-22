package com.lymors.phonesecure.presentation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.lymors.phonesecure.domain.usecase.SimChangeUseCase
import com.lymors.phonesecure.presentation.services.IntruderDetectionService
import com.lymors.phonesecure.presentation.services.LocationTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Broadcast receiver that handles SIM card change events
 * This is a critical component for anti-theft protection
 */
class SimChangeReceiver : BroadcastReceiver() {
    
    private val receiverScope = CoroutineScope(Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED) {
            val simState = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE, TelephonyManager.SIM_STATE_UNKNOWN)
            
            if (simState == TelephonyManager.SIM_STATE_READY) {
                // SIM card is ready, check if it's a new SIM
                handlePossibleSimChange(context)
            }
        }
    }
    
    private fun handlePossibleSimChange(context: Context) {
        // Get application instance
        val app = context.applicationContext as com.lymors.phonesecure.PhoneSecureApp
        val simChangeUseCase = app.getSimChangeUseCase()
        
        receiverScope.launch {
            // Check if SIM change detection is enabled
            if (simChangeUseCase.isSimChangeDetectionEnabled()) {
                // Get the current SIM card number
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val newSimNumber = telephonyManager.line1Number ?: "Unknown"
                
                // Start location tracking
                val locationIntent = Intent(context, LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.ACTION_START_LOCATION_TRACKING
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(locationIntent)
                } else {
                    context.startService(locationIntent)
                }
                
                // Capture intruder photo if enabled
                if (simChangeUseCase.shouldCapturePhotoOnSimChange()) {
                    val intruderIntent = Intent(context, IntruderDetectionService::class.java).apply {
                        action = IntruderDetectionService.ACTION_CAPTURE_INTRUDER
                        putExtra(
                            IntruderDetectionService.EXTRA_DESCRIPTION,
                            "SIM card changed to $newSimNumber"
                        )
                    }
                    context.startService(intruderIntent)
                }
                
                // Log the SIM change event
                simChangeUseCase.handleSimChange(newSimNumber)
                
                // Send alerts to emergency contacts
                val emergencyContacts = simChangeUseCase.getEmergencyContactsToNotify()
                if (emergencyContacts.isNotEmpty()) {
                    // Send SMS if enabled
                    if (simChangeUseCase.shouldSendSmsOnSimChange()) {
                        sendSmsToEmergencyContacts(context, emergencyContacts, newSimNumber)
                    }
                    
                    // Send email if enabled
                    if (simChangeUseCase.shouldSendEmailOnSimChange()) {
                        sendEmailToEmergencyContacts(context, emergencyContacts, newSimNumber)
                    }
                }
            }
        }
    }
    
    private fun sendSmsToEmergencyContacts(context: Context, emergencyContacts: List<com.lymors.phonesecure.domain.model.EmergencyContact>, newSimNumber: String) {
        // This would be implemented with SmsManager to send SMS messages
        // For now, we'll just log the intent to send SMS
        android.util.Log.d(TAG, "Would send SMS to ${emergencyContacts.size} contacts about SIM change to $newSimNumber")
    }
    
    private fun sendEmailToEmergencyContacts(context: Context, emergencyContacts: List<com.lymors.phonesecure.domain.model.EmergencyContact>, newSimNumber: String) {
        // This would be implemented with JavaMail or similar to send emails
        // For now, we'll just log the intent to send emails
        android.util.Log.d(TAG, "Would send email to ${emergencyContacts.size} contacts about SIM change to $newSimNumber")
    }
    
    companion object {
        private const val TAG = "SimChangeReceiver"
    }
}
