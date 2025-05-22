package com.lymors.phonesecure.presentation.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.guolindev.permissionx.PermissionX
import com.lymors.phonesecure.R

/**
 * Utility class for handling permissions in the PhoneSecure app
 */
object PermissionUtils {
    
    /**
     * Check if all the required permissions for the anti-theft features are granted
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        val requiredPermissions = getRequiredPermissions()
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get a list of all required permissions for the anti-theft features
     */
    fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
        
        // Add background location permission for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        
        return permissions
    }
    
    /**
     * Request all required permissions using PermissionX library
     */
    fun requestRequiredPermissions(activity: Activity, callback: (Boolean) -> Unit) {
        val permissionBuilder = PermissionX.init(activity)
            .permissions(getRequiredPermissions())
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    activity.getString(R.string.permission_rationale_message),
                    activity.getString(R.string.action_continue),
                    activity.getString(R.string.action_cancel)
                )
            }
        
        // Special handling for background location on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionBuilder.explainReasonBeforeRequest()
        }
        
        permissionBuilder.request { allGranted, _, _ ->
            callback(allGranted)
        }
    }
    
    /**
     * Check if the overlay permission is granted (needed for fake shutdown feature)
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Overlay permission is automatically granted on older Android versions
        }
    }
    
    /**
     * Open the overlay permission settings screen
     */
    fun openOverlayPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    }
    
    /**
     * Check if the app has permission to send SMS
     */
    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request SMS permission
     */
    fun requestSmsPermission(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.SEND_SMS),
            requestCode
        )
    }
}
