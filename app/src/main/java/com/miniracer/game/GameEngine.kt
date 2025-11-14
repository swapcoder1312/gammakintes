package com.miniracer.game

import android.content.Context
import android.view.Choreographer
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GameEngine - Core game loop manager that coordinates all game entities.
 * Implements a fixed-timestep game loop (60 FPS target) with state machine.
 * Handles game state, updates, rendering, and input processing.
 * 
 * UPDATE AND RENDER ORDER:
 * 1. Choreographer callback fires (syncs with display refresh rate)
 * 2. Game loop accumulates delta time
 * 3. Fixed timestep updates are executed (physics, AI, collisions)
 * 4. Render callback is triggered (draws current state to canvas)
 * 
 * This ensures consistent game logic regardless of frame rate variations.
 */
class GameEngine(private val context: Context? = null) {
    
    /**
     * Game state machine representing different phases of the game lifecycle.
     */
    enum class EngineState {
        LOADING,    // Initial loading state
        RUNNING,    // Game is actively running
        PAUSED,     // Game is paused (can be resumed)
        GAME_OVER   // Game has ended (requires restart)
    }
    
    /**
     * Player input types for handling user interaction.
     */
    enum class Input {
        LEFT, RIGHT, ACCELERATE, RELEASE_LEFT, RELEASE_RIGHT, RELEASE_ACCELERATE
    }
    
    // Game entities
    private val playerCar = PlayerCar()
    private val track = Track(context)
    private val opponents = mutableListOf<OpponentCar>()
    private val collisionDetector = CollisionDetector()
    private val saveManager = SaveManager(context)
    
    // State management
    private val _engineState = MutableStateFlow(EngineState.LOADING)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()
    
    private val _gameState = MutableStateFlow(GameStateData())
    val gameState: StateFlow<GameStateData> = _gameState.asStateFlow()
    
    // Input state (for continuous input)
    private var isSteeringLeft = false
    private var isSteeringRight = false
    private var isAccelerating = false
    private var tiltSteeringValue = 0f // -1.0 (left) to 1.0 (right)
    
    // Game loop timing - Fixed timestep at 60 FPS
    private val targetFPS = 60
    private val targetFrameTimeNs = 1_000_000_000L / targetFPS // ~16.67ms in nanoseconds
    private val targetFrameTimeMs = targetFrameTimeNs / 1_000_000.0 // ~16.67ms in milliseconds
    private val fixedTimeStep = targetFrameTimeMs / 1000.0 // Convert to seconds (~0.0167s)
    
    private var choreographer: Choreographer? = null
    private var frameCallback: Choreographer.FrameCallback? = null
    private var gameLoopJob: Job? = null
    private val gameScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Timing variables for fixed timestep (using nanoseconds for precision)
    private var lastFrameTimeNs = 0L
    private var accumulatorNs = 0L
    private var isRunning = false
    
    // Game data
    private var gameSpeed = 1f
    private var baseSpeed = 1f
    private var score = 0
    private var gameTime = 0L // Milliseconds
    private var startTime = 0L
    private var pausedTime = 0L
    private var pauseStartTime = 0L
    private var position = 1 // Player position (1 = first)
    private var renderCallback: ((DrawScope) -> Unit)? = null
    
    init {
        // Initialize game entities
        initializeGame()
        _engineState.value = EngineState.LOADING
    }
    
    /**
     * Initializes game entities (opponents, track, player).
     */
    private fun initializeGame() {
        // Initialize opponents
        opponents.clear()
        repeat(3) { i ->
            opponents.add(OpponentCar(lane = i, yOffset = -200f * (i + 1)))
        }
        
        // Reset game data
        gameSpeed = 1f
        baseSpeed = 1f
        score = 0
        gameTime = 0L
        startTime = 0L
        pausedTime = 0L
        position = 1
        isSteeringLeft = false
        isSteeringRight = false
        isAccelerating = false
        tiltSteeringValue = 0f
        playerCar.reset()
    }
    
    /**
     * Starts the game engine and begins the game loop.
     * Transitions from LOADING or GAME_OVER to RUNNING state.
     */
    fun start() {
        if (_engineState.value == EngineState.RUNNING) return
        
        initializeGame()
        _engineState.value = EngineState.RUNNING
        isRunning = true
        lastFrameTimeNs = System.nanoTime()
        accumulatorNs = 0L
        startTime = System.currentTimeMillis()
        pausedTime = 0L
        
        // Start the game loop using Choreographer for frame synchronization
        choreographer = Choreographer.getInstance()
        startGameLoop()
    }
    
    /**
     * Stops the game engine and cleans up resources.
     * Transitions to LOADING state and stops all game loops.
     */
    fun stop() {
        isRunning = false
        stopGameLoop()
        _engineState.value = EngineState.LOADING
    }
    
    /**
     * Pauses the game engine.
     * Transitions from RUNNING to PAUSED state.
     * Game state is preserved and can be resumed.
     */
    fun pause() {
        if (_engineState.value != EngineState.RUNNING) return
        
        isRunning = false
        stopGameLoop()
        pauseStartTime = System.currentTimeMillis()
        _engineState.value = EngineState.PAUSED
    }
    
    /**
     * Resumes the game engine from paused state.
     * Transitions from PAUSED to RUNNING state.
     * Game continues from where it was paused.
     */
    fun resume() {
        if (_engineState.value != EngineState.PAUSED) return
        
        // Add paused time to total paused time
        pausedTime += System.currentTimeMillis() - pauseStartTime
        pauseStartTime = 0L
        
        _engineState.value = EngineState.RUNNING
        isRunning = true
        lastFrameTimeNs = System.nanoTime()
        accumulatorNs = 0L
        startGameLoop()
    }
    
    /**
     * Starts the game loop using Choreographer for frame-based updates.
     * Choreographer ensures updates are synchronized with the display refresh rate.
     * Uses fixed timestep for consistent game logic regardless of frame rate variations.
     * 
     * HOW IT WORKS:
     * 1. Choreographer calls our callback on each VSYNC (typically 60Hz)
     * 2. We calculate delta time between frames
     * 3. We accumulate delta time in an accumulator
     * 4. We execute fixed timestep updates (60 per second) based on accumulated time
     * 5. This ensures game logic runs at consistent intervals even if rendering FPS varies
     */
    private fun startGameLoop() {
        stopGameLoop() // Ensure no duplicate loops
        
        choreographer = Choreographer.getInstance()
        frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
            if (!isRunning) return@FrameCallback
            
            // Calculate delta time in nanoseconds since last frame
            // frameTimeNanos is the timestamp when this frame should be displayed
            val deltaTimeNs = if (lastFrameTimeNs == 0L) {
                // First frame: use a reasonable initial delta (one frame)
                lastFrameTimeNs = frameTimeNanos
                targetFrameTimeNs // Use target frame time for first frame
            } else {
                val delta = frameTimeNanos - lastFrameTimeNs
                lastFrameTimeNs = frameTimeNanos
                // Cap delta time to prevent large jumps (e.g., when app resumes after pause)
                // Cap at ~100ms (100,000,000 nanoseconds) to prevent game logic explosion
                delta.coerceAtMost(100_000_000L)
            }
            
            // Fixed timestep accumulation (in nanoseconds)
            accumulatorNs += deltaTimeNs
            
            // Execute fixed timestep updates
            // This ensures game logic runs at consistent intervals (60 times per second)
            // regardless of actual rendering frame rate
            // If we're running slow, we may execute multiple updates per frame
            // If we're running fast, we may skip updates occasionally
            while (accumulatorNs >= targetFrameTimeNs) {
                updateGame(fixedTimeStep.toFloat())
                accumulatorNs -= targetFrameTimeNs
            }
            
            // Schedule next frame callback
            // Choreographer will call this on the next VSYNC signal (~16.67ms later at 60Hz)
            choreographer?.postFrameCallback(frameCallback!!)
        }
        
        // Initialize timing (will be set on first callback)
        lastFrameTimeNs = 0L
        accumulatorNs = 0L
        
        // Start the frame callback loop
        // First callback will fire on the next VSYNC
        choreographer?.postFrameCallback(frameCallback!!)
    }
    
    /**
     * Stops the game loop and cleans up Choreographer callbacks.
     */
    private fun stopGameLoop() {
        frameCallback?.let { callback ->
            choreographer?.removeFrameCallback(callback)
        }
        frameCallback = null
        gameLoopJob?.cancel()
        gameLoopJob = null
    }
    
    /**
     * Fixed timestep game update.
     * This method is called at a consistent rate (60 times per second) regardless of rendering FPS.
     * 
     * UPDATE ORDER:
     * 1. Track scrolling (background movement)
     * 2. Player car movement (input processing happens asynchronously)
     * 3. Opponent cars movement (AI updates)
     * 4. Collision detection
     * 5. Score and difficulty updates
     * 6. Game state updates
     * 
     * @param deltaTime Fixed timestep in seconds (typically 1/60 = 0.0167s)
     */
    private fun updateGame(deltaTime: Float) {
        if (_engineState.value != EngineState.RUNNING) return
        
        // 1. PROCESS ACCELERATION - Adjust game speed based on accelerator input
        if (isAccelerating) {
            baseSpeed = (baseSpeed + 0.02f).coerceAtMost(3.0f) // Max speed multiplier
        } else {
            baseSpeed = (baseSpeed - 0.01f).coerceAtLeast(1.0f) // Min speed
        }
        gameSpeed = baseSpeed + (score / 1000f) * 0.1f // Speed increases with score
        
        // 2. UPDATE TRACK - Scroll the road background
        track.update(gameSpeed)
        
        // 3. UPDATE PLAYER - Process player car movement and lane changes
        playerCar.update()
        
        // 4. UPDATE OPPONENTS - Move AI-controlled cars down the track
        opponents.forEach { opponent ->
            opponent.update(gameSpeed)
            
            // Respawn opponent if off screen (recycle for performance)
            if (opponent.getY() > 2000f) {
                opponent.reset()
            }
        }
        
        // 5. COLLISION DETECTION - Check for collisions between player and opponents
        if (collisionDetector.checkCollision(playerCar, opponents)) {
            // Collision detected - transition to game over state
            _engineState.value = EngineState.GAME_OVER
            saveManager.saveHighScore(score)
            isRunning = false
            stopGameLoop()
        }
        
        // 6. UPDATE SCORE - Increase score over time based on speed
        score += (gameSpeed * 0.1f).toInt()
        
        // 7. UPDATE GAME TIME - Track elapsed game time (excluding paused time)
        if (startTime > 0) {
            gameTime = System.currentTimeMillis() - startTime - pausedTime
        }
        
        // 8. CALCULATE POSITION - Determine player position relative to opponents
        // For now, assume player is always in first position (can be enhanced later)
        position = 1
        
        // 9. UPDATE GAME STATE - Broadcast current game state to UI
        _gameState.value = _gameState.value.copy(
            score = score,
            speed = gameSpeed,
            gameTime = gameTime,
            position = position,
            isGameOver = _engineState.value == EngineState.GAME_OVER
        )
    }
    
    /**
     * Renders the current game state to the provided DrawScope.
     * This method is called by the UI whenever the canvas needs to be redrawn.
     * 
     * RENDER ORDER:
     * 1. Track/Background (road, lanes, markings)
     * 2. Opponent cars (drawn first so player car appears on top)
     * 3. Player car (drawn last for proper layering)
     * 
     * Note: Rendering is decoupled from updates. Multiple renders can occur
     * between updates, but the game state remains consistent due to fixed timestep.
     * 
     * @param drawScope The Compose DrawScope for drawing operations
     */
    fun render(drawScope: DrawScope) {
        // Only render if game is in a renderable state
        if (_engineState.value == EngineState.LOADING) {
            // Could show loading screen here
            return
        }
        
        // 1. RENDER TRACK - Draw road, lane markings, and background
        track.render(drawScope)
        
        // 2. RENDER OPPONENTS - Draw all opponent cars
        opponents.forEach { it.render(drawScope) }
        
        // 3. RENDER PLAYER - Draw player car (on top layer)
        playerCar.render(drawScope)
        
        // Store render callback for potential future use
        renderCallback = { scope -> render(scope) }
    }
    
    /**
     * Handles player input for lane changes and acceleration.
     * Input is processed immediately when received, affecting the next update cycle.
     * 
     * @param input The input action (LEFT, RIGHT, ACCELERATE, or release actions)
     */
    fun handleInput(input: Input) {
        // Only process input when game is running
        if (_engineState.value != EngineState.RUNNING) return
        
        when (input) {
            Input.LEFT -> {
                isSteeringLeft = true
                isSteeringRight = false
                playerCar.moveLeft()
            }
            Input.RIGHT -> {
                isSteeringRight = true
                isSteeringLeft = false
                playerCar.moveRight()
            }
            Input.RELEASE_LEFT -> {
                isSteeringLeft = false
            }
            Input.RELEASE_RIGHT -> {
                isSteeringRight = false
            }
            Input.ACCELERATE -> {
                isAccelerating = true
            }
            Input.RELEASE_ACCELERATE -> {
                isAccelerating = false
            }
        }
    }
    
    /**
     * Sets tilt steering value from accelerometer.
     * @param tiltValue Tilt value from -1.0 (left) to 1.0 (right)
     */
    fun setTiltSteering(tiltValue: Float) {
        tiltSteeringValue = tiltValue.coerceIn(-1f, 1f)
        
        // Apply tilt steering continuously
        // Map tilt value to lane position: -1.0 = lane 0, 0.0 = lane 1, 1.0 = lane 2
        if (kotlin.math.abs(tiltValue) > 0.1f) {
            val targetLane = ((tiltValue + 1f) * 1f).toInt().coerceIn(0, 2)
            val currentLane = playerCar.getLane()
            
            // Only change lane if tilt is significant
            if (targetLane != currentLane) {
                if (tiltValue < -0.3f && currentLane > 0) {
                    playerCar.moveLeft()
                } else if (tiltValue > 0.3f && currentLane < 2) {
                    playerCar.moveRight()
                }
            }
        }
    }
    
    /**
     * Resets the game to initial state.
     * Can be called from GAME_OVER state to restart the game.
     */
    fun reset() {
        stop()
        initializeGame()
        _gameState.value = GameStateData()
        _engineState.value = EngineState.LOADING
    }
    
    /**
     * Cleans up resources when the engine is no longer needed.
     * Should be called when the activity/fragment is destroyed.
     */
    fun dispose() {
        stop()
        gameScope.cancel()
        renderCallback = null
    }
}

/**
 * Game state data class containing current game information.
 */
data class GameStateData(
    val score: Int = 0,
    val speed: Float = 1f,
    val gameTime: Long = 0L, // Milliseconds
    val position: Int = 1,
    val isGameOver: Boolean = false
)

