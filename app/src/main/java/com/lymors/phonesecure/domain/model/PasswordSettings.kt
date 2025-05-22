package com.lymors.phonesecure.domain.model

/**
 * Data class representing password protection settings
 */
data class PasswordSettings(
    val passwordEnabled: Boolean = false,
    val password: String = "",
    val useBiometric: Boolean = false,
    val lockAfterTimeout: Boolean = false,
    val timeoutMinutes: Int = 5,
    val failedAttempts: Int = 0,
    val maxFailedAttempts: Int = 5,
    val lastFailedTimestamp: Long = 0L
)
