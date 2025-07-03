package com.macsdev.fiverings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.*

object GameViewModel {

    // --- Состояние, видимое для UI ---
    var gameState by mutableStateOf(createInitialGameState())
        private set
    var rings by mutableStateOf<List<Ring>>(emptyList())
        private set
    var points by mutableStateOf<List<Point>>(emptyList())
        private set
    var selectedRingId by mutableStateOf<Int?>(null)
        private set
    var rotationButtonInfo by mutableStateOf<RotationButtonInfo?>(null)
        private set

    // --- Приватные переменные ---
    private const val numMajorCircles = 5
    private const val stonesInQuadToWin = 4
    private var canvasSize = 0f

    fun setupGame(width: Float, height: Float) {
        this.canvasSize = minOf(width, height)

        val majorRadius = canvasSize * 0.17f
        val innerRadius = canvasSize * 0.25f
        val outerRadius = canvasSize * 0.28f
        val centerX = canvasSize / 2f
        val centerY = canvasSize / 2f

        val localRings = mutableListOf<Ring>()
        for (i in 0 until numMajorCircles) {
            val angle = (i.toFloat() / numMajorCircles) * 2 * PI - PI / 2
            val majorX = centerX + cos(angle.toFloat()) * majorRadius
            val majorY = centerY + sin(angle.toFloat()) * majorRadius
            localRings.add(Ring(i * 2, majorX, majorY, innerRadius, RingType.INNER))
            localRings.add(Ring(i * 2 + 1, majorX, majorY, outerRadius, RingType.OUTER))
        }

        val localPointsMap = mutableMapOf<String, Point>()
        for (i in localRings.indices) {
            for (j in i + 1 until localRings.size) {
                val intersections = calculateIntersections(localRings[i], localRings[j])
                for (p in intersections) {
                    val key = "${p.x.roundToInt()},${p.y.roundToInt()}"
                    val existingPoint = localPointsMap[key]
                    if (existingPoint == null) {
                        localPointsMap[key] = Point(key, p.x, p.y, listOf(i, j))
                    } else {
                        val updatedRings = (existingPoint.ringIds + i + j).distinct()
                        localPointsMap[key] = existingPoint.copy(ringIds = updatedRings)
                    }
                }
            }
        }
        val localPoints = localPointsMap.values.toList()

        val initialBoardState = mutableMapOf<String, Player>()
        localPoints.forEach { point ->
            if (point.ringIds.size >= 2) {
                val ring1Type = localRings.getOrNull(point.ringIds[0])?.type
                val ring2Type = localRings.getOrNull(point.ringIds[1])?.type
                if (ring1Type != null && ring2Type != null) {
                    initialBoardState[point.key] = if (ring1Type == ring2Type) Player.WHITE else Player.BLACK
                } else {
                    initialBoardState[point.key] = Player.NONE
                }
            } else {
                initialBoardState[point.key] = Player.NONE
            }
        }

        val calculatedQuads = mutableListOf<List<String>>()
        for (i in 0 until numMajorCircles) {
            for (j in i + 1 until numMajorCircles) {
                val cluster = localPoints.filter { p ->
                    val doubleRingIndices = p.ringIds.map { it / 2 }.toSet()
                    doubleRingIndices.contains(i) && doubleRingIndices.contains(j)
                }
                if (cluster.size >= stonesInQuadToWin) {
                    var remainingPoints = cluster.toMutableList()
                    while (remainingPoints.size >= stonesInQuadToWin) {
                        val seedPoint = remainingPoints.removeAt(0)
                        val quad = mutableListOf(seedPoint)
                        remainingPoints.sortBy { hypot(it.x - seedPoint.x, it.y - seedPoint.y) }
                        quad.addAll(remainingPoints.take(3))
                        remainingPoints = remainingPoints.drop(3).toMutableList()
                        calculatedQuads.add(quad.map { it.key })
                    }
                }
            }
        }

        val newGameState = GameState(
            boardState = initialBoardState,
            currentPlayer = Player.WHITE,
            lastMove = null,
            scoreWhite = 0,
            scoreBlack = 0,
            winningQuads = calculatedQuads
        )

        this.rings = localRings
        this.points = localPoints
        this.gameState = newGameState
        this.selectedRingId = null
        this.rotationButtonInfo = null
    }

    fun onCanvasTap(offset: Offset) {
        rotationButtonInfo?.let { info ->
            if (hypot(offset.x - info.ccwPos.x, offset.y - info.ccwPos.y) < info.radius) {
                handleRotation(-1)
                return
            }
            if (hypot(offset.x - info.cwPos.x, offset.y - info.cwPos.y) < info.radius) {
                handleRotation(1)
                return
            }
        }

        val clickTolerance = canvasSize * 0.05f
        val bestMatch = rings.minByOrNull { ring ->
            val dist = abs(hypot(offset.x - ring.x, offset.y - ring.y) - ring.radius)
            if (dist < clickTolerance) dist else Float.MAX_VALUE
        }

        if (bestMatch != null && abs(hypot(offset.x - bestMatch.x, offset.y - bestMatch.y) - bestMatch.radius) < clickTolerance) {
            selectedRingId = bestMatch.id
            val buttonRadius = canvasSize * 0.03f
            val buttonOffset = buttonRadius * 2.5f
            rotationButtonInfo = RotationButtonInfo(
                ccwPos = Offset(offset.x - buttonOffset, offset.y),
                cwPos = Offset(offset.x + buttonOffset, offset.y),
                radius = buttonRadius
            )
        } else {
            selectedRingId = null
            rotationButtonInfo = null
        }
    }

    private fun handleRotation(direction: Int) {
        val ringId = selectedRingId ?: return
        val currentGameState = gameState
        if (currentGameState.lastMove?.ringId == ringId && currentGameState.lastMove.direction == -direction) {
            return
        }

        val ringToRotate = rings.find { it.id == ringId } ?: return
        val pointsOnRing = points.filter { it.ringIds.contains(ringId) }
            .sortedBy { atan2(it.y - ringToRotate.y, it.x - ringToRotate.x) }

        if (pointsOnRing.isEmpty()) return

        val currentBoard = currentGameState.boardState.toMutableMap()
        val pointKeys = pointsOnRing.map { it.key }
        val colors = pointKeys.mapNotNull { currentBoard[it] }

        if (colors.size != pointKeys.size) return

        val newColors = if (direction == 1) {
            listOf(colors.last()) + colors.dropLast(1)
        } else {
            colors.drop(1) + colors.first()
        }

        pointKeys.forEachIndexed { index, key ->
            currentBoard[key] = newColors[index]
        }

        val scoredPlayers = checkAndProcessQuads(currentBoard, currentGameState.winningQuads)
        val quadsProcessed = scoredPlayers.isNotEmpty()

        var scoreChangeWhite = 0
        var scoreChangeBlack = 0
        scoredPlayers.forEach { player ->
            if (player == Player.WHITE) scoreChangeWhite++
            if (player == Player.BLACK) scoreChangeBlack++
        }

        gameState = currentGameState.copy(
            boardState = currentBoard,
            lastMove = Move(ringId, direction),
            currentPlayer = if (quadsProcessed) currentGameState.currentPlayer else if (currentGameState.currentPlayer == Player.WHITE) Player.BLACK else Player.WHITE,
            scoreWhite = currentGameState.scoreWhite + scoreChangeWhite,
            scoreBlack = currentGameState.scoreBlack + scoreChangeBlack
        )

        selectedRingId = null
        rotationButtonInfo = null
    }

    private fun checkAndProcessQuads(board: MutableMap<String, Player>, winningQuads: List<List<String>>): List<Player> {
        val scoredPlayers = mutableListOf<Player>()
        val pointsToRemove = mutableSetOf<String>()

        for (quadKeys in winningQuads) {
            if (quadKeys.any { pointsToRemove.contains(it) }) continue
            val firstPlayer = board[quadKeys.firstOrNull()]
            if (firstPlayer == null || firstPlayer == Player.NONE) continue
            if (quadKeys.all { board[it] == firstPlayer }) {
                scoredPlayers.add(firstPlayer)
                pointsToRemove.addAll(quadKeys)
            }
        }

        if (pointsToRemove.isNotEmpty()) {
            pointsToRemove.forEach { board[it] = Player.NONE }
        }
        return scoredPlayers
    }

    private fun calculateIntersections(c1: Ring, c2: Ring): List<Offset> {
        val d = hypot(c2.x - c1.x, c2.y - c1.y)
        if (d > c1.radius + c2.radius || d < abs(c1.radius - c2.radius) || d == 0f) return emptyList()
        val a = (c1.radius.pow(2) - c2.radius.pow(2) + d.pow(2)) / (2 * d)
        val h = sqrt(max(0f, c1.radius.pow(2) - a.pow(2)))
        val x0 = c1.x + a * (c2.x - c1.x) / d
        val y0 = c1.y + a * (c2.y - c1.y) / d
        val rx = -h * (c2.y - c1.y) / d
        val ry = h * (c2.x - c1.x) / d
        return listOf(Offset(x0 + rx, y0 + ry), Offset(x0 - rx, y0 - ry))
    }

    private fun createInitialGameState(): GameState = GameState(emptyMap(), Player.WHITE, null, 0, 0, emptyList())
}

private fun atan2(y: Float, x: Float): Float {
    return kotlin.math.atan2(y.toDouble(), x.toDouble()).toFloat()
}