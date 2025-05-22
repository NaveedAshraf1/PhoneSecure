package com.lymors.phonesecure.presentation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lymors.phonesecure.domain.repository.UserRepository
import com.lymors.phonesecure.presentation.services.LocationTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Broadcast receiver that handles device boot events
 * Used to restart security services when the device is restarted
 */
class BootReceiver : BroadcastReceiver() {
    
    private val receiverScope = CoroutineScope(Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Get application instance
            val app = context.applicationContext as com.lymors.phonesecure.PhoneSecureApp
            val userRepository = app.getUserRepository()
            
            // Check if location tracking was enabled before device restart
            receiverScope.launch {
                val settings = userRepository.getSecuritySettings()
                
                // Restart location tracking service if it was enabled
                if (settings.isLocationTrackingEnabled) {
                    val serviceIntent = Intent(context, LocationTrackingService::class.java).apply {
                        action = LocationTrackingService.ACTION_START_LOCATION_TRACKING
                    }
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}
