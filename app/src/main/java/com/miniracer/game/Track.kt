package com.miniracer.game

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * Track - Manages the road/track rendering with lane markings.
 * Supports loading track geometry from JSON assets and procedural generation.
 * Includes collision polygons for off-road detection.
 */
class Track(private val context: Context? = null) {
    private var roadOffset: Float = 0f
    private val laneMarkingHeight = 50f
    private val laneMarkingGap = 30f
    
    // Track geometry data
    private var trackData: TrackData? = null
    
    /**
     * Data class representing track geometry loaded from JSON or generated procedurally.
     */
    data class TrackData(
        val length: Float, // Total track length in pixels
        val centerLine: List<Offset>, // Center line points of the track
        val curves: List<CurveSegment>, // Curved segments
        val elevationMarkers: List<ElevationMarker>, // Visual elevation markers
        val spawnPoints: List<SpawnPoint>, // Car spawn positions
        val collisionPolygons: List<CollisionPolygon> // Off-road collision boundaries
    )
    
    /**
     * Represents a curved segment of the track.
     */
    data class CurveSegment(
        val startY: Float,
        val endY: Float,
        val curvature: Float, // Positive = right curve, negative = left curve
        val radius: Float // Curve radius in pixels
    )
    
    /**
     * Visual elevation marker (for display only, doesn't affect physics).
     */
    data class ElevationMarker(
        val y: Float,
        val elevation: Float, // Visual elevation value (0-100)
        val markerType: String // "hill", "valley", "flat"
    )
    
    /**
     * Spawn point for cars.
     */
    data class SpawnPoint(
        val y: Float,
        val x: Float, // X position on track
        val lane: Int // Lane index (0-2)
    )
    
    /**
     * Collision polygon for off-road detection.
     * Points define a closed polygon boundary.
     */
    data class CollisionPolygon(
        val points: List<Offset>, // Polygon vertices
        val isRoad: Boolean = true // true = road area, false = off-road
    )
    
    /**
     * Loads track geometry from a JSON file in assets.
     * @param name Asset filename (without .json extension)
     * @return true if loaded successfully, false otherwise
     */
    fun loadFromAssets(name: String): Boolean {
        if (context == null) return false
        
        return try {
            val inputStream = context.assets.open("$name.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            trackData = parseTrackJson(json)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Generates a procedural track with random curves, elevation markers, and spawn points.
     * @param seed Random seed for reproducible generation
     * @return true if generation succeeded
     */
    fun generate(seed: Long): Boolean {
        val random = Random(seed)
        
        // Generate track length between 1000-3000 pixels
        val length = 1000f + random.nextFloat() * 2000f
        
        // Generate center line points
        val centerLine = mutableListOf<Offset>()
        val screenWidth = 1080f // Default screen width
        val centerX = screenWidth / 2f
        var currentX = centerX
        var currentY = 0f
        val segmentLength = 50f // Points every 50 pixels
        
        centerLine.add(Offset(currentX, currentY))
        
        while (currentY < length) {
            // Add slight random variation to X position for curves
            val curveAmount = (random.nextFloat() - 0.5f) * 100f // Max 50px deviation
            currentX += curveAmount * 0.1f // Smooth curve
            currentX = currentX.coerceIn(screenWidth * 0.2f, screenWidth * 0.8f)
            currentY += segmentLength
            centerLine.add(Offset(currentX, currentY))
        }
        
        // Generate curve segments
        val curves = mutableListOf<CurveSegment>()
        var y = 0f
        while (y < length) {
            val segmentLength = 200f + random.nextFloat() * 300f
            val curvature = (random.nextFloat() - 0.5f) * 2f // -1 to 1
            val radius = 300f + random.nextFloat() * 500f
            
            if (y + segmentLength <= length) {
                curves.add(CurveSegment(y, y + segmentLength, curvature, radius))
            }
            y += segmentLength
        }
        
        // Generate elevation markers (visual only)
        val elevationMarkers = mutableListOf<ElevationMarker>()
        y = 0f
        while (y < length) {
            val markerY = y + random.nextFloat() * 200f
            if (markerY < length) {
                val elevation = random.nextFloat() * 100f
                val markerType = when {
                    elevation > 70f -> "hill"
                    elevation < 30f -> "valley"
                    else -> "flat"
                }
                elevationMarkers.add(ElevationMarker(markerY, elevation, markerType))
            }
            y += 300f
        }
        
        // Generate spawn points
        val spawnPoints = mutableListOf<SpawnPoint>()
        y = 0f
        while (y < length) {
            val spawnY = y + random.nextFloat() * 100f
            if (spawnY < length) {
                val lane = random.nextInt(0, 3)
                val laneX = when (lane) {
                    0 -> screenWidth * 0.2f
                    1 -> screenWidth * 0.5f
                    else -> screenWidth * 0.8f
                }
                spawnPoints.add(SpawnPoint(spawnY, laneX, lane))
            }
            y += 400f
        }
        
        // Generate collision polygons (road boundaries)
        val collisionPolygons = generateCollisionPolygons(centerLine, screenWidth)
        
        trackData = TrackData(
            length = length,
            centerLine = centerLine,
            curves = curves,
            elevationMarkers = elevationMarkers,
            spawnPoints = spawnPoints,
            collisionPolygons = collisionPolygons
        )
        
        return true
    }
    
    /**
     * Generates collision polygons based on center line and road width.
     */
    private fun generateCollisionPolygons(
        centerLine: List<Offset>,
        screenWidth: Float
    ): List<CollisionPolygon> {
        val polygons = mutableListOf<CollisionPolygon>()
        val roadWidth = screenWidth * 0.7f // Road width
        val halfWidth = roadWidth / 2f
        
        if (centerLine.isEmpty()) return polygons
        
        // Create road polygon (inside boundary)
        val leftEdgePoints = mutableListOf<Offset>()
        val rightEdgePoints = mutableListOf<Offset>()
        
        // Generate left and right edge points along the track
        for (i in centerLine.indices) {
            val center = centerLine[i]
            val angle = when {
                i < centerLine.size - 1 -> {
                    val next = centerLine[i + 1]
                    kotlin.math.atan2(next.y - center.y, next.x - center.x)
                }
                i > 0 -> {
                    val prev = centerLine[i - 1]
                    kotlin.math.atan2(center.y - prev.y, center.x - prev.x)
                }
                else -> 0f
            }
            
            // Perpendicular angle for road edges
            val perpAngle = angle + kotlin.math.PI.toFloat() / 2f
            
            val leftX = center.x + kotlin.math.cos(perpAngle) * halfWidth
            val leftY = center.y + kotlin.math.sin(perpAngle) * halfWidth
            val rightX = center.x - kotlin.math.cos(perpAngle) * halfWidth
            val rightY = center.y - kotlin.math.sin(perpAngle) * halfWidth
            
            leftEdgePoints.add(Offset(leftX, leftY))
            rightEdgePoints.add(Offset(rightX, rightY))
        }
        
        // Create road polygon (closed): left edge + reversed right edge
        val roadPolygon = leftEdgePoints + rightEdgePoints.reversed()
        polygons.add(CollisionPolygon(roadPolygon, isRoad = true))
        
        // Create off-road polygons (left and right sides of screen)
        val firstY = centerLine.first().y
        val lastY = centerLine.last().y
        
        // Left off-road polygon
        val leftOffRoad = listOf(
            Offset(0f, firstY),
            Offset(0f, lastY),
            Offset(leftEdgePoints.first().x, lastY),
            Offset(leftEdgePoints.first().x, firstY)
        )
        polygons.add(CollisionPolygon(leftOffRoad, isRoad = false))
        
        // Right off-road polygon
        val rightOffRoad = listOf(
            Offset(rightEdgePoints.first().x, firstY),
            Offset(rightEdgePoints.first().x, lastY),
            Offset(screenWidth, lastY),
            Offset(screenWidth, firstY)
        )
        polygons.add(CollisionPolygon(rightOffRoad, isRoad = false))
        
        return polygons
    }
    
    /**
     * Parses track data from JSON object.
     */
    private fun parseTrackJson(json: JSONObject): TrackData {
        val length = json.getDouble("length").toFloat()
        
        // Parse center line
        val centerLineArray = json.getJSONArray("centerLine")
        val centerLine = mutableListOf<Offset>()
        for (i in 0 until centerLineArray.length()) {
            val point = centerLineArray.getJSONObject(i)
            centerLine.add(Offset(
                point.getDouble("x").toFloat(),
                point.getDouble("y").toFloat()
            ))
        }
        
        // Parse curves
        val curvesArray = json.optJSONArray("curves") ?: JSONArray()
        val curves = mutableListOf<CurveSegment>()
        for (i in 0 until curvesArray.length()) {
            val curve = curvesArray.getJSONObject(i)
            curves.add(CurveSegment(
                curve.getDouble("startY").toFloat(),
                curve.getDouble("endY").toFloat(),
                curve.getDouble("curvature").toFloat(),
                curve.getDouble("radius").toFloat()
            ))
        }
        
        // Parse elevation markers
        val elevationArray = json.optJSONArray("elevationMarkers") ?: JSONArray()
        val elevationMarkers = mutableListOf<ElevationMarker>()
        for (i in 0 until elevationArray.length()) {
            val marker = elevationArray.getJSONObject(i)
            elevationMarkers.add(ElevationMarker(
                marker.getDouble("y").toFloat(),
                marker.getDouble("elevation").toFloat(),
                marker.getString("markerType")
            ))
        }
        
        // Parse spawn points
        val spawnArray = json.optJSONArray("spawnPoints") ?: JSONArray()
        val spawnPoints = mutableListOf<SpawnPoint>()
        for (i in 0 until spawnArray.length()) {
            val spawn = spawnArray.getJSONObject(i)
            spawnPoints.add(SpawnPoint(
                spawn.getDouble("y").toFloat(),
                spawn.getDouble("x").toFloat(),
                spawn.getInt("lane")
            ))
        }
        
        // Parse collision polygons
        val polygonsArray = json.optJSONArray("collisionPolygons") ?: JSONArray()
        val collisionPolygons = mutableListOf<CollisionPolygon>()
        for (i in 0 until polygonsArray.length()) {
            val polygon = polygonsArray.getJSONObject(i)
            val pointsArray = polygon.getJSONArray("points")
            val points = mutableListOf<Offset>()
            for (j in 0 until pointsArray.length()) {
                val point = pointsArray.getJSONObject(j)
                points.add(Offset(
                    point.getDouble("x").toFloat(),
                    point.getDouble("y").toFloat()
                ))
            }
            collisionPolygons.add(CollisionPolygon(
                points = points,
                isRoad = polygon.optBoolean("isRoad", true)
            ))
        }
        
        return TrackData(
            length = length,
            centerLine = centerLine,
            curves = curves,
            elevationMarkers = elevationMarkers,
            spawnPoints = spawnPoints,
            collisionPolygons = collisionPolygons
        )
    }
    
    /**
     * Checks if a point is off-road using collision polygons.
     * @param point The point to check
     * @return true if the point is off-road, false if on road
     */
    fun isOffRoad(point: Offset): Boolean {
        val data = trackData ?: return false
        
        for (polygon in data.collisionPolygons) {
            if (isPointInPolygon(point, polygon.points)) {
                return !polygon.isRoad
            }
        }
        
        // Default to off-road if not in any polygon
        return true
    }
    
    /**
     * Ray casting algorithm to check if a point is inside a polygon.
     */
    private fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
        if (polygon.size < 3) return false
        
        var inside = false
        var j = polygon.size - 1
        
        for (i in polygon.indices) {
            val xi = polygon[i].x
            val yi = polygon[i].y
            val xj = polygon[j].x
            val yj = polygon[j].y
            
            val intersect = ((yi > point.y) != (yj > point.y)) &&
                    (point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi)
            
            if (intersect) inside = !inside
            j = i
        }
        
        return inside
    }
    
    /**
     * Gets the track data.
     */
    fun getTrackData(): TrackData? = trackData
    
    /**
     * Gets spawn points for car placement.
     */
    fun getSpawnPoints(): List<SpawnPoint> = trackData?.spawnPoints ?: emptyList()
    
    fun update(speed: Float) {
        roadOffset += 5f * speed
        if (roadOffset > laneMarkingHeight + laneMarkingGap) {
            roadOffset = 0f
        }
    }
    
    fun render(drawScope: DrawScope) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        
        // If track data exists, render it
        val data = trackData
        if (data != null) {
            renderTrackGeometry(drawScope, data, width, height)
        } else {
            // Fallback to simple road rendering
            renderSimpleRoad(drawScope, width, height)
        }
    }
    
    /**
     * Renders track geometry with curves, elevation markers, etc.
     */
    private fun renderTrackGeometry(
        drawScope: DrawScope,
        data: TrackData,
        width: Float,
        height: Float
    ) {
        // Draw road background using center line
        if (data.centerLine.size >= 2) {
            val roadPath = Path()
            val roadWidth = width * 0.7f
            val halfWidth = roadWidth / 2f
            
            // Create road path from center line
            for (i in 0 until data.centerLine.size - 1) {
                val current = data.centerLine[i]
                val next = data.centerLine[i + 1]
                
                val angle = kotlin.math.atan2(next.y - current.y, next.x - current.x)
                val perpAngle = angle + kotlin.math.PI.toFloat() / 2f
                
                val leftX = current.x + kotlin.math.cos(perpAngle) * halfWidth
                val leftY = current.y + kotlin.math.sin(perpAngle) * halfWidth
                
                if (i == 0) {
                    roadPath.moveTo(leftX, leftY)
                } else {
                    roadPath.lineTo(leftX, leftY)
                }
            }
            
            // Close the road path
            for (i in data.centerLine.size - 1 downTo 0) {
                val current = data.centerLine[i]
                val angle = if (i > 0) {
                    val prev = data.centerLine[i - 1]
                    kotlin.math.atan2(current.y - prev.y, current.x - prev.x)
                } else {
                    kotlin.math.atan2(
                        data.centerLine[1].y - current.y,
                        data.centerLine[1].x - current.x
                    )
                }
                val perpAngle = angle - kotlin.math.PI.toFloat() / 2f
                
                val rightX = current.x - kotlin.math.cos(perpAngle) * halfWidth
                val rightY = current.y - kotlin.math.sin(perpAngle) * halfWidth
                
                roadPath.lineTo(rightX, rightY)
            }
            
            roadPath.close()
            
            // Draw road
            drawScope.drawPath(roadPath, Color(0xFF333333))
            
            // Draw lane dividers
            val lane1X = width * 0.4f
            val lane2X = width * 0.6f
            
            var y = roadOffset
            while (y < height) {
                drawScope.drawRect(
                    color = Color.White,
                    topLeft = Offset(lane1X - 2f, y),
                    size = Size(4f, laneMarkingHeight)
                )
                drawScope.drawRect(
                    color = Color.White,
                    topLeft = Offset(lane2X - 2f, y),
                    size = Size(4f, laneMarkingHeight)
                )
                y += laneMarkingHeight + laneMarkingGap
            }
            
            // Draw elevation markers (visual only)
            for (marker in data.elevationMarkers) {
                if (marker.y >= 0 && marker.y < height) {
                    val color = when (marker.markerType) {
                        "hill" -> Color(0xFFFFD700) // Gold
                        "valley" -> Color(0xFF00CED1) // Dark turquoise
                        else -> Color(0xFF90EE90) // Light green
                    }
                    drawScope.drawRect(
                        color = color.copy(alpha = 0.3f),
                        topLeft = Offset(0f, marker.y),
                        size = Size(width, 10f)
                    )
                }
            }
        }
        
        // Draw road edges
        drawScope.drawRect(
            color = Color.Yellow,
            topLeft = Offset(width * 0.15f, 0f),
            size = Size(4f, height)
        )
        drawScope.drawRect(
            color = Color.Yellow,
            topLeft = Offset(width * 0.85f - 4f, 0f),
            size = Size(4f, height)
        )
    }
    
    /**
     * Simple road rendering (fallback when no track data).
     */
    private fun renderSimpleRoad(drawScope: DrawScope, width: Float, height: Float) {
        // Draw road background (gray)
        drawScope.drawRect(
            color = Color(0xFF333333),
            topLeft = Offset(width * 0.15f, 0f),
            size = Size(width * 0.7f, height)
        )
        
        // Draw lane dividers
        val lane1X = width * 0.4f
        val lane2X = width * 0.6f
        
        var y = roadOffset
        while (y < height) {
            // Left lane divider
            drawScope.drawRect(
                color = Color.White,
                topLeft = Offset(lane1X - 2f, y),
                size = Size(4f, laneMarkingHeight)
            )
            
            // Right lane divider
            drawScope.drawRect(
                color = Color.White,
                topLeft = Offset(lane2X - 2f, y),
                size = Size(4f, laneMarkingHeight)
            )
            
            y += laneMarkingHeight + laneMarkingGap
        }
        
        // Draw road edges
        drawScope.drawRect(
            color = Color.Yellow,
            topLeft = Offset(width * 0.15f, 0f),
            size = Size(4f, height)
        )
        drawScope.drawRect(
            color = Color.Yellow,
            topLeft = Offset(width * 0.85f - 4f, 0f),
            size = Size(4f, height)
        )
    }
}

