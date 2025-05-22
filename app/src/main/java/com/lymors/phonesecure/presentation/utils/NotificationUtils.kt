package com.lymors.phonesecure.presentation.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lymors.phonesecure.R
import com.lymors.phonesecure.presentation.ui.MainActivity

/**
 * Utility class for handling notifications in the PhoneSecure app
 */
object NotificationUtils {
    
    private const val CHANNEL_ID_SECURITY = "security_channel"
    private const val CHANNEL_ID_LOCATION = "location_channel"
    
    private const val NOTIFICATION_ID_SECURITY = 1001
    private const val NOTIFICATION_ID_LOCATION = 1002
    private const val NOTIFICATION_ID_INTRUDER = 1003
    private const val NOTIFICATION_ID_SIM_CHANGE = 1004
    
    /**
     * Create notification channels required for the app
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the security alerts channel
            val securityChannel = NotificationChannel(
                CHANNEL_ID_SECURITY,
                context.getString(R.string.notification_channel_security),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important security alerts from PhoneSecure"
                enableVibration(true)
            }
            
            // Create the location tracking channel
            val locationChannel = NotificationChannel(
                CHANNEL_ID_LOCATION,
                context.getString(R.string.notification_channel_location),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Location tracking status notifications"
                setShowBadge(false)
            }
            
            // Register the channels with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(securityChannel, locationChannel))
        }
    }
    
    /**
     * Show a security alert notification
     */
    fun showSecurityAlertNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SECURITY)
            .setSmallIcon(R.drawable.ic_security)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SECURITY, builder.build())
    }
    
    /**
     * Show an intruder detection notification
     */
    fun showIntruderDetectionNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SECURITY)
            .setSmallIcon(R.drawable.ic_security)
            .setContentTitle(context.getString(R.string.notification_intruder_detected))
            .setContentText(context.getString(R.string.alert_intruder_detected))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_INTRUDER, builder.build())
    }
    
    /**
     * Show a SIM change notification
     */
    fun showSimChangeNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SECURITY)
            .setSmallIcon(R.drawable.ic_security)
            .setContentTitle(context.getString(R.string.notification_sim_changed))
            .setContentText(context.getString(R.string.alert_sim_changed))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SIM_CHANGE, builder.build())
    }
    
    /**
     * Create a foreground service notification for location tracking
     */
    fun createLocationTrackingNotification(context: Context): androidx.core.app.NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID_LOCATION)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notification_location_tracking))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
    }
}
