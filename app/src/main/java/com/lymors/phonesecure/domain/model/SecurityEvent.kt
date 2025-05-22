package com.lymors.phonesecure.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Represents a security event that occurred on the device
 */
@Parcelize
data class SecurityEvent(
    val id: String,
    val type: SecurityEventType,
    val timestamp: Date,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoPath: String? = null,
    val audioPath: String? = null,
    val description: String = "",
    val isHandled: Boolean = false
) : Parcelable

/**
 * Types of security events that can be detected by the app
 */
enum class SecurityEventType {
    SIM_CHANGE,
    WRONG_PASSWORD_ATTEMPT,
    INTRUDER_DETECTED,
    DEVICE_UNLOCKED,
    PANIC_BUTTON_PRESSED,
    DEVICE_LOCATION_CHANGED,
    DEVICE_POWERED_OFF,
    DEVICE_POWERED_ON,
    FAKE_SHUTDOWN_ACTIVATED,
    FAKE_SHUTDOWN_DEACTIVATED
}
