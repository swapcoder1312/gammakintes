package com.miniracer.game

/**
 * CollisionDetector - Handles collision detection between game entities.
 * Uses lane-based collision detection with Y position overlap checking.
 */
class CollisionDetector {
    
    /**
     * Checks if the player car collides with any opponent car.
     * Uses lane matching and Y position overlap for efficient detection.
     * @param playerCar The player's car
     * @param opponents List of opponent cars
     * @return true if collision detected, false otherwise
     */
    fun checkCollision(playerCar: PlayerCar, opponents: List<OpponentCar>): Boolean {
        val playerLane = playerCar.getLane()
        val playerY = playerCar.getY()
        val carHeight = 120f
        
        for (opponent in opponents) {
            // Check if in same lane
            if (opponent.getLane() == playerLane) {
                val opponentY = opponent.getY()
                // Check Y overlap (cars are 120f tall, player is at bottom ~1500f)
                val playerTop = playerY - carHeight
                val playerBottom = playerY
                val opponentTop = opponentY - carHeight
                val opponentBottom = opponentY
                
                // Check if Y ranges overlap
                if (playerTop < opponentBottom && playerBottom > opponentTop) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Axis-Aligned Bounding Box collision detection.
     * Checks if two rectangular bounds overlap.
     */
    fun checkCollision(bounds1: CarBounds, bounds2: CarBounds): Boolean {
        return bounds1.x < bounds2.x + bounds2.width &&
               bounds1.x + bounds1.width > bounds2.x &&
               bounds1.y < bounds2.y + bounds2.height &&
               bounds1.y + bounds1.height > bounds2.y
    }
}

