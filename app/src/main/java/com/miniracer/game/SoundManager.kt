package com.miniracer.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SoundManager - Manages game sounds and audio effects.
 * Handles sound playback, volume control, and sound on/off toggle.
 */
class SoundManager(private val context: Context) {
    
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<String, Int>()
    private var isEnabled = true
    private var masterVolume = 1.0f
    
    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()
    
    init {
        initializeSoundPool()
    }
    
    /**
     * Initializes the SoundPool for audio playback.
     */
    private fun initializeSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()
        
        // Load sound effects here when available
        // Example: soundIds["engine"] = soundPool?.load(context, R.raw.engine, 1)
    }
    
    /**
     * Enables sound playback.
     */
    fun enable() {
        isEnabled = true
        _isSoundEnabled.value = true
    }
    
    /**
     * Disables sound playback.
     */
    fun disable() {
        isEnabled = false
        _isSoundEnabled.value = false
    }
    
    /**
     * Plays a sound effect.
     * @param soundKey The key of the sound to play
     * @param volume Volume (0.0 to 1.0)
     */
    fun playSound(soundKey: String, volume: Float = 1.0f) {
        if (!isEnabled) return
        
        val soundId = soundIds[soundKey] ?: return
        soundPool?.play(
            soundId,
            volume * masterVolume,
            volume * masterVolume,
            1,
            0,
            1.0f
        )
    }
    
    /**
     * Sets the master volume.
     * @param volume Volume (0.0 to 1.0)
     */
    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }
    
    /**
     * Loads a sound from resources.
     * @param soundKey Key to identify the sound
     * @param resourceId Android resource ID of the sound file
     */
    fun loadSound(soundKey: String, resourceId: Int) {
        soundIds[soundKey] = soundPool?.load(context, resourceId, 1) ?: 0
    }
    
    /**
     * Cleans up resources when the sound manager is no longer needed.
     */
    fun dispose() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
    }
}

