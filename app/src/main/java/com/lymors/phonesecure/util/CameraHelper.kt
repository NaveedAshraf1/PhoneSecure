package com.lymors.phonesecure.util

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.getSystemService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class CameraHelper @Inject constructor(
    private val context: Context
) {
    private val tag = "CameraHelper"
    private val backgroundThread = HandlerThread("CameraBackground").apply { start() }
    private val backgroundHandler = Handler(backgroundThread.looper)

    suspend fun takeFrontCameraPhoto(): String? = suspendCoroutine { continuation ->
        try {
            val cameraManager = context.getSystemService<CameraManager>() ?: throw Exception("Camera service not available")
            
            // Find front camera
            val frontCameraId = cameraManager.cameraIdList.find { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: throw Exception("Front camera not found")
            
            // Create output file
            val photoFile = createPhotoFile()
            
            // Setup image reader
            val characteristics = cameraManager.getCameraCharacteristics(frontCameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: throw Exception("Cannot get camera configurations")
            
            val largest = map.getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }
                ?: throw Exception("Cannot get output sizes")
            
            val imageReader = ImageReader.newInstance(
                largest.width, largest.height,
                ImageFormat.JPEG, 1
            ).apply {
                setOnImageAvailableListener({ reader ->
                    try {
                        val image = reader.acquireLatestImage()
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes)
                        photoFile.outputStream().use { it.write(bytes) }
                        image.close()
                        continuation.resume(photoFile.absolutePath)
                    } catch (e: Exception) {
                        Log.e(tag, "Error saving photo", e)
                        continuation.resumeWithException(e)
                    } finally {
                        reader.close()
                    }
                }, backgroundHandler)
            }
            
            // Open camera
            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    try {
                        // Create capture session
                        val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                            addTarget(imageReader.surface)
                            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                        }
                        
                        camera.createCaptureSession(listOf(imageReader.surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    try {
                                        session.capture(captureBuilder.build(), null, backgroundHandler)
                                    } catch (e: Exception) {
                                        Log.e(tag, "Error capturing photo", e)
                                        continuation.resumeWithException(e)
                                    }
                                }
                                
                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    val error = Exception("Failed to configure camera session")
                                    Log.e(tag, error.message, error)
                                    continuation.resumeWithException(error)
                                }
                            }, backgroundHandler)
                    } catch (e: Exception) {
                        Log.e(tag, "Error creating capture session", e)
                        continuation.resumeWithException(e)
                    }
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    Log.d(tag, "Camera disconnected")
                    camera.close()
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    val errorMsg = "Camera error: $error"
                    Log.e(tag, errorMsg)
                    camera.close()
                    continuation.resumeWithException(Exception(errorMsg))
                }
            }, backgroundHandler)
            
        } catch (e: Exception) {
            Log.e(tag, "Error taking photo", e)
            continuation.resumeWithException(e)
        }
    }
    
    private fun createPhotoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val photoDir = File(context.filesDir, "intruder_photos").apply { mkdirs() }
        return File(photoDir, "IMG_$timestamp.jpg")
    }
    
    fun cleanup() {
        backgroundThread.quitSafely()
    }
}
