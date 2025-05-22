package com.lymors.phonesecure.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lymors.phonesecure.R
import com.lymors.phonesecure.domain.model.AntiTheftSettings
import com.lymors.phonesecure.domain.repository.UserRepository
import com.lymors.phonesecure.presentation.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AntiTheftService : Service(), SensorEventListener {

    @Inject
    lateinit var userRepository: UserRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastSimSerialNumber: String? = null
    private var settings: AntiTheftSettings? = null
    
    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == SMS_RECEIVED) {
                val pdus = intent.extras?.get("pdus") as Array<*>?
                pdus?.let { handleSmsReceived(it) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        // Register SMS receiver for remote lock
        registerReceiver(smsReceiver, IntentFilter(SMS_RECEIVED))
        
        // Load settings and initialize
        serviceScope.launch {
            settings = userRepository.getAntiTheftSettings()
            initializeProtection()
        }
    }

    private fun initializeProtection() {
        settings?.let { settings ->
            // Store current SIM serial number for later comparison
            if (settings.simChangeDetectionEnabled) {
                lastSimSerialNumber = getCurrentSimSerialNumber()
            }
            
            // Register motion sensor if enabled
            if (settings.motionDetectionEnabled && accelerometer != null) {
                sensorManager.registerListener(
                    this,
                    accelerometer,
                    getSensorDelay(settings.motionSensitivity)
                )
            }
        }
    }
    
    private fun getSensorDelay(sensitivity: Int): Int {
        return when {
            sensitivity <= 3 -> SensorManager.SENSOR_DELAY_NORMAL
            sensitivity <= 7 -> SensorManager.SENSOR_DELAY_GAME
            else -> SensorManager.SENSOR_DELAY_UI
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_SERVICE -> {
                    Log.d(TAG, "Starting anti-theft service")
                }
                ACTION_STOP_SERVICE -> {
                    Log.d(TAG, "Stopping anti-theft service")
                    stopSelf()
                }
                ACTION_UPDATE_SETTINGS -> {
                    serviceScope.launch {
                        settings = userRepository.getAntiTheftSettings()
                        updateProtectionSettings()
                    }
                }
            }
        }
        
        // Restart if killed
        return START_STICKY
    }

    private fun updateProtectionSettings() {
        settings?.let { settings ->
            // Update SIM detection
            if (settings.simChangeDetectionEnabled) {
                if (lastSimSerialNumber == null) {
                    lastSimSerialNumber = getCurrentSimSerialNumber()
                }
            } else {
                lastSimSerialNumber = null
            }
            
            // Update motion detection
            if (settings.motionDetectionEnabled && accelerometer != null) {
                sensorManager.registerListener(
                    this,
                    accelerometer,
                    getSensorDelay(settings.motionSensitivity)
                )
            } else {
                sensorManager.unregisterListener(this)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        try {
            unregisterReceiver(smsReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }

    // SensorEventListener implementation
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val settings = this.settings ?: return
            if (!settings.motionDetectionEnabled) return
            
            // Calculate acceleration
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            // Calculate magnitude of acceleration
            val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble())
            
            // Adjust threshold based on sensitivity (1-10)
            val threshold = 15 - settings.motionSensitivity
            
            if (acceleration > threshold) {
                // Motion detected
                sendMotionAlert()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    // Check for SIM change
    private fun checkSimChange() {
        val settings = this.settings ?: return
        if (!settings.simChangeDetectionEnabled) return
        
        val currentSimSerial = getCurrentSimSerialNumber()
        if (lastSimSerialNumber != null && currentSimSerial != lastSimSerialNumber) {
            // SIM changed
            sendSimChangeAlert()
        }
    }

    private fun getCurrentSimSerialNumber(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
                if (activeSubscriptionInfoList.isNotEmpty()) {
                    activeSubscriptionInfoList[0].iccId
                } else {
                    null
                }
            } else {
                val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                telephonyManager.simSerialNumber
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SIM serial number", e)
            null
        }
    }

    // Handle incoming SMS for remote lock
    private fun handleSmsReceived(pdus: Array<*>) {
        val settings = this.settings ?: return
        if (!settings.remoteLockEnabled || settings.secretCode.isBlank()) return
        
        for (pdu in pdus) {
            // Process SMS message and check for secret code
            // This is a simplified implementation
            // In a real app, you would parse the SMS message properly
            val message = "SMS message content here"
            if (message.contains(settings.secretCode)) {
                lockDevice()
                break
            }
        }
    }

    // Alert methods
    private fun sendSimChangeAlert() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.sim_changed_alert))
            .setContentText(getString(R.string.sim_change_description))
            .setSmallIcon(R.drawable.ic_security)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SIM_CHANGE_NOTIFICATION_ID, notification)
    }

    private fun sendMotionAlert() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.motion_detected_alert))
            .setContentText(getString(R.string.motion_detection_description))
            .setSmallIcon(R.drawable.ic_security)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(MOTION_NOTIFICATION_ID, notification)
    }

    private fun lockDevice() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.device_locked_remotely))
            .setContentText(getString(R.string.remote_lock_description))
            .setSmallIcon(R.drawable.ic_security)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(REMOTE_LOCK_NOTIFICATION_ID, notification)
        
        // In a real implementation, you would use the DevicePolicyManager to lock the device
        // This requires setting up a device admin application
    }

    // Notification setup
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Anti-Theft Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors device for security threats"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Anti-Theft Protection")
            .setContentText("Protecting your device")
            .setSmallIcon(R.drawable.ic_security)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "AntiTheftService"
        private const val CHANNEL_ID = "anti_theft_channel"
        private const val NOTIFICATION_ID = 1001
        private const val SIM_CHANGE_NOTIFICATION_ID = 1002
        private const val MOTION_NOTIFICATION_ID = 1003
        private const val REMOTE_LOCK_NOTIFICATION_ID = 1004
        
        const val ACTION_START_SERVICE = "com.lymors.phonesecure.START_ANTI_THEFT_SERVICE"
        const val ACTION_STOP_SERVICE = "com.lymors.phonesecure.STOP_ANTI_THEFT_SERVICE"
        const val ACTION_UPDATE_SETTINGS = "com.lymors.phonesecure.UPDATE_ANTI_THEFT_SETTINGS"
        
        private const val SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"
        
        fun startService(context: Context) {
            val intent = Intent(context, AntiTheftService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, AntiTheftService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
        
        fun updateSettings(context: Context) {
            val intent = Intent(context, AntiTheftService::class.java).apply {
                action = ACTION_UPDATE_SETTINGS
            }
            context.startService(intent)
        }
    }
}
