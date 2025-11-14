package com.miniracer.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Track - Manages the road/track rendering with lane markings.
 * Creates the illusion of movement through scrolling road elements.
 */
class Track {
    private var roadOffset: Float = 0f
    private val laneMarkingHeight = 50f
    private val laneMarkingGap = 30f
    
    fun update(speed: Float) {
        roadOffset += 5f * speed
        if (roadOffset > laneMarkingHeight + laneMarkingGap) {
            roadOffset = 0f
        }
    }
    
    fun render(drawScope: DrawScope) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        
        // Draw road background (gray)
        drawScope.drawRect(
            color = Color(0xFF333333),
            topLeft = Offset(width * 0.15f, 0f),
            size = androidx.compose.ui.geometry.Size(width * 0.7f, height)
        )
        
        // Draw lane dividers
        val lane1X = width * 0.4f
        val lane2X = width * 0.6f
        
        var y = roadOffset
        while (y < height) {
            // Left lane divider
            drawScope.drawRect(
                color = Color.White,
                topLeft = Offset(lane1X - 2f, y),
                size = androidx.compose.ui.geometry.Size(4f, laneMarkingHeight)
            )
            
            // Right lane divider
            drawScope.drawRect(
                color = Color.White,
                topLeft = Offset(lane2X - 2f, y),
                size = androidx.compose.ui.geometry.Size(4f, laneMarkingHeight)
            )
            
            y += laneMarkingHeight + laneMarkingGap
        }
        
        // Draw road edges
        drawScope.drawRect(
            color = Color.Yellow,
            topLeft = Offset(width * 0.15f, 0f),
            size = androidx.compose.ui.geometry.Size(4f, height)
        )
        drawScope.drawRect(
            color = Color.Yellow,
            topLeft = Offset(width * 0.85f - 4f, 0f),
            size = androidx.compose.ui.geometry.Size(4f, height)
        )
    }
}

