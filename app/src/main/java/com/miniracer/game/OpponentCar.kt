package com.miniracer.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Difficulty levels for opponent AI.
 */
enum class AIDifficulty {
    EASY,    // Slower, cautious, avoids risks
    MEDIUM,  // Balanced speed and aggression
    HARD    // Fast, aggressive, willing to risk collisions
}

/**
 * Waypoint for AI navigation.
 */
data class Waypoint(
    val position: Offset,
    val curveRadius: Float = Float.MAX_VALUE, // Larger = straighter, smaller = tighter curve
    val isStraight: Boolean = true,
    val speedLimit: Float = Float.MAX_VALUE // Optional speed limit for this waypoint
)

/**
 * OpponentCar - Represents an AI-controlled opponent car with waypoint-based navigation.
 * Uses deterministic physics and AI for reproducible behavior.
 */
class OpponentCar(
    var lane: Int = 0,
    var yOffset: Float = 0f,
    private val difficulty: AIDifficulty = AIDifficulty.MEDIUM,
    private val aiSeed: Long = 0L,
    private val waypoints: List<Waypoint> = emptyList()
) {
    // ============================================================================
    // DIFFICULTY-BASED PARAMETERS
    // ============================================================================
    
    private val difficultyParams = when (difficulty) {
        AIDifficulty.EASY -> DifficultyParams(
            maxSpeed = 300f,           // Slower max speed
            baseSpeed = 250f,          // Lower base speed
            reactionTime = 0.3f,       // Slower reaction (300ms delay)
            collisionRisk = 0.1f,      // Very low risk tolerance (10%)
            aggressiveness = 0.2f,     // Low overtaking aggression
            curveSlowdown = 0.7f       // More cautious in curves
        )
        AIDifficulty.MEDIUM -> DifficultyParams(
            maxSpeed = 450f,
            baseSpeed = 350f,
            reactionTime = 0.15f,      // Medium reaction (150ms delay)
            collisionRisk = 0.4f,      // Moderate risk tolerance (40%)
            aggressiveness = 0.5f,     // Moderate overtaking
            curveSlowdown = 0.85f
        )
        AIDifficulty.HARD -> DifficultyParams(
            maxSpeed = 600f,           // Faster max speed
            baseSpeed = 500f,          // Higher base speed
            reactionTime = 0.05f,      // Fast reaction (50ms delay)
            collisionRisk = 0.8f,      // High risk tolerance (80%)
            aggressiveness = 0.9f,     // Very aggressive overtaking
            curveSlowdown = 0.95f      // Less slowdown in curves
        )
    }
    
    private data class DifficultyParams(
        val maxSpeed: Float,
        val baseSpeed: Float,
        val reactionTime: Float,
        val collisionRisk: Float,
        val aggressiveness: Float,
        val curveSlowdown: Float
    )
    
    // ============================================================================
    // PHYSICS PARAMETERS
    // ============================================================================
    
    private var maxSpeed: Float = difficultyParams.maxSpeed
    private var accel: Float = 800f
    private var friction: Float = 0.96f
    private var grip: Float = 0.90f
    private var collisionSpeedLoss: Float = 0.5f
    private var collisionRecoveryTime: Float = 0.4f
    
    // ============================================================================
    // PHYSICS STATE
    // ============================================================================
    
    private var x: Float = 0f
    private var y: Float = yOffset
    private var rotation: Float = 0f
    
    // Velocity components
    private var velocityForward: Float = difficultyParams.baseSpeed
    private var velocityLateral: Float = 0f
    
    // AI state - Waypoint navigation
    private var currentWaypointIndex: Int = 0
    private var targetWaypoint: Waypoint? = null
    private var waypointReachedDistance: Float = 50f // Distance to consider waypoint reached
    
    // AI state - Overtaking and behavior
    private var targetLane: Int = lane
    private var laneChangeCooldown: Float = 0f
    private var reactionTimer: Float = 0f
    private var isOvertaking: Boolean = false
    private var overtakingTarget: OpponentCar? = null
    
    // AI state - Curve handling
    private var approachingCurve: Boolean = false
    private var curveSlowdownFactor: Float = 1f
    
    // Collision state
    private var collisionTimer: Float = 0f
    private var impactAnimationScale: Float = 1f
    
    // Deterministic random number generator
    private val random = Random(aiSeed)
    
    // Car dimensions
    private val carWidth = 80f
    private val carHeight = 120f
    
    private val lanes = listOf(200f, 400f, 600f)
    private val colors = listOf(Color.Red, Color.Green, Color.Yellow, Color.Magenta)
    private val color = colors[random.nextInt(colors.size)]
    
    init {
        x = lanes[lane]
        targetLane = lane
        updateTargetWaypoint()
    }
    
    /**
     * Resets the car to initial state.
     */
    fun reset() {
        lane = random.nextInt(0, 3)
        targetLane = lane
        x = lanes[lane]
        y = -200f - random.nextFloat() * 500f
        rotation = 0f
        velocityForward = difficultyParams.baseSpeed + random.nextFloat() * 50f
        velocityLateral = 0f
        collisionTimer = 0f
        impactAnimationScale = 1f
        laneChangeCooldown = 0f
        currentWaypointIndex = 0
        isOvertaking = false
        overtakingTarget = null
        approachingCurve = false
        curveSlowdownFactor = 1f
        reactionTimer = 0f
        updateTargetWaypoint()
    }
    
    /**
     * Updates the target waypoint based on current position.
     */
    private fun updateTargetWaypoint() {
        if (waypoints.isEmpty()) {
            targetWaypoint = null
            return
        }
        
        // Check if we've reached the current waypoint
        val currentWaypoint = waypoints.getOrNull(currentWaypointIndex)
        if (currentWaypoint != null) {
            val distanceToWaypoint = sqrt(
                (currentWaypoint.position.x - x).pow(2) + 
                (currentWaypoint.position.y - y).pow(2)
            )
            
            // In top-down view, Y increases downward, so "ahead" means higher Y
            // Check if we've passed the waypoint (y > waypoint.y) or are close enough
            if (distanceToWaypoint < waypointReachedDistance || y > currentWaypoint.position.y + 50f) {
                // Move to next waypoint
                currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.size
            }
        }
        
        // Find the next waypoint ahead
        targetWaypoint = null
        for (i in 0 until waypoints.size) {
            val index = (currentWaypointIndex + i) % waypoints.size
            val waypoint = waypoints[index]
            
            // In top-down view, waypoints ahead have higher Y values
            if (waypoint.position.y >= y - 100f) { // Allow some look-ahead
                targetWaypoint = waypoint
                currentWaypointIndex = index
                break
            }
        }
        
        // Fallback to current waypoint if none found ahead
        if (targetWaypoint == null) {
            targetWaypoint = waypoints.getOrNull(currentWaypointIndex)
        }
    }
    
    /**
     * Checks if there's a car ahead that we should overtake.
     */
    private fun checkOvertakingOpportunity(
        otherCars: List<OpponentCar>,
        playerCar: PlayerCar?
    ): OpponentCar? {
        if (!isOvertaking && laneChangeCooldown <= 0f) {
            // Check for cars ahead in same lane
            val carsAhead = mutableListOf<Pair<OpponentCar, Float>>()
            
            // Check other opponents
            otherCars.forEach { other ->
                if (other !== this && other.getLane() == lane) {
                    val distance = other.getY() - y
                    if (distance > 0 && distance < 300f) { // Ahead and within range
                        carsAhead.add(Pair(other, distance))
                    }
                }
            }
            
            // Check player if in same lane
            playerCar?.let { player ->
                if (player.getLane() == lane) {
                    val distance = player.getY() - y
                    if (distance > 0 && distance < 300f) {
                        // Treat player as slower car for overtaking decision
                        if (player.getVelocity() < velocityForward * 0.9f) {
                            // Consider overtaking player if they're slower
                            val targetLane = findOvertakingLane(otherCars, playerCar)
                            if (targetLane != null && random.nextFloat() < difficultyParams.aggressiveness) {
                                return null // Will trigger lane change
                            }
                        }
                    }
                }
            }
            
            // Find closest car ahead
            val closestCar = carsAhead.minByOrNull { it.second }
            
            if (closestCar != null) {
                val (car, distance) = closestCar
                // Only overtake if we're faster and on a straight section
                val isStraight = targetWaypoint?.isStraight == true
                val isFaster = velocityForward > car.getVelocity() * 1.1f
                
                if (isStraight && isFaster && random.nextFloat() < difficultyParams.aggressiveness) {
                    return car
                }
            }
        }
        
        return null
    }
    
    /**
     * Finds a suitable lane for overtaking.
     */
    private fun findOvertakingLane(
        otherCars: List<OpponentCar>,
        playerCar: PlayerCar?
    ): Int? {
        val currentLaneX = lanes[lane]
        val candidateLanes = listOf(0, 1, 2).filter { it != lane }
        
        for (candidateLane in candidateLanes) {
            val candidateX = lanes[candidateLane]
            var isClear = true
            
            // Check other opponents
            for (other in otherCars) {
                if (other !== this && other.getLane() == candidateLane) {
                    val distance = abs(other.getY() - y)
                    if (distance < 200f) { // Too close
                        isClear = false
                        break
                    }
                }
            }
            
            // Check player
            playerCar?.let { player ->
                if (player.getLane() == candidateLane) {
                    val distance = abs(player.getY() - y)
                    if (distance < 200f) {
                        isClear = false
                    }
                }
            }
            
            if (isClear) {
                return candidateLane
            }
        }
        
        return null
    }
    
    /**
     * Calculates speed adjustment for curves.
     */
    private fun calculateCurveSpeed(): Float {
        val waypoint = targetWaypoint ?: return 1f
        
        if (waypoint.isStraight) {
            approachingCurve = false
            curveSlowdownFactor = 1f
            return 1f
        }
        
        // Calculate distance to curve
        val distanceToCurve = sqrt(
            (waypoint.position.x - x).pow(2) + 
            (waypoint.position.y - y).pow(2)
        )
        
        // Start slowing down when approaching curve
        if (distanceToCurve < 200f) {
            approachingCurve = true
            // Tighter curves require more slowdown
            val curveTightness = 1f - (waypoint.curveRadius / 500f).coerceIn(0f, 1f)
            curveSlowdownFactor = difficultyParams.curveSlowdown * (1f - curveTightness * 0.3f)
        } else {
            approachingCurve = false
            curveSlowdownFactor = 1f
        }
        
        return curveSlowdownFactor
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
     * Updates car physics with waypoint-based AI behavior.
     * @param deltaTime Time since last update in seconds
     * @param gameSpeed Global game speed multiplier
     * @param otherCars List of other opponent cars for overtaking logic
     * @param playerCar Player car for overtaking logic
     */
    fun update(
        deltaTime: Float, 
        gameSpeed: Float = 1f,
        otherCars: List<OpponentCar> = emptyList(),
        playerCar: PlayerCar? = null
    ) {
        // Update collision animation
        if (collisionTimer > 0f) {
            collisionTimer -= deltaTime
            impactAnimationScale = 1f + (collisionTimer / collisionRecoveryTime) * 0.2f
            if (collisionTimer < 0f) {
                collisionTimer = 0f
                impactAnimationScale = 1f
            }
        }
        
        // Update reaction timer (simulates reaction delay)
        if (reactionTimer > 0f) {
            reactionTimer -= deltaTime
        }
        
        // Update waypoint target
        updateTargetWaypoint()
        
        // Calculate curve speed adjustment
        val curveSpeedFactor = calculateCurveSpeed()
        
        // Update lane change cooldown
        if (laneChangeCooldown > 0f) {
            laneChangeCooldown -= deltaTime
        }
        
        // Overtaking logic (only when reaction timer allows)
        if (reactionTimer <= 0f) {
            val overtakingTarget = checkOvertakingOpportunity(otherCars, playerCar)
            
            if (overtakingTarget != null) {
                this.overtakingTarget = overtakingTarget
                isOvertaking = true
                
                // Find a lane to overtake
                val overtakingLane = findOvertakingLane(otherCars, playerCar)
                if (overtakingLane != null) {
                    targetLane = overtakingLane
                    laneChangeCooldown = 3f // Prevent rapid lane changes
                    reactionTimer = difficultyParams.reactionTime // Reset reaction timer
                }
            } else if (isOvertaking) {
                // Check if we've passed the target
                overtakingTarget?.let { target ->
                    if (y > target.getY() + 100f) {
                        // Return to original lane or continue
                        if (random.nextFloat() < 0.7f) {
                            // Return to a more central lane
                            targetLane = 1
                            laneChangeCooldown = 2f
                        }
                        isOvertaking = false
                        this.overtakingTarget = null
                    }
                }
            }
        }
        
        // Waypoint-based steering
        var targetX = lanes[targetLane]
        if (targetWaypoint != null) {
            // Blend between lane position and waypoint position
            val waypointWeight = 0.6f // How much to follow waypoint vs lane
            targetX = targetX * (1f - waypointWeight) + targetWaypoint.position.x * waypointWeight
        }
        
        // Apply acceleration with curve slowdown
        if (collisionTimer <= 0f) {
            val effectiveMaxSpeed = maxSpeed * curveSpeedFactor * gameSpeed
            val targetSpeed = if (approachingCurve) {
                effectiveMaxSpeed * curveSlowdownFactor
            } else {
                effectiveMaxSpeed
            }
            
            if (velocityForward < targetSpeed) {
                velocityForward += accel * deltaTime
                velocityForward = min(velocityForward, targetSpeed)
            } else if (velocityForward > targetSpeed) {
                // Brake for curves
                velocityForward = max(velocityForward - accel * 1.5f * deltaTime, targetSpeed)
            } else {
                velocityForward *= friction
            }
        }
        
        // Calculate steering input based on target position
        val targetDistance = targetX - x
        val laneWidth = 200f
        val steeringInput = (targetDistance / laneWidth).coerceIn(-1f, 1f)
        
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
        if (abs(targetDistance) < 10f) {
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

