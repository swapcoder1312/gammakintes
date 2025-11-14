package com.miniracer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.miniracer.game.*

/**
 * GameScreen - Composable UI that hosts the game canvas and handles touch input.
 * Integrates GameEngine with Jetpack Compose Canvas for rendering.
 * Manages game lifecycle (start/pause/resume/stop) based on screen visibility.
 * Includes HUD overlay, touch controls, and tilt steering support.
 */
@Composable
fun GameScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Create and remember managers
    val gameEngine = remember { GameEngine(context) }
    val settingsManager = remember { SettingsManager(context) }
    val sensorManager = remember { SensorManager(context) }
    val soundManager = remember { SoundManager(context) }
    
    // Settings state
    var isTiltEnabled by remember { mutableStateOf(settingsManager.isTiltEnabled()) }
    var isSoundEnabled by remember { mutableStateOf(settingsManager.isSoundEnabled()) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    
    // Collect game state and engine state
    val gameState by gameEngine.gameState.collectAsState()
    val engineState by gameEngine.engineState.collectAsState()
    val tiltValue by sensorManager.tiltValue.collectAsState()
    
    // Update tilt sensitivity and dead zone from settings
    LaunchedEffect(Unit) {
        sensorManager.setSensitivity(settingsManager.getTiltSensitivity())
        sensorManager.setDeadZone(settingsManager.getTiltDeadZone())
    }
    
    // Handle tilt steering
    LaunchedEffect(tiltValue, isTiltEnabled) {
        if (isTiltEnabled && engineState == GameEngine.EngineState.RUNNING) {
            gameEngine.setTiltSteering(tiltValue)
        }
    }
    
    // Handle sound settings
    LaunchedEffect(isSoundEnabled) {
        if (isSoundEnabled) {
            soundManager.enable()
        } else {
            soundManager.disable()
        }
    }
    
    // Handle lifecycle events to start/pause/resume/stop the game
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Enable tilt if enabled in settings
                    if (isTiltEnabled) {
                        sensorManager.enable()
                    }
                    
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
                    sensorManager.disable()
                }
                Lifecycle.Event.ON_STOP -> {
                    // Stop the game when screen is stopped
                    gameEngine.stop()
                    sensorManager.disable()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Clean up managers when composable is disposed
            gameEngine.dispose()
            sensorManager.dispose()
            soundManager.dispose()
        }
    }
    
    // Start game automatically on first composition
    LaunchedEffect(Unit) {
        if (gameEngine.engineState.value == GameEngine.EngineState.LOADING) {
            gameEngine.start()
        }
    }
    
    // Enable/disable tilt based on settings
    LaunchedEffect(isTiltEnabled) {
        if (isTiltEnabled && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            sensorManager.enable()
        } else {
            sensorManager.disable()
        }
    }

    // Main game content with overlay
    Box(modifier = Modifier.fillMaxSize()) {
        // Game canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Render the game
            gameEngine.render(this)
        }
        
        // HUD Overlay (only show when game is running or paused)
        if (engineState == GameEngine.EngineState.RUNNING ||
            engineState == GameEngine.EngineState.PAUSED ||
            engineState == GameEngine.EngineState.GAME_OVER) {
            HUD(
                gameState = gameState,
                engineState = engineState,
                onPauseClick = {
                    if (engineState == GameEngine.EngineState.RUNNING) {
                        gameEngine.pause()
                    } else if (engineState == GameEngine.EngineState.PAUSED) {
                        gameEngine.resume()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Touch Controls (only show when game is running and tilt is disabled)
        if (engineState == GameEngine.EngineState.RUNNING && !isTiltEnabled) {
            TouchControls(
                gameEngine = gameEngine,
                engineState = engineState,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Settings Screen (if shown)
        if (showSettingsScreen) {
            SettingsScreen(
                settingsManager = settingsManager,
                onBackClick = {
                    showSettingsScreen = false
                    // Refresh settings state
                    isTiltEnabled = settingsManager.isTiltEnabled()
                    isSoundEnabled = settingsManager.isSoundEnabled()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Pause Menu (if paused)
        if (engineState == GameEngine.EngineState.PAUSED && !showSettingsScreen) {
            PauseMenu(
                onResume = { gameEngine.resume() },
                onSettings = { showSettingsScreen = true },
                onQuit = { gameEngine.stop() },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Game Over Screen (if game over)
        if (engineState == GameEngine.EngineState.GAME_OVER) {
            GameOverScreen(
                score = gameState.score,
                onRestart = { gameEngine.start() },
                onQuit = { gameEngine.stop() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * PauseMenu - Shows pause menu overlay.
 */
@Composable
fun PauseMenu(
    onResume: () -> Unit,
    onSettings: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "PAUSED",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resume")
                }
                
                Button(
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Settings")
                }
                
                Button(
                    onClick = onQuit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Quit")
                }
            }
        }
    }
}

/**
 * GameOverScreen - Shows game over screen.
 */
@Composable
fun GameOverScreen(
    score: Int,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "GAME OVER",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restart")
                }
                
                Button(
                    onClick = onQuit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Quit")
                }
            }
        }
    }
}
