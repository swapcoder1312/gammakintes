package com.miniracer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miniracer.game.SettingsManager

/**
 * SettingsScreen - Composable screen for game settings.
 * Allows toggling tilt control, sound, and adjusting tilt sensitivity.
 */
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tiltEnabled by remember { mutableStateOf(settingsManager.isTiltEnabled()) }
    var soundEnabled by remember { mutableStateOf(settingsManager.isSoundEnabled()) }
    var tiltSensitivity by remember { mutableStateOf(settingsManager.getTiltSensitivity()) }
    var tiltDeadZone by remember { mutableStateOf(settingsManager.getTiltDeadZone()) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        // Settings Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tilt Control Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Tilt Control",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    // Tilt Enable/Disable
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Tilt Steering",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Use device tilt to steer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = tiltEnabled,
                            onCheckedChange = { enabled ->
                                tiltEnabled = enabled
                                settingsManager.setTiltEnabled(enabled)
                            }
                        )
                    }
                    
                    // Tilt Sensitivity (only shown when tilt is enabled)
                    if (tiltEnabled) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tilt Sensitivity",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = String.format("%.1f", tiltSensitivity),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = tiltSensitivity,
                                onValueChange = { value ->
                                    tiltSensitivity = value
                                    settingsManager.setTiltSensitivity(value)
                                },
                                valueRange = 0.5f..5.0f,
                                steps = 8
                            )
                        }
                        
                        // Tilt Dead Zone
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tilt Dead Zone",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = String.format("%.2f", tiltDeadZone),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "Prevents unwanted steering from small movements",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Slider(
                                value = tiltDeadZone,
                                onValueChange = { value ->
                                    tiltDeadZone = value
                                    settingsManager.setTiltDeadZone(value)
                                },
                                valueRange = 0f..0.5f,
                                steps = 9
                            )
                        }
                    }
                }
            }
            
            // Audio Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Audio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    // Sound Enable/Disable
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Sound",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Play game sound effects",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { enabled ->
                                soundEnabled = enabled
                                settingsManager.setSoundEnabled(enabled)
                            }
                        )
                    }
                }
            }
            
            // Reset Button
            Button(
                onClick = {
                    settingsManager.resetToDefaults()
                    tiltEnabled = settingsManager.isTiltEnabled()
                    soundEnabled = settingsManager.isSoundEnabled()
                    tiltSensitivity = settingsManager.getTiltSensitivity()
                    tiltDeadZone = settingsManager.getTiltDeadZone()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reset to Defaults")
            }
        }
    }
}

