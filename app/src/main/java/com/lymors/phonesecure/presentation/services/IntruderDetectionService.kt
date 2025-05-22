package com.lymors.phonesecure.presentation.services

import android.app.Service
import android.content.Intent
import android.hardware.Camera
import android.os.IBinder
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.lymors.phonesecure.domain.model.SecurityEvent
import com.lymors.phonesecure.domain.model.SecurityEventType
import com.lymors.phonesecure.domain.repository.LocationRepository
import com.lymors.phonesecure.domain.repository.SecurityEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Service for detecting intruders by capturing photos when triggered
 */
@Suppress("DEPRECATION") // Using Camera API for wider device compatibility
class IntruderDetectionService : Service(), SurfaceHolder.Callback {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var camera: Camera? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var surfaceView: SurfaceView? = null
    
    private lateinit var securityEventRepository: SecurityEventRepository
    private lateinit var locationRepository: LocationRepository
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize repositories
        securityEventRepository = (application as com.lymors.phonesecure.PhoneSecureApp).getSecurityEventRepository()
        locationRepository = (application as com.lymors.phonesecure.PhoneSecureApp).getLocationRepository()
        
        // Initialize surface view for camera preview
        surfaceView = SurfaceView(this)
        surfaceHolder = surfaceView?.holder
        surfaceHolder?.addCallback(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CAPTURE_INTRUDER -> captureIntruder(intent.getStringExtra(EXTRA_DESCRIPTION) ?: "Intruder detected")
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun captureIntruder(description: String) {
        try {
            if (camera == null) {
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
            }
            
            camera?.let { cam ->
                try {
                    cam.setPreviewDisplay(surfaceHolder)
                    cam.startPreview()
                    
                    cam.takePicture(null, null) { data, _ ->
                        val photoFile = savePhoto(data)
                        logIntruderEvent(photoFile?.absolutePath, description)
                        
                        // Release camera resources
                        cam.stopPreview()
                        cam.release()
                        camera = null
                        
                        // Stop service after capturing photo
                        stopSelf()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    logIntruderEvent(null, "$description (Failed to capture photo)")
                    stopSelf()
                }
            } ?: run {
                logIntruderEvent(null, "$description (Failed to access camera)")
                stopSelf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logIntruderEvent(null, "$description (Error: ${e.message})")
            stopSelf()
        }
    }
    
    private fun savePhoto(data: ByteArray): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "INTRUDER_$timeStamp.jpg"
            val photoDir = File(filesDir, "intruder_photos")
            
            if (!photoDir.exists()) {
                photoDir.mkdirs()
            }
            
            val photoFile = File(photoDir, fileName)
            FileOutputStream(photoFile).use { fos ->
                fos.write(data)
            }
            
            photoFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun logIntruderEvent(photoPath: String?, description: String) {
        serviceScope.launch {
            // Get current location if available
            val location = locationRepository.getCurrentLocation()
            
            securityEventRepository.logSecurityEvent(
                SecurityEvent(
                    id = UUID.randomUUID().toString(),
                    type = SecurityEventType.INTRUDER_DETECTED,
                    timestamp = Date(),
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    photoPath = photoPath,
                    description = description
                )
            )
        }
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        // Surface created, camera can be initialized
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Surface changed, adjust camera if needed
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface destroyed, release camera resources
        camera?.stopPreview()
        camera?.release()
        camera = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        camera?.release()
        camera = null
    }
    
    companion object {
        // Intent actions for controlling the service
        const val ACTION_CAPTURE_INTRUDER = "com.lymors.phonesecure.ACTION_CAPTURE_INTRUDER"
        
        // Intent extras
        const val EXTRA_DESCRIPTION = "extra_description"
    }
}
