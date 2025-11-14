package com.miniracer.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

/**
 * OpponentCar - Represents an AI-controlled opponent car.
 * Handles autonomous movement, lane positioning, and rendering.
 */
class OpponentCar(
    var lane: Int = Random.nextInt(0, 3),
    var yOffset: Float = 0f
) {
    private var x: Float = 0f
    private var y: Float = yOffset
    private val carWidth = 80f
    private val carHeight = 120f
    
    private val lanes = listOf(200f, 400f, 600f)
    private val colors = listOf(Color.Red, Color.Green, Color.Yellow, Color.Magenta)
    private val color = colors[Random.nextInt(colors.size)]
    
    init {
        x = lanes[lane]
    }
    
    fun reset() {
        lane = Random.nextInt(0, 3)
        x = lanes[lane]
        y = -200f - Random.nextFloat() * 500f
    }
    
    fun update(speed: Float) {
        y += 5f * speed
    }
    
    fun render(drawScope: DrawScope) {
        val canvasWidth = drawScope.size.width
        val adjustedLanes = listOf(
            canvasWidth * 0.2f,
            canvasWidth * 0.5f,
            canvasWidth * 0.8f
        )
        
        val adjustedX = adjustedLanes[lane]
        val adjustedY = y
        
        // Draw car body
        drawScope.drawRect(
            color = color,
            topLeft = Offset(adjustedX - carWidth / 2, adjustedY - carHeight),
            size = Size(carWidth, carHeight)
        )
        
        // Draw car windows
        drawScope.drawRect(
            color = Color.DarkGray,
            topLeft = Offset(adjustedX - carWidth / 2 + 10f, adjustedY - carHeight + 20f),
            size = Size(carWidth - 20f, 40f)
        )
    }
    
    fun getBounds(): CarBounds {
        return CarBounds(
            x = lanes[lane] - carWidth / 2,
            y = y - carHeight,
            width = carWidth,
            height = carHeight
        )
    }
    
    fun getLane(): Int = lane
    fun getY(): Float = y
}

