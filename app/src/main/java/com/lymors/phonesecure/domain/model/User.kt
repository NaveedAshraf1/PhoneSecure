package com.lymors.phonesecure.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val securitySettings: SecuritySettings = SecuritySettings()
) : Parcelable

@Parcelize
data class EmergencyContact(
    val id: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val notifyOnSIMChange: Boolean = true,
    val notifyOnIntruderDetection: Boolean = true,
    val notifyOnPanicButton: Boolean = true
) : Parcelable

@Parcelize
data class SecuritySettings(
    val isFakeShutdownEnabled: Boolean = false,
    val isIntruderDetectionEnabled: Boolean = false,
    val isLocationTrackingEnabled: Boolean = false,
    val isSIMChangeAlertEnabled: Boolean = false,
    val isPanicButtonEnabled: Boolean = false,
    val isRemoteControlEnabled: Boolean = false,
    val lockDeviceOnSIMChange: Boolean = true,
    val capturePhotoOnWrongPassword: Boolean = true,
    val capturePhotoOnSIMChange: Boolean = true,
    val sendSMSOnSIMChange: Boolean = true,
    val sendEmailOnSIMChange: Boolean = true,
    val wrongPasswordAttempts: Int = 3
) : Parcelable
