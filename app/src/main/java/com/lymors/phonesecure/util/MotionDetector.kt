package com.lymors.phonesecure.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Utility class to detect device motion using the accelerometer
 */
class MotionDetector(private val context: Context) : SensorEventListener {

    private val tag = "MotionDetector"
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val _motionDetectedFlow = MutableStateFlow(false)
    val motionDetectedFlow: StateFlow<Boolean> = _motionDetectedFlow.asStateFlow()
    
    private var sensitivity: Int = 5 // Default sensitivity (1-10 scale)
    private var isMonitoring = false
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastUpdate = 0L
    private var lastMotionTime = 0L
    
    /**
     * Start monitoring for motion
     * @param sensitivity the sensitivity level (1-10, where 10 is most sensitive)
     */
    fun startMonitoring(sensitivity: Int = 5) {
        if (accelerometer == null) {
            Log.e(tag, "Accelerometer sensor not available on this device")
            return
        }
        
        this.sensitivity = sensitivity.coerceIn(1, 10)
        
        if (!isMonitoring) {
            sensorManager.registerListener(
                this,
                accelerometer,
                getSensorDelay(sensitivity)
            )
            isMonitoring = true
            Log.d(tag, "Started motion monitoring with sensitivity $sensitivity")
        } else {
            // Update sensitivity if already monitoring
            sensorManager.unregisterListener(this)
            sensorManager.registerListener(
                this,
                accelerometer,
                getSensorDelay(sensitivity)
            )
            Log.d(tag, "Updated motion monitoring sensitivity to $sensitivity")
        }
    }
    
    /**
     * Stop monitoring for motion
     */
    fun stopMonitoring() {
        if (isMonitoring) {
            sensorManager.unregisterListener(this)
            isMonitoring = false
            _motionDetectedFlow.value = false
            Log.d(tag, "Stopped motion monitoring")
        }
    }
    
    /**
     * Check if the detector is currently monitoring
     */
    fun isMonitoring(): Boolean = isMonitoring
    
    /**
     * Get the appropriate sensor delay based on sensitivity
     */
    private fun getSensorDelay(sensitivity: Int): Int {
        return when {
            sensitivity <= 3 -> SensorManager.SENSOR_DELAY_NORMAL
            sensitivity <= 7 -> SensorManager.SENSOR_DELAY_GAME
            else -> SensorManager.SENSOR_DELAY_UI
        }
    }
    
    /**
     * Calculate the threshold for motion detection based on sensitivity
     */
    private fun getMotionThreshold(): Float {
        // Higher sensitivity = lower threshold
        return (15 - sensitivity).toFloat()
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            
            // Don't process events too frequently
            if (currentTime - lastUpdate < 100) {
                return
            }
            
            lastUpdate = currentTime
            
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            // Skip the first reading
            if (lastX == 0f && lastY == 0f && lastZ == 0f) {
                lastX = x
                lastY = y
                lastZ = z
                return
            }
            
            // Calculate the change in acceleration
            val deltaX = Math.abs(lastX - x)
            val deltaY = Math.abs(lastY - y)
            val deltaZ = Math.abs(lastZ - z)
            
            // Calculate magnitude of change
            val deltaAcceleration = Math.sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()
            
            // Check if the change exceeds the threshold
            if (deltaAcceleration > getMotionThreshold()) {
                // Motion detected
                _motionDetectedFlow.value = true
                lastMotionTime = currentTime
                Log.d(tag, "Motion detected! Delta: $deltaAcceleration")
            } else if (_motionDetectedFlow.value && currentTime - lastMotionTime > MOTION_RESET_DELAY) {
                // Reset motion state after delay
                _motionDetectedFlow.value = false
            }
            
            // Update last values
            lastX = x
            lastY = y
            lastZ = z
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
    
    companion object {
        private const val MOTION_RESET_DELAY = 3000L // 3 seconds
        
        @Volatile
        private var INSTANCE: MotionDetector? = null
        
        fun getInstance(context: Context): MotionDetector {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MotionDetector(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
