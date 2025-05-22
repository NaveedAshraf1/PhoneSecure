package com.lymors.phonesecure.domain.model

import java.util.Date

/**
 * Represents location data with timestamp for tracking device location history
 */
data class LocationData(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Date,
    val address: String? = null,
    val batteryLevel: Int? = null,
    val isFromEmergencyAlert: Boolean = false
)
