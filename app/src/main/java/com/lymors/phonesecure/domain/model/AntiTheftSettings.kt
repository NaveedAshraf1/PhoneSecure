package com.lymors.phonesecure.domain.model

/**
 * Data class representing anti-theft protection settings
 */
data class AntiTheftSettings(
    // SIM Change Detection
    val simChangeDetectionEnabled: Boolean = false,
    
    // Motion Detection
    val motionDetectionEnabled: Boolean = false,
    val motionSensitivity: Int = 5, // 1-10 scale
    
    // Wrong Password Detection
    val wrongPasswordDetectionEnabled: Boolean = false,
    val maxPasswordAttempts: Int = 3,
    
    // Remote Lock
    val remoteLockEnabled: Boolean = false,
    val secretCode: String = ""
)
