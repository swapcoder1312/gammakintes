package com.miniracer.game

import android.content.Context
import android.content.SharedPreferences

/**
 * SettingsManager - Manages game settings using SharedPreferences.
 * Handles persistence of user preferences including tilt control, sound, and other settings.
 */
class SettingsManager(private val context: Context) {
    private val prefsName = "MiniRacerSettings"
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    
    // Setting keys
    private val keyTiltEnabled = "tilt_enabled"
    private val keySoundEnabled = "sound_enabled"
    private val keyTiltSensitivity = "tilt_sensitivity"
    private val keyTiltDeadZone = "tilt_dead_zone"
    
    // Default values
    private val defaultTiltEnabled = false
    private val defaultSoundEnabled = true
    private val defaultTiltSensitivity = 2.0f
    private val defaultTiltDeadZone = 0.1f
    
    /**
     * Gets whether tilt steering is enabled.
     */
    fun isTiltEnabled(): Boolean {
        return prefs.getBoolean(keyTiltEnabled, defaultTiltEnabled)
    }
    
    /**
     * Sets whether tilt steering is enabled.
     */
    fun setTiltEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(keyTiltEnabled, enabled).apply()
    }
    
    /**
     * Gets whether sound is enabled.
     */
    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean(keySoundEnabled, defaultSoundEnabled)
    }
    
    /**
     * Sets whether sound is enabled.
     */
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(keySoundEnabled, enabled).apply()
    }
    
    /**
     * Gets tilt sensitivity.
     */
    fun getTiltSensitivity(): Float {
        return prefs.getFloat(keyTiltSensitivity, defaultTiltSensitivity)
    }
    
    /**
     * Sets tilt sensitivity.
     */
    fun setTiltSensitivity(sensitivity: Float) {
        prefs.edit().putFloat(keyTiltSensitivity, sensitivity).apply()
    }
    
    /**
     * Gets tilt dead zone.
     */
    fun getTiltDeadZone(): Float {
        return prefs.getFloat(keyTiltDeadZone, defaultTiltDeadZone)
    }
    
    /**
     * Sets tilt dead zone.
     */
    fun setTiltDeadZone(deadZone: Float) {
        prefs.edit().putFloat(keyTiltDeadZone, deadZone).apply()
    }
    
    /**
     * Resets all settings to default values.
     */
    fun resetToDefaults() {
        prefs.edit()
            .putBoolean(keyTiltEnabled, defaultTiltEnabled)
            .putBoolean(keySoundEnabled, defaultSoundEnabled)
            .putFloat(keyTiltSensitivity, defaultTiltSensitivity)
            .putFloat(keyTiltDeadZone, defaultTiltDeadZone)
            .apply()
    }
}

