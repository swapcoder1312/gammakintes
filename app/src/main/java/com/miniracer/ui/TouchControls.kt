package com.miniracer.ui

import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniracer.game.GameEngine

/**
 * TouchControls - On-screen touch controls for steering and acceleration.
 * Provides left/right steering buttons and an accelerator button.
 */
@Composable
fun TouchControls(
    gameEngine: GameEngine,
    engineState: GameEngine.EngineState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Bottom Left - Steering Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Button
            SteeringButton(
                text = "◀",
                onClick = {
                    if (engineState == GameEngine.EngineState.RUNNING) {
                        gameEngine.handleInput(GameEngine.Input.LEFT)
                    }
                },
                onPress = {
                    if (engineState == GameEngine.EngineState.RUNNING) {
                        gameEngine.handleInput(GameEngine.Input.LEFT)
                    }
                },
                onRelease = {
                    gameEngine.handleInput(GameEngine.Input.RELEASE_LEFT)
                },
                enabled = engineState == GameEngine.EngineState.RUNNING,
                modifier = Modifier.size(64.dp)
            )
            
            // Right Button
            SteeringButton(
                text = "▶",
                onClick = {
                    if (engineState == GameEngine.EngineState.RUNNING) {
                        gameEngine.handleInput(GameEngine.Input.RIGHT)
                    }
                },
                onPress = {
                    if (engineState == GameEngine.EngineState.RUNNING) {
                        gameEngine.handleInput(GameEngine.Input.RIGHT)
                    }
                },
                onRelease = {
                    gameEngine.handleInput(GameEngine.Input.RELEASE_RIGHT)
                },
                enabled = engineState == GameEngine.EngineState.RUNNING,
                modifier = Modifier.size(64.dp)
            )
        }
        
        // Bottom Right - Accelerator Button
        AcceleratorButton(
            onClick = {
                if (engineState == GameEngine.EngineState.RUNNING) {
                    gameEngine.handleInput(GameEngine.Input.ACCELERATE)
                }
            },
            onPress = {
                if (engineState == GameEngine.EngineState.RUNNING) {
                    gameEngine.handleInput(GameEngine.Input.ACCELERATE)
                }
            },
            onRelease = {
                gameEngine.handleInput(GameEngine.Input.RELEASE_ACCELERATE)
            },
            enabled = engineState == GameEngine.EngineState.RUNNING,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(80.dp, 80.dp)
        )
    }
}

/**
 * Steering button for left/right controls.
 */
@Composable
fun SteeringButton(
    text: String,
    onClick: () -> Unit,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = if (isPressed) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.8f else 0.4f)
        },
        contentColor = if (isPressed) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        enabled = enabled
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    
                    detectDragGestures(
                        onDragStart = {
                            isPressed = true
                            onPress()
                        },
                        onDragEnd = {
                            isPressed = false
                            onRelease()
                        },
                        onDrag = { _, _ -> }
                    )
                }
        ) {
            Text(
                text = text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * Accelerator button for speed control.
 */
@Composable
fun AcceleratorButton(
    onClick: () -> Unit,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    FloatingActionButton(
        onClick = {
            if (!isPressed) {
                isPressed = true
                onPress()
            } else {
                isPressed = false
                onRelease()
            }
        },
        modifier = modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            forEachGesture {
                awaitPointerEventScope {
                    val down = awaitFirstDown()
                    isPressed = true
                    onPress()
                    waitForUpOrCancellation()
                    isPressed = false
                    onRelease()
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isPressed) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.error.copy(alpha = if (enabled) 0.8f else 0.4f)
        },
        contentColor = if (isPressed) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onError
        },
        enabled = enabled
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GO",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "▼",
                fontSize = 12.sp
            )
        }
    }
}

