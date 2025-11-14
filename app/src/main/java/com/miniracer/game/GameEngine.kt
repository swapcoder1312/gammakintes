package com.miniracer.game

import android.content.Context
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GameEngine - Core game loop manager that coordinates all game entities.
 * Handles game state, updates, rendering, and input processing.
 */
class GameEngine(private val context: Context? = null) {
    private val playerCar = PlayerCar()
    private val track = Track()
    private val opponents = mutableListOf<OpponentCar>()
    private val collisionDetector = CollisionDetector()
    private val saveManager = SaveManager(context)
    
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    private var gameSpeed = 1f
    private var score = 0
    
    init {
        // Initialize opponents
        repeat(3) { i ->
            opponents.add(OpponentCar(lane = i, yOffset = -200f * (i + 1)))
        }
    }
    
    enum class Input {
        LEFT, RIGHT
    }
    
    fun handleInput(input: Input) {
        when (input) {
            Input.LEFT -> playerCar.moveLeft()
            Input.RIGHT -> playerCar.moveRight()
        }
    }
    
    fun update() {
        if (_gameState.value.isGameOver) return
        
        // Update track
        track.update(gameSpeed)
        
        // Update player
        playerCar.update()
        
        // Update opponents
        opponents.forEach { opponent ->
            opponent.update(gameSpeed)
            
            // Respawn opponent if off screen
            if (opponent.getY() > 2000f) {
                opponent.reset()
            }
        }
        
        // Check collisions
        if (collisionDetector.checkCollision(playerCar, opponents)) {
            _gameState.value = _gameState.value.copy(isGameOver = true)
            saveManager.saveHighScore(score)
        }
        
        // Update score
        score += (gameSpeed * 0.1f).toInt()
        gameSpeed += 0.001f // Gradually increase difficulty
        
        _gameState.value = _gameState.value.copy(
            score = score,
            speed = gameSpeed
        )
    }
    
    fun render(drawScope: DrawScope) {
        track.render(drawScope)
        playerCar.render(drawScope)
        opponents.forEach { it.render(drawScope) }
    }
    
    fun reset() {
        playerCar.reset()
        opponents.forEach { it.reset() }
        gameSpeed = 1f
        score = 0
        _gameState.value = GameState()
    }
}

data class GameState(
    val score: Int = 0,
    val speed: Float = 1f,
    val isGameOver: Boolean = false
)

