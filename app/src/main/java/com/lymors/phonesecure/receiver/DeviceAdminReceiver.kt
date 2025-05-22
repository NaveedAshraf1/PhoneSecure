package com.lymors.phonesecure.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.lymors.phonesecure.R

/**
 * Receiver for device administrator events
 * Required for remote lock functionality
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {

    private val tag = "DeviceAdminReceiver"

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(tag, "Device admin enabled")
        Toast.makeText(context, R.string.device_admin_enabled, Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(tag, "Device admin disabled")
        Toast.makeText(context, R.string.device_admin_disabled, Toast.LENGTH_SHORT).show()
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Log.i(tag, "Password attempt failed")
        // This could be used to implement the wrong password detection feature
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        Log.i(tag, "Password attempt succeeded")
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.i(tag, "Lock task mode entering")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.i(tag, "Lock task mode exiting")
    }
}
