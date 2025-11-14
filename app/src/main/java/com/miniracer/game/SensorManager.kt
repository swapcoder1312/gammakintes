package com.miniracer.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SensorManager - Handles accelerometer input for tilt steering.
 * Detects device tilt and converts it to steering input (-1.0 to 1.0).
 * Provides smooth tilt-based steering control for the game.
 */
class SensorManager(private val context: Context) : SensorEventListener {
    
    private val sensorManager: AndroidSensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? AndroidSensorManager
    
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val _tiltValue = MutableStateFlow(0f) // -1.0 (left) to 1.0 (right)
    val tiltValue: StateFlow<Float> = _tiltValue.asStateFlow()
    
    private var isEnabled = false
    private var sensitivity = 2.0f // Tilt sensitivity multiplier
    private var deadZone = 0.1f // Dead zone to prevent drift
    
    // Smoothing variables
    private val smoothingFactor = 0.8f
    private var smoothedTilt = 0f
    
    /**
     * Enables tilt steering by starting accelerometer sensor monitoring.
     */
    fun enable() {
        if (isEnabled) return
        
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(this, sensor, AndroidSensorManager.SENSOR_DELAY_GAME)
            isEnabled = true
        }
    }
    
    /**
     * Disables tilt steering by stopping accelerometer sensor monitoring.
     */
    fun disable() {
        if (!isEnabled) return
        
        sensorManager?.unregisterListener(this)
        isEnabled = false
        _tiltValue.value = 0f
        smoothedTilt = 0f
    }
    
    /**
     * Sets the sensitivity of tilt steering.
     * @param sensitivity Sensitivity multiplier (1.0 = normal, higher = more sensitive)
     */
    fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity.coerceIn(0.5f, 5.0f)
    }
    
    /**
     * Sets the dead zone to prevent unwanted steering input.
     * @param deadZone Dead zone value (0.0 to 1.0)
     */
    fun setDeadZone(deadZone: Float) {
        this.deadZone = deadZone.coerceIn(0f, 0.5f)
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        
        // Get X-axis tilt (device roll)
        // Negative X = tilt left, Positive X = tilt right
        val rawTilt = event.values[0] / 9.8f // Normalize by gravity
        
        // Apply sensitivity and clamp to [-1, 1]
        val adjustedTilt = (rawTilt * sensitivity).coerceIn(-1f, 1f)
        
        // Apply dead zone
        val tiltWithDeadZone = if (kotlin.math.abs(adjustedTilt) < deadZone) {
            0f
        } else {
            // Remove dead zone and rescale
            val sign = if (adjustedTilt > 0) 1f else -1f
            val magnitude = (kotlin.math.abs(adjustedTilt) - deadZone) / (1f - deadZone)
            sign * magnitude.coerceIn(0f, 1f)
        }
        
        // Apply smoothing to reduce jitter
        smoothedTilt = smoothedTilt * smoothingFactor + tiltWithDeadZone * (1f - smoothingFactor)
        
        _tiltValue.value = smoothedTilt
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }
    
    /**
     * Cleans up resources when the sensor manager is no longer needed.
     */
    fun dispose() {
        disable()
    }
}

