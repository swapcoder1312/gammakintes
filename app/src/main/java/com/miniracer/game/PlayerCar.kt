package com.miniracer.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * PlayerCar - Represents the player's controllable car entity.
 * Handles movement, position, and rendering of the player vehicle.
 */
class PlayerCar {
    private var x: Float = 0f
    private var y: Float = 0f
    private var lane: Int = 1 // 0 = left, 1 = center, 2 = right
    private val carWidth = 80f
    private val carHeight = 120f
    private val laneWidth = 200f
    private val screenWidth = 1080f // Will be set from canvas
    
    private val lanes = listOf(200f, 400f, 600f) // Lane X positions
    
    init {
        reset()
    }
    
    fun reset() {
        lane = 1
        x = lanes[lane]
        y = 1800f // Bottom Y position (car bottom coordinate)
    }
    
    fun moveLeft() {
        if (lane > 0) {
            lane--
        }
    }
    
    fun moveRight() {
        if (lane < 2) {
            lane++
        }
    }
    
    /**
     * Sets the target lane for smooth steering (used for tilt control).
     * @param targetLane Desired lane (0-2), clamped to valid range
     */
    fun setLaneTarget(targetLane: Int) {
        val clampedLane = targetLane.coerceIn(0, 2)
        if (kotlin.math.abs(clampedLane - lane) <= 1) {
            lane = clampedLane
        }
    }
    
    fun update() {
        // Smoothly move to target lane
        val targetX = lanes[lane]
        x += (targetX - x) * 0.2f
        // Keep Y at bottom position
        y = 1800f
    }
    
    fun render(drawScope: DrawScope) {
        val canvasWidth = drawScope.size.width
        val canvasHeight = drawScope.size.height
        
        // Adjust lanes based on canvas width
        val adjustedLanes = listOf(
            canvasWidth * 0.2f,
            canvasWidth * 0.5f,
            canvasWidth * 0.8f
        )
        
        val adjustedX = adjustedLanes[lane] + (adjustedLanes[lane] - x) * 0.2f
        val adjustedY = canvasHeight - 200f
        
        // Draw car body
        drawScope.drawRect(
            color = Color.Blue,
            topLeft = Offset(adjustedX - carWidth / 2, adjustedY - carHeight),
            size = Size(carWidth, carHeight)
        )
        
        // Draw car windows
        drawScope.drawRect(
            color = Color.Cyan,
            topLeft = Offset(adjustedX - carWidth / 2 + 10f, adjustedY - carHeight + 20f),
            size = Size(carWidth - 20f, 40f)
        )
    }
    
    fun getBounds(): CarBounds {
        val adjustedLanes = listOf(200f, 400f, 600f)
        return CarBounds(
            x = adjustedLanes[lane] - carWidth / 2,
            y = y - carHeight,
            width = carWidth,
            height = carHeight
        )
    }
    
    fun getLane(): Int = lane
    fun getY(): Float = y
}

data class CarBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

