package com.miniracer.game

import android.content.Context
import android.content.SharedPreferences

/**
 * SaveManager - Manages persistent game data using SharedPreferences.
 * Handles saving/loading high scores and game settings.
 */
class SaveManager(private val context: Context? = null) {
    private val prefsName = "MiniRacerPrefs"
    private val highScoreKey = "high_score"
    
    private fun getSharedPreferences(): SharedPreferences? {
        return context?.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }
    
    /**
     * Saves the high score if it's greater than the current saved score.
     * @param score The score to save
     */
    fun saveHighScore(score: Int) {
        val currentHighScore = getHighScore()
        if (score > currentHighScore) {
            getSharedPreferences()?.edit()?.putInt(highScoreKey, score)?.apply()
        }
    }
    
    /**
     * Retrieves the saved high score.
     * @return The high score, or 0 if none exists
     */
    fun getHighScore(): Int {
        return getSharedPreferences()?.getInt(highScoreKey, 0) ?: 0
    }
    
    /**
     * Clears all saved data.
     */
    fun clearData() {
        getSharedPreferences()?.edit()?.clear()?.apply()
    }
}

