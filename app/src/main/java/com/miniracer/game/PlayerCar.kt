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

/**
 * PlayerCar - Represents the player's controllable car entity with physics-based movement.
 * Implements velocity, acceleration, friction, lateral grip, and drift mechanics.
 */
class PlayerCar {
    // ============================================================================
    // PHYSICS PARAMETERS - Tune these for different gameplay feels
    // ============================================================================
    
    // ARCADE FEEL (default): High grip, fast acceleration, forgiving physics
    // SIMULATION FEEL: Lower grip, realistic acceleration, more challenging
    
    // Speed parameters
    private var maxSpeed: Float = 800f // Maximum forward velocity (pixels/second)
        // Arcade: 800-1200, Simulation: 400-600
    
    private var accel: Float = 1200f // Acceleration force (pixels/second²)
        // Arcade: 1000-1500, Simulation: 400-800
    
    private var brakeForce: Float = 2000f // Braking/deceleration force (pixels/second²)
        // Arcade: 1500-2500, Simulation: 800-1500
    
    // Friction and grip
    private var friction: Float = 0.95f // Friction coefficient (0-1, higher = less friction)
        // Arcade: 0.92-0.97, Simulation: 0.85-0.92
    
    private var grip: Float = 0.85f // Lateral grip coefficient (0-1, higher = more grip)
        // Arcade: 0.80-0.90, Simulation: 0.60-0.75
    
    // Drift and slip
    private var driftFactor: Float = 0.3f // Drift/slip factor (0-1, higher = more drift)
        // Arcade: 0.2-0.4, Simulation: 0.4-0.6
    
    private var minTurnRadius: Float = 150f // Minimum turning radius at max speed (pixels)
        // Arcade: 100-200, Simulation: 200-400
    
    // Collision response
    private var collisionSpeedLoss: Float = 0.4f // Speed reduction on collision (0-1)
        // Arcade: 0.3-0.5, Simulation: 0.5-0.7
    
    private var collisionRecoveryTime: Float = 0.5f // Time to recover from collision (seconds)
    
    // ============================================================================
    // PHYSICS STATE
    // ============================================================================
    
    // Position and orientation
    private var x: Float = 0f
    private var y: Float = 0f
    private var rotation: Float = 0f // Rotation angle in radians
    
    // Velocity components
    private var velocityForward: Float = 0f // Forward velocity (along car's forward direction)
    private var velocityLateral: Float = 0f // Lateral velocity (perpendicular to forward)
    
    // Input state
    private var isAccelerating: Boolean = false
    private var isBraking: Boolean = false
    private var steeringInput: Float = 0f // -1.0 (left) to 1.0 (right)
    
    // Collision state
    private var collisionTimer: Float = 0f
    private var impactAnimationScale: Float = 1f
    
    // Car dimensions
    private val carWidth = 80f
    private val carHeight = 120f
    
    // Lane-based positioning (for compatibility)
    private var lane: Int = 1 // 0 = left, 1 = center, 2 = right
    private val lanes = listOf(200f, 400f, 600f) // Lane X positions
    
    init {
        reset()
    }
    
    /**
     * Resets the car to initial state.
     */
    fun reset() {
        x = lanes[1]
        y = 1800f // Bottom Y position
        rotation = 0f
        velocityForward = 200f // Start with some initial speed
        velocityLateral = 0f
        isAccelerating = false
        isBraking = false
        steeringInput = 0f
        collisionTimer = 0f
        impactAnimationScale = 1f
        lane = 1
    }
    
    /**
     * Sets acceleration input.
     */
    fun setAccelerating(accelerating: Boolean) {
        isAccelerating = accelerating
    }
    
    /**
     * Sets braking input.
     */
    fun setBraking(braking: Boolean) {
        isBraking = braking
    }
    
    /**
     * Sets steering input (-1.0 to 1.0).
     */
    fun setSteering(steering: Float) {
        steeringInput = steering.coerceIn(-1f, 1f)
    }
    
    /**
     * Legacy lane-based movement (for compatibility).
     */
    fun moveLeft() {
        if (lane > 0) {
            lane--
            setSteering(-1f)
        }
    }
    
    fun moveRight() {
        if (lane < 2) {
            lane++
            setSteering(1f)
        }
    }
    
    /**
     * Applies collision response: reduces speed and triggers impact animation.
     */
    fun applyCollision(impactDirection: Offset = Offset(0f, -1f)) {
        // Reduce forward velocity
        velocityForward *= (1f - collisionSpeedLoss)
        velocityLateral *= (1f - collisionSpeedLoss * 0.5f)
        
        // Trigger impact animation
        collisionTimer = collisionRecoveryTime
        impactAnimationScale = 1.2f
        
        // Add some lateral velocity based on impact direction
        val impactAngle = kotlin.math.atan2(impactDirection.y, impactDirection.x)
        velocityLateral += sin(impactAngle) * velocityForward * 0.3f
    }
    
    /**
     * Updates car physics based on input and time delta.
     * @param deltaTime Time since last update in seconds
     */
    fun update(deltaTime: Float) {
        // Update collision animation
        if (collisionTimer > 0f) {
            collisionTimer -= deltaTime
            impactAnimationScale = 1f + (collisionTimer / collisionRecoveryTime) * 0.2f
            if (collisionTimer < 0f) {
                collisionTimer = 0f
                impactAnimationScale = 1f
            }
        }
        
        // Apply acceleration or braking
        if (isAccelerating && collisionTimer <= 0f) {
            velocityForward += accel * deltaTime
            velocityForward = min(velocityForward, maxSpeed)
        } else if (isBraking) {
            velocityForward -= brakeForce * deltaTime
            velocityForward = max(velocityForward, 0f)
        } else {
            // Apply friction when not accelerating
            velocityForward *= friction
        }
        
        // Calculate turning radius based on speed
        // Higher speed = larger turning radius (less grip)
        val speedRatio = velocityForward / maxSpeed
        val effectiveGrip = grip * (1f - speedRatio * driftFactor)
        val turnRadius = minTurnRadius / (effectiveGrip + 0.1f)
        
        // Apply lateral movement based on steering and speed
        if (abs(steeringInput) > 0.01f && velocityForward > 10f) {
            // Calculate lateral acceleration based on turning radius
            val lateralAccel = (velocityForward * velocityForward) / turnRadius * steeringInput
            velocityLateral += lateralAccel * deltaTime * effectiveGrip
            
            // Update rotation based on steering
            val rotationSpeed = (velocityForward / turnRadius) * steeringInput
            rotation += rotationSpeed * deltaTime
        } else {
            // Gradually return to straight
            rotation *= 0.95f
        }
        
        // Apply lateral friction (drift behavior)
        val lateralFriction = 1f - (1f - effectiveGrip) * 0.1f
        velocityLateral *= lateralFriction
        
        // Limit lateral velocity based on forward speed
        val maxLateralSpeed = velocityForward * (1f - effectiveGrip) * 0.5f
        velocityLateral = velocityLateral.coerceIn(-maxLateralSpeed, maxLateralSpeed)
        
        // Update position based on velocity
        val forwardX = cos(rotation)
        val forwardY = sin(rotation)
        val lateralX = -sin(rotation)
        val lateralY = cos(rotation)
        
        x += (forwardX * velocityForward + lateralX * velocityLateral) * deltaTime
        y += (forwardY * velocityForward + lateralY * velocityLateral) * deltaTime
        
        // Keep Y at bottom position (for top-down racing style)
        y = 1800f
        
        // Update lane based on X position (for compatibility)
        val canvasWidth = 1080f // Default, will be adjusted in render
        val laneWidth = canvasWidth / 3f
        lane = ((x / laneWidth).toInt()).coerceIn(0, 2)
        
        // Gradually reduce steering input (return to center)
        steeringInput *= 0.9f
    }
    
    /**
     * Renders the car with physics-based position and rotation.
     */
    fun render(drawScope: DrawScope) {
        val canvasWidth = drawScope.size.width
        val canvasHeight = drawScope.size.height
        
        // Adjust lanes based on canvas width
        val adjustedLanes = listOf(
            canvasWidth * 0.2f,
            canvasWidth * 0.5f,
            canvasWidth * 0.8f
        )
        
        // Use physics-based position or fallback to lane-based
        val renderX = if (abs(velocityForward) > 1f) {
            x.coerceIn(canvasWidth * 0.1f, canvasWidth * 0.9f)
        } else {
            adjustedLanes[lane]
        }
        val renderY = canvasHeight - 200f
        
        // Apply impact animation scale
        val scale = impactAnimationScale
        val scaledWidth = carWidth * scale
        val scaledHeight = carHeight * scale
        
        // Calculate rotated corners for car body (simplified rotation visualization)
        val cosR = cos(rotation)
        val sinR = sin(rotation)
        val halfWidth = scaledWidth / 2f
        val halfHeight = scaledHeight / 2f
        
        // Rotated corners
        val topLeft = Offset(
            renderX + (-halfWidth * cosR - (-halfHeight) * sinR),
            renderY - scaledHeight + (-halfWidth * sinR + (-halfHeight) * cosR)
        )
        val topRight = Offset(
            renderX + (halfWidth * cosR - (-halfHeight) * sinR),
            renderY - scaledHeight + (halfWidth * sinR + (-halfHeight) * cosR)
        )
        val bottomLeft = Offset(
            renderX + (-halfWidth * cosR - halfHeight * sinR),
            renderY + (-halfWidth * sinR + halfHeight * cosR)
        )
        val bottomRight = Offset(
            renderX + (halfWidth * cosR - halfHeight * sinR),
            renderY + (halfWidth * sinR + halfHeight * cosR)
        )
        
        // Draw car body as rotated rectangle (using path)
        val carPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(topLeft.x, topLeft.y)
            lineTo(topRight.x, topRight.y)
            lineTo(bottomRight.x, bottomRight.y)
            lineTo(bottomLeft.x, bottomLeft.y)
            close()
        }
        drawScope.drawPath(carPath, Color.Blue)
        
        // Draw car windows (simplified, centered)
        val windowWidth = scaledWidth - 20f
        val windowHeight = 40f
        val windowPath = androidx.compose.ui.graphics.Path().apply {
            val wTopLeft = Offset(
                renderX + (-windowWidth / 2 * cosR - (-scaledHeight / 2 + 20f) * sinR),
                renderY - scaledHeight / 2 + 20f + (-windowWidth / 2 * sinR + (-scaledHeight / 2 + 20f) * cosR)
            )
            val wTopRight = Offset(
                renderX + (windowWidth / 2 * cosR - (-scaledHeight / 2 + 20f) * sinR),
                renderY - scaledHeight / 2 + 20f + (windowWidth / 2 * sinR + (-scaledHeight / 2 + 20f) * cosR)
            )
            val wBottomLeft = Offset(
                renderX + (-windowWidth / 2 * cosR - (-scaledHeight / 2 + 20f + windowHeight) * sinR),
                renderY - scaledHeight / 2 + 20f + windowHeight + (-windowWidth / 2 * sinR + (-scaledHeight / 2 + 20f + windowHeight) * cosR)
            )
            val wBottomRight = Offset(
                renderX + (windowWidth / 2 * cosR - (-scaledHeight / 2 + 20f + windowHeight) * sinR),
                renderY - scaledHeight / 2 + 20f + windowHeight + (windowWidth / 2 * sinR + (-scaledHeight / 2 + 20f + windowHeight) * cosR)
            )
            moveTo(wTopLeft.x, wTopLeft.y)
            lineTo(wTopRight.x, wTopRight.y)
            lineTo(wBottomRight.x, wBottomRight.y)
            lineTo(wBottomLeft.x, wBottomLeft.y)
            close()
        }
        drawScope.drawPath(windowPath, Color.Cyan)
        
        // Draw drift indicator (when lateral velocity is high) - simplified as side marker
        if (abs(velocityLateral) > 50f) {
            val driftAlpha = (abs(velocityLateral) / 200f).coerceIn(0f, 0.5f)
            val driftSide = if (velocityLateral > 0) 1f else -1f
            val driftOffsetX = driftSide * (halfWidth + 5f) * cosR
            val driftOffsetY = driftSide * (halfWidth + 5f) * sinR
            drawScope.drawRect(
                color = Color.Red.copy(alpha = driftAlpha),
                topLeft = Offset(renderX + driftOffsetX - 2.5f, renderY - scaledHeight / 2),
                size = Size(5f, scaledHeight)
            )
        }
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
     * Gets current forward velocity.
     */
    fun getVelocity(): Float = velocityForward
    
    /**
     * Gets current lane (for compatibility).
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
     * Gets rotation angle in radians.
     */
    fun getRotation(): Float = rotation
}

data class CarBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

