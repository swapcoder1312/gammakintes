# MiniRacer Project Structure

```
MiniRacer/
├── app/
│   ├── build.gradle.kts          # App-level Gradle configuration with dependencies
│   ├── proguard-rules.pro         # ProGuard rules for code obfuscation
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml    # Android app manifest
│           ├── java/com/miniracer/
│           │   ├── MainActivity.kt    # Single activity entry point
│           │   ├── game/
│           │   │   ├── GameEngine.kt      # Core game loop and coordination
│           │   │   ├── PlayerCar.kt       # Player car entity
│           │   │   ├── OpponentCar.kt     # AI opponent car entity
│           │   │   ├── Track.kt           # Road/track rendering
│           │   │   ├── SpriteLoader.kt    # Asset loading utility
│           │   │   ├── CollisionDetector.kt  # Collision detection system
│           │   │   └── SaveManager.kt       # Persistent data management
│           │   └── ui/
│           │       ├── GameScreen.kt       # Compose UI with game canvas
│           │       └── theme/
│           │           └── Theme.kt         # Material theme configuration
│           └── res/
│               ├── values/
│               │   ├── strings.xml         # String resources
│               │   └── themes.xml          # App theme
│               └── mipmap/                 # App icons (not included)
├── build.gradle.kts              # Project-level Gradle configuration
├── settings.gradle.kts           # Gradle settings and project structure
├── gradle.properties             # Gradle properties
└── PROJECT_STRUCTURE.md          # This file
```

## Key Files Summary

### MainActivity.kt
Single activity entry point that sets up Jetpack Compose UI and hosts the game screen.

### GameEngine.kt
Core game loop manager that coordinates all game entities, handles game state, updates, rendering, and input processing.

### PlayerCar.kt
Represents the player's controllable car entity. Handles movement, position, and rendering of the player vehicle.

### OpponentCar.kt
Represents an AI-controlled opponent car. Handles autonomous movement, lane positioning, and rendering.

### Track.kt
Manages the road/track rendering with lane markings. Creates the illusion of movement through scrolling road elements.

### SpriteLoader.kt
Utility class for loading and caching game sprites/assets. Handles bitmap loading from resources and conversion to Compose ImageBitmap.

### CollisionDetector.kt
Handles collision detection between game entities. Uses lane-based collision detection with Y position overlap checking for efficient car collision detection.

### SaveManager.kt
Manages persistent game data using SharedPreferences. Handles saving/loading high scores and game settings.

