package com.miniracer.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * SpriteLoader - Utility class for loading and caching game sprites/assets.
 * Handles bitmap loading from resources and conversion to Compose ImageBitmap.
 */
class SpriteLoader(private val context: Context) {
    private val spriteCache = mutableMapOf<String, ImageBitmap>()
    
    /**
     * Loads a sprite from resources and caches it.
     * @param resourceId The Android resource ID of the image
     * @param key Cache key for the sprite
     * @return ImageBitmap or null if loading fails
     */
    fun loadSprite(resourceId: Int, key: String): ImageBitmap? {
        if (spriteCache.containsKey(key)) {
            return spriteCache[key]
        }
        
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            val imageBitmap = bitmap.asImageBitmap()
            spriteCache[key] = imageBitmap
            imageBitmap
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Clears the sprite cache to free memory.
     */
    fun clearCache() {
        spriteCache.clear()
    }
    
    /**
     * Gets a cached sprite by key.
     */
    fun getSprite(key: String): ImageBitmap? {
        return spriteCache[key]
    }
}

