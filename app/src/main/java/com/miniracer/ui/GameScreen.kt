package com.miniracer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.miniracer.game.GameEngine
import com.miniracer.game.GameStateData

/**
 * GameScreen - Composable UI that hosts the game canvas and handles touch input.
 * Integrates GameEngine with Jetpack Compose Canvas for rendering.
 * Manages game lifecycle (start/pause/resume/stop) based on screen visibility.
 */
@Composable
fun GameScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Create and remember game engine instance
    val gameEngine = remember { GameEngine(context) }
    
    // Collect game state and engine state
    val gameState by gameEngine.gameState.collectAsState()
    val engineState by gameEngine.engineState.collectAsState()
    
    // Handle lifecycle events to start/pause/resume/stop the game
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Start the game when screen becomes visible
                    val currentState = gameEngine.engineState.value
                    if (currentState == GameEngine.EngineState.LOADING ||
                        currentState == GameEngine.EngineState.GAME_OVER) {
                        gameEngine.start()
                    } else if (currentState == GameEngine.EngineState.PAUSED) {
                        gameEngine.resume()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // Pause the game when screen is paused
                    if (gameEngine.engineState.value == GameEngine.EngineState.RUNNING) {
                        gameEngine.pause()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    // Stop the game when screen is stopped
                    gameEngine.stop()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Clean up game engine when composable is disposed
            gameEngine.dispose()
        }
    }
    
    // Start game automatically on first composition
    LaunchedEffect(Unit) {
        if (gameEngine.engineState.value == GameEngine.EngineState.LOADING) {
            gameEngine.start()
        }
    }

    // Game canvas with touch input handling
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val currentState = gameEngine.engineState.value
                        // Only handle input when game is running
                        if (currentState == GameEngine.EngineState.RUNNING) {
                            val centerX = size.width / 2
                            if (offset.x < centerX) {
                                gameEngine.handleInput(GameEngine.Input.LEFT)
                            } else {
                                gameEngine.handleInput(GameEngine.Input.RIGHT)
                            }
                        } else if (currentState == GameEngine.EngineState.GAME_OVER) {
                            // Restart game on tap when game is over
                            gameEngine.start()
                        } else if (currentState == GameEngine.EngineState.PAUSED) {
                            // Resume game on tap when paused
                            gameEngine.resume()
                        }
                    }
                )
            }
    ) {
        // Render the game
        // Rendering is called by Compose whenever the canvas needs to be redrawn
        // This is decoupled from the game loop updates, which run at fixed intervals
        gameEngine.render(this)
        
        // Optionally render UI overlays (score, game over, etc.)
        // This could be expanded to show score, pause button, etc.
    }
}

