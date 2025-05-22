package com.lymors.phonesecure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lymors.phonesecure.domain.repository.UserRepository
import com.lymors.phonesecure.service.AntiTheftService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver to handle events related to anti-theft protection
 * such as boot completion and SIM state changes
 */
@AndroidEntryPoint
class AntiTheftReceiver : BroadcastReceiver() {

    @Inject
    lateinit var userRepository: UserRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received action: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Start the anti-theft service on device boot if enabled
                scope.launch {
                    val settings = userRepository.getAntiTheftSettings()
                    if (settings.simChangeDetectionEnabled || 
                        settings.motionDetectionEnabled || 
                        settings.remoteLockEnabled) {
                        AntiTheftService.startService(context)
                    }
                }
            }
            Intent.ACTION_SIM_STATE_CHANGED -> {
                // Handle SIM state changes
                scope.launch {
                    val settings = userRepository.getAntiTheftSettings()
                    if (settings.simChangeDetectionEnabled) {
                        // The service will handle the actual SIM change detection
                        // Just make sure the service is running
                        AntiTheftService.startService(context)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "AntiTheftReceiver"
    }
}
