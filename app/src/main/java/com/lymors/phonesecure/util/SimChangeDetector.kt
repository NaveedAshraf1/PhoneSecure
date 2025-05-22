package com.lymors.phonesecure.util

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class to detect SIM card changes
 */
class SimChangeDetector(private val context: Context) {

    private val tag = "SimChangeDetector"
    private var lastKnownSimId: String? = null

    /**
     * Initialize the detector with the current SIM ID
     */
    fun initialize() {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            lastKnownSimId = getCurrentSimId()
            Log.d(tag, "Initialized with SIM ID: $lastKnownSimId")
        }
    }

    /**
     * Check if the SIM card has been changed
     * @return true if the SIM card has been changed, false otherwise
     */
    suspend fun hasSimChanged(): Boolean = withContext(Dispatchers.IO) {
        if (lastKnownSimId == null) {
            // Initialize if not already done
            initialize()
            return@withContext false
        }

        val currentSimId = getCurrentSimId()
        val changed = currentSimId != null && currentSimId != lastKnownSimId
        
        if (changed) {
            Log.d(tag, "SIM change detected! Previous: $lastKnownSimId, Current: $currentSimId")
        }
        
        return@withContext changed
    }

    /**
     * Update the last known SIM ID to the current one
     */
    suspend fun updateLastKnownSimId() = withContext(Dispatchers.IO) {
        lastKnownSimId = getCurrentSimId()
        Log.d(tag, "Updated last known SIM ID to: $lastKnownSimId")
    }

    /**
     * Get the current SIM ID
     * @return the current SIM ID, or null if not available
     */
    private fun getCurrentSimId(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
                if (activeSubscriptionInfoList.isNotEmpty()) {
                    activeSubscriptionInfoList[0].iccId
                } else {
                    null
                }
            } else {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                telephonyManager.simSerialNumber
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting SIM ID", e)
            null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SimChangeDetector? = null

        fun getInstance(context: Context): SimChangeDetector {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SimChangeDetector(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
