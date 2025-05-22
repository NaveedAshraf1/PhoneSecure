package com.lymors.phonesecure.util

import android.content.Context
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.getSystemService
import com.lymors.phonesecure.domain.model.IntruderEvent
import com.lymors.phonesecure.domain.repository.SecurityEventRepository
import com.lymors.phonesecure.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntruderDetectionManager @Inject constructor(
    private val context: Context,
    private val userRepository: UserRepository,
    private val securityEventRepository: SecurityEventRepository,
    private val cameraHelper: CameraHelper,
    private val locationHelper: LocationHelper
) {
    private val tag = "IntruderDetectionManager"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Record an intruder event with photo and location
     */
    fun recordIntruderEvent(triggerType: IntruderEvent.TriggerType) {
        coroutineScope.launch {
            try {
                // Get device info
                val deviceInfo = getDeviceInfo()
                
                // Take photo if camera is available
                val photoPath = try {
                    cameraHelper.takeFrontCameraPhoto()
                } catch (e: Exception) {
                    Log.e(tag, "Error taking photo", e)
                    null
                }
                
                // Get location if available
                val location = try {
                    locationHelper.getCurrentLocation()?.let { location ->
                        IntruderEvent.Location(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy
                        )
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error getting location", e)
                    null
                }
                
                // Create and save intruder event
                val event = IntruderEvent(
                    id = UUID.randomUUID().toString(),
                    timestamp = Date(),
                    photoPath = photoPath,
                    location = location,
                    triggerType = triggerType,
                    deviceInfo = deviceInfo
                )
                
                securityEventRepository.recordIntruderEvent(event)
                
                // Check if we should notify emergency contacts
                val settings = userRepository.getAntiTheftSettings()
                if (settings.notifyEmergencyContacts) {
                    notifyEmergencyContacts(event)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error recording intruder event", e)
            }
        }
    }
    
    private fun getDeviceInfo(): IntruderEvent.DeviceInfo {
        val batteryManager = context.getSystemService<BatteryManager>()
        val telephonyManager = context.getSystemService<TelephonyManager>()
        
        return IntruderEvent.DeviceInfo(
            deviceId = Build.SERIAL,
            batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1,
            isCharging = batteryManager?.isCharging ?: false,
            networkType = getNetworkType(telephonyManager),
            simSerialNumber = try {
                telephonyManager?.simSerialNumber
            } catch (e: SecurityException) {
                null
            }
        )
    }
    
    private fun getNetworkType(telephonyManager: TelephonyManager?): String {
        return when (telephonyManager?.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT -> "2G"
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> "Unknown"
        }
    }
    
    private suspend fun notifyEmergencyContacts(event: IntruderEvent) {
        try {
            val contacts = userRepository.getEmergencyContacts()
            val user = userRepository.getCurrentUser()
            
            contacts.forEach { contact ->
                // TODO: Implement SMS sending with location and photo
                // This will be implemented in the next step
            }
        } catch (e: Exception) {
            Log.e(tag, "Error notifying emergency contacts", e)
        }
    }
    
    companion object {
        private const val PHOTO_DIR = "intruder_photos"
    }
}
