package com.lymors.phonesecure.presentation.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import com.lymors.phonesecure.R
import com.lymors.phonesecure.domain.model.SecurityEvent
import com.lymors.phonesecure.domain.model.SecurityEventType
import com.lymors.phonesecure.domain.repository.SecurityEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

/**
 * Accessibility service that enables the fake shutdown feature
 * This service overlays a black screen on top of the device to simulate shutdown
 * while keeping the device active for tracking and monitoring
 */
class FakeShutdownAccessibilityService : AccessibilityService() {
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayActive = false
    
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var securityEventRepository: SecurityEventRepository
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        // Initialize repositories
        securityEventRepository = (application as com.lymors.phonesecure.PhoneSecureApp).getSecurityEventRepository()
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle specific accessibility events for this feature
    }
    
    override fun onInterrupt() {
        // Service interrupted, remove overlay if active
        removeOverlay()
    }
    
    override fun onUnbind(intent: Intent?): Boolean {
        // Service unbound, remove overlay if active
        removeOverlay()
        return super.onUnbind(intent)
    }
    
    /**
     * Activate fake shutdown by showing a black overlay that covers the entire screen
     */
    fun activateFakeShutdown() {
        if (isOverlayActive) return
        
        // Create overlay view
        val inflater = LayoutInflater.from(this)
        val frame = FrameLayout(this)
        overlayView = inflater.inflate(R.layout.fake_shutdown_overlay, frame, false)
        
        // Set up window parameters
        val params = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            format = PixelFormat.OPAQUE
            gravity = Gravity.CENTER
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
        
        // Add the view to window manager
        try {
            windowManager?.addView(overlayView, params)
            isOverlayActive = true
            
            // Log security event
            serviceScope.launch {
                securityEventRepository.logSecurityEvent(
                    SecurityEvent(
                        id = UUID.randomUUID().toString(),
                        type = SecurityEventType.FAKE_SHUTDOWN_ACTIVATED,
                        timestamp = Date(),
                        description = "Fake shutdown was activated"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Deactivate fake shutdown by removing the overlay
     */
    fun deactivateFakeShutdown() {
        removeOverlay()
        
        // Log security event
        serviceScope.launch {
            securityEventRepository.logSecurityEvent(
                SecurityEvent(
                    id = UUID.randomUUID().toString(),
                    type = SecurityEventType.FAKE_SHUTDOWN_DEACTIVATED,
                    timestamp = Date(),
                    description = "Fake shutdown was deactivated"
                )
            )
        }
    }
    
    private fun removeOverlay() {
        if (!isOverlayActive) return
        
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
                isOverlayActive = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    companion object {
        private const val TAG = "FakeShutdownService"
        
        // Intent actions for controlling the service
        const val ACTION_ACTIVATE_FAKE_SHUTDOWN = "com.lymors.phonesecure.ACTION_ACTIVATE_FAKE_SHUTDOWN"
        const val ACTION_DEACTIVATE_FAKE_SHUTDOWN = "com.lymors.phonesecure.ACTION_DEACTIVATE_FAKE_SHUTDOWN"
    }
}
