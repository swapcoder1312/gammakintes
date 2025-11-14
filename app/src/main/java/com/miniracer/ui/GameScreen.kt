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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.miniracer.game.GameEngine
import com.miniracer.game.GameState
import kotlinx.coroutines.delay

/**
 * GameScreen - Composable UI that hosts the game canvas and handles touch input.
 * Integrates GameEngine with Jetpack Compose Canvas for rendering.
 */
@Composable
fun GameScreen() {
    val context = LocalContext.current
    val gameEngine = remember { GameEngine(context) }
    val gameState by gameEngine.gameState.collectAsState()
    val density = LocalDensity.current

    // Game loop
    LaunchedEffect(Unit) {
        while (true) {
            gameEngine.update()
            delay(16) // ~60 FPS
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val centerX = size.width / 2
                        if (offset.x < centerX) {
                            gameEngine.handleInput(GameEngine.Input.LEFT)
                        } else {
                            gameEngine.handleInput(GameEngine.Input.RIGHT)
                        }
                    }
                )
            }
    ) {
        gameEngine.render(this)
    }
}

