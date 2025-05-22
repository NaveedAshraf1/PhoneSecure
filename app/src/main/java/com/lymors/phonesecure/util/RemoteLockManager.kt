package com.lymors.phonesecure.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Utility class to manage remote locking functionality
 */
class RemoteLockManager(private val context: Context) {

    private val tag = "RemoteLockManager"
    private var secretCode: String = ""
    
    /**
     * Set the secret code for remote locking
     */
    fun setSecretCode(code: String) {
        secretCode = code
        Log.d(tag, "Secret code set")
    }
    
    /**
     * Check if the provided message contains the secret code
     * @return true if the message contains the secret code, false otherwise
     */
    fun isRemoteLockMessage(message: String): Boolean {
        if (secretCode.isBlank()) {
            Log.d(tag, "Secret code is blank, remote lock not possible")
            return false
        }
        
        val containsCode = message.contains(secretCode)
        if (containsCode) {
            Log.d(tag, "Remote lock message detected")
        }
        
        return containsCode
    }
    
    /**
     * Lock the device if the device admin permission is granted
     * @return true if the device was locked, false otherwise
     */
    fun lockDevice(): Boolean {
        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponentName = ComponentName(context, "com.lymors.phonesecure.receiver.DeviceAdminReceiver")
            
            if (devicePolicyManager.isAdminActive(adminComponentName)) {
                // Lock the device
                devicePolicyManager.lockNow()
                Log.d(tag, "Device locked successfully")
                return true
            } else {
                Log.e(tag, "Device admin permission not granted")
                return false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error locking device", e)
            return false
        }
    }
    
    /**
     * Check if the device admin permission is granted
     * @return true if the permission is granted, false otherwise
     */
    fun isDeviceAdminActive(): Boolean {
        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponentName = ComponentName(context, "com.lymors.phonesecure.receiver.DeviceAdminReceiver")
            return devicePolicyManager.isAdminActive(adminComponentName)
        } catch (e: Exception) {
            Log.e(tag, "Error checking device admin status", e)
            return false
        }
    }
    
    /**
     * Open the device admin settings to request permission
     */
    fun requestDeviceAdminPermission() {
        try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, 
                ComponentName(context, "com.lymors.phonesecure.receiver.DeviceAdminReceiver"))
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "Device admin permission is required for remote locking functionality")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(tag, "Opened device admin settings")
        } catch (e: Exception) {
            Log.e(tag, "Error opening device admin settings", e)
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: RemoteLockManager? = null
        
        fun getInstance(context: Context): RemoteLockManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RemoteLockManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
