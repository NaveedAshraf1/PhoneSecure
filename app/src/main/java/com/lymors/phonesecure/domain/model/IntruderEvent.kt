package com.lymors.phonesecure.domain.model

import java.util.Date

/**
 * Data class representing an intruder detection event
 */
data class IntruderEvent(
    val id: String,
    val timestamp: Date,
    val photoPath: String?,
    val location: Location?,
    val triggerType: TriggerType,
    val deviceInfo: DeviceInfo
) {
    enum class TriggerType {
        WRONG_PASSWORD,
        UNAUTHORIZED_MOVEMENT,
        SIM_CHANGE,
        DEVICE_UNLOCKED
    }
    
    data class Location(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float
    )
    
    data class DeviceInfo(
        val deviceId: String,
        val batteryLevel: Int,
        val isCharging: Boolean,
        val networkType: String,
        val simSerialNumber: String?
    )
}
