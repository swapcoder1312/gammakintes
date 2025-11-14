package com.miniracer.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * OpponentCar - Represents an AI-controlled opponent car with simplified physics.
 * Uses lightweight physics model for autonomous movement.
 */
class OpponentCar(
    var lane: Int = Random.nextInt(0, 3),
    var yOffset: Float = 0f
) {
    // ============================================================================
    // PHYSICS PARAMETERS - Simplified for AI opponents
    // ============================================================================
    
    // Speed parameters
    private var maxSpeed: Float = 400f // Maximum forward velocity (pixels/second)
        // AI opponents typically slower than player
    
    private var accel: Float = 600f // Acceleration force (pixels/second²)
    
    private var friction: Float = 0.96f // Friction coefficient
    
    private var grip: Float = 0.90f // Lateral grip (AI cars have high grip for stability)
    
    // Collision response
    private var collisionSpeedLoss: Float = 0.5f // Speed reduction on collision
    
    private var collisionRecoveryTime: Float = 0.4f // Time to recover from collision
    
    // ============================================================================
    // PHYSICS STATE
    // ============================================================================
    
    private var x: Float = 0f
    private var y: Float = yOffset
    private var rotation: Float = 0f
    
    // Velocity components
    private var velocityForward: Float = 200f // Start with some speed
    private var velocityLateral: Float = 0f
    
    // AI state
    private var targetLane: Int = lane
    private var laneChangeCooldown: Float = 0f
    
    // Collision state
    private var collisionTimer: Float = 0f
    private var impactAnimationScale: Float = 1f
    
    // Car dimensions
    private val carWidth = 80f
    private val carHeight = 120f
    
    private val lanes = listOf(200f, 400f, 600f)
    private val colors = listOf(Color.Red, Color.Green, Color.Yellow, Color.Magenta)
    private val color = colors[Random.nextInt(colors.size)]
    
    init {
        x = lanes[lane]
        targetLane = lane
    }
    
    /**
     * Resets the car to initial state.
     */
    fun reset() {
        lane = Random.nextInt(0, 3)
        targetLane = lane
        x = lanes[lane]
        y = -200f - Random.nextFloat() * 500f
        rotation = 0f
        velocityForward = 150f + Random.nextFloat() * 100f
        velocityLateral = 0f
        collisionTimer = 0f
        impactAnimationScale = 1f
        laneChangeCooldown = 0f
    }
    
    /**
     * Applies collision response: reduces speed and triggers impact animation.
     */
    fun applyCollision(impactDirection: Offset = Offset(0f, 1f)) {
        // Reduce forward velocity
        velocityForward *= (1f - collisionSpeedLoss)
        velocityLateral *= (1f - collisionSpeedLoss * 0.5f)
        
        // Trigger impact animation
        collisionTimer = collisionRecoveryTime
        impactAnimationScale = 1.2f
        
        // Add some lateral velocity based on impact direction
        val impactAngle = kotlin.math.atan2(impactDirection.y, impactDirection.x)
        velocityLateral += sin(impactAngle) * velocityForward * 0.2f
    }
    
    /**
     * Updates car physics with simple AI behavior.
     * @param deltaTime Time since last update in seconds
     * @param gameSpeed Global game speed multiplier
     */
    fun update(deltaTime: Float, gameSpeed: Float = 1f) {
        // Update collision animation
        if (collisionTimer > 0f) {
            collisionTimer -= deltaTime
            impactAnimationScale = 1f + (collisionTimer / collisionRecoveryTime) * 0.2f
            if (collisionTimer < 0f) {
                collisionTimer = 0f
                impactAnimationScale = 1f
            }
        }
        
        // Update lane change cooldown
        if (laneChangeCooldown > 0f) {
            laneChangeCooldown -= deltaTime
        }
        
        // Simple AI: occasionally change lanes
        if (laneChangeCooldown <= 0f && Random.nextFloat() < 0.001f) {
            val newLane = Random.nextInt(0, 3)
            if (newLane != lane) {
                targetLane = newLane
                laneChangeCooldown = 2f + Random.nextFloat() * 3f
            }
        }
        
        // Apply acceleration (AI maintains speed)
        if (collisionTimer <= 0f) {
            val targetSpeed = maxSpeed * gameSpeed
            if (velocityForward < targetSpeed) {
                velocityForward += accel * deltaTime
                velocityForward = min(velocityForward, targetSpeed)
            } else {
                velocityForward *= friction
            }
        }
        
        // Simple lane following with smooth movement
        val targetX = lanes[targetLane]
        val laneDistance = targetX - x
        val laneWidth = 200f
        
        // Calculate steering input based on lane distance
        val steeringInput = (laneDistance / laneWidth).coerceIn(-1f, 1f)
        
        // Apply lateral movement based on steering
        if (abs(steeringInput) > 0.01f && velocityForward > 10f) {
            val lateralAccel = steeringInput * grip * 300f
            velocityLateral += lateralAccel * deltaTime
            
            // Update rotation slightly for visual effect
            rotation += steeringInput * velocityForward * 0.0001f
            rotation = rotation.coerceIn(-0.2f, 0.2f) // Limit rotation
        } else {
            // Return to straight
            rotation *= 0.95f
        }
        
        // Apply lateral friction
        velocityLateral *= 0.9f
        
        // Limit lateral velocity
        val maxLateralSpeed = velocityForward * 0.3f
        velocityLateral = velocityLateral.coerceIn(-maxLateralSpeed, maxLateralSpeed)
        
        // Update position
        x += velocityLateral * deltaTime
        y += velocityForward * deltaTime * gameSpeed
        
        // Update lane when close to target
        if (abs(laneDistance) < 10f) {
            lane = targetLane
        }
    }
    
    /**
     * Renders the car.
     */
    fun render(drawScope: DrawScope) {
        val canvasWidth = drawScope.size.width
        val adjustedLanes = listOf(
            canvasWidth * 0.2f,
            canvasWidth * 0.5f,
            canvasWidth * 0.8f
        )
        
        val adjustedX = adjustedLanes[lane] + (x - lanes[lane])
        val adjustedY = y
        
        // Apply impact animation scale
        val scale = impactAnimationScale
        val scaledWidth = carWidth * scale
        val scaledHeight = carHeight * scale
        
        // Draw car body (simplified, no rotation for AI cars)
        drawScope.drawRect(
            color = color,
            topLeft = Offset(adjustedX - scaledWidth / 2, adjustedY - scaledHeight),
            size = Size(scaledWidth, scaledHeight)
        )
        
        // Draw car windows
        drawScope.drawRect(
            color = Color.DarkGray,
            topLeft = Offset(adjustedX - scaledWidth / 2 + 10f, adjustedY - scaledHeight + 20f),
            size = Size(scaledWidth - 20f, 40f)
        )
    }
    
    /**
     * Gets car bounds for collision detection.
     */
    fun getBounds(): CarBounds {
        return CarBounds(
            x = x - carWidth / 2,
            y = y - carHeight,
            width = carWidth,
            height = carHeight
        )
    }
    
    /**
     * Gets current lane.
     */
    fun getLane(): Int = lane
    
    /**
     * Gets Y position.
     */
    fun getY(): Float = y
    
    /**
     * Gets X position.
     */
    fun getX(): Float = x
    
    /**
     * Gets forward velocity.
     */
    fun getVelocity(): Float = velocityForward
}

