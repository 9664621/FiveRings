package com.macsdev.fiverings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.*

object GameViewModel {

    // --- ПОРЯДОК ИНИЦИАЛИЗАЦИИ ИСПРАВЛЕН ---
    // Сначала создаем логическую структуру...
    val logicalRings = createLogicalRings()
    val logicalPoints = createLogicalPoints(logicalRings)
    val winningQuads = calculateWinningQuads(logicalPoints)

    // ...и только потом создаем состояние игры, которое от них зависит.
    var gameState by mutableStateOf(createInitialGameState())
        private set
    var selectedRingId by mutableStateOf<Int?>(null)
        private set

    private fun createInitialGameState(): GameState {
        val board = mutableMapOf<Int, Player>()
        logicalPoints.forEachIndexed { index, point ->
            if (point.ringIds.size >= 2) {
                val ring1Type = logicalRings[point.ringIds[0]].type
                val ring2Type = logicalRings[point.ringIds[1]].type
                board[index] = if (ring1Type == ring2Type) Player.WHITE else Player.BLACK
            } else {
                board[index] = Player.NONE
            }
        }
        return GameState(board, Player.WHITE, null, 0, 0)
    }

    fun onNewGame() {
        gameState = createInitialGameState()
        selectedRingId = null
    }

    fun onRingSelected(ringId: Int?) {
        selectedRingId = ringId
    }

    fun onRotate(direction: Int) {
        val ringId = selectedRingId ?: return

        val currentGameState = gameState
        if (currentGameState.lastMove?.ringId == ringId && currentGameState.lastMove.direction == -direction) {
            return // Запрет отмены хода
        }

        val pointsOnRing = logicalPoints.filter { it.ringIds.contains(ringId) }
        if (pointsOnRing.isEmpty()) return

        val currentBoard = currentGameState.boardState.toMutableMap()
        val pointIds = pointsOnRing.map { it.id }
        val colors = pointIds.mapNotNull { currentBoard[it] }

        if (colors.size != pointIds.size) return

        val newColors = if (direction == 1) {
            listOf(colors.last()) + colors.dropLast(1)
        } else {
            colors.drop(1) + colors.first()
        }

        pointIds.forEachIndexed { index, id ->
            currentBoard[id] = newColors[index]
        }

        val scoredPlayers = checkAndProcessQuads(currentBoard)
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
    }

    private fun checkAndProcessQuads(board: MutableMap<Int, Player>): List<Player> {
        val scoredPlayers = mutableListOf<Player>()
        val pointsToRemove = mutableSetOf<Int>()

        for (quad in winningQuads) {
            if (quad.any { pointsToRemove.contains(it) }) continue

            val firstPlayer = board[quad.first()] ?: Player.NONE
            if (firstPlayer == Player.NONE) continue

            if (quad.all { board[it] == firstPlayer }) {
                scoredPlayers.add(firstPlayer)
                pointsToRemove.addAll(quad)
            }
        }

        if (pointsToRemove.isNotEmpty()) {
            pointsToRemove.forEach { board[it] = Player.NONE }
        }
        return scoredPlayers
    }
}

// Функции создания логической структуры доски
private fun createLogicalRings(): List<LogicalRing> {
    return (0 until 10).map { id ->
        LogicalRing(id, if (id % 2 == 0) RingType.INNER else RingType.OUTER)
    }
}

// Эта функция теперь создает только логические связи, без координат
private fun createLogicalPoints(rings: List<LogicalRing>): List<LogicalPoint> {
    val tempPoints = mutableListOf<Pair<Float, Float>>()
    val ringConnections = mutableMapOf<String, MutableList<Int>>()

    // Эмулируем геометрию для нахождения пересечений
    val size = 800f
    val majorRadius = size * 0.17f
    val innerRadius = size * 0.25f
    val outerRadius = size * 0.28f
    val centerX = size / 2f
    val centerY = size / 2f

    val tempUiRings = rings.map {
        val angle = (it.id / 2).toFloat() / 5f * 2f * PI.toFloat() - PI.toFloat() / 2f
        val majorX = centerX + cos(angle) * majorRadius
        val majorY = centerY + sin(angle) * majorRadius
        object { val id = it.id; val x = majorX; val y = majorY; val radius = if(it.type == RingType.INNER) innerRadius else outerRadius }
    }

    for (i in tempUiRings.indices) {
        for (j in i + 1 until tempUiRings.size) {
            val c1 = tempUiRings[i]
            val c2 = tempUiRings[j]
            val d = hypot(c2.x - c1.x, c2.y - c1.y)
            if (d > c1.radius + c2.radius || d < abs(c1.radius - c2.radius) || d == 0f) continue

            val a = (c1.radius.pow(2) - c2.radius.pow(2) + d.pow(2)) / (2 * d)
            val h = sqrt(max(0f, c1.radius.pow(2) - a.pow(2)))
            val x0 = c1.x + a * (c2.x - c1.x) / d
            val y0 = c1.y + a * (c2.y - c1.y) / d

            val rx = -h * (c2.y - c1.y) / d
            val ry = h * (c2.x - c1.x) / d

            val p1 = Pair(x0 + rx, y0 + ry)
            val p2 = Pair(x0 - rx, y0 - ry)

            listOf(p1, p2).forEach { p ->
                val key = "${p.first.roundToInt()},${p.second.roundToInt()}"
                if (!ringConnections.containsKey(key)) {
                    ringConnections[key] = mutableListOf()
                }
                ringConnections[key]?.add(c1.id)
                ringConnections[key]?.add(c2.id)
            }
        }
    }

    return ringConnections.values
        .map { it.distinct().sorted() }
        .distinct()
        .mapIndexed { index, ringIds -> LogicalPoint(index, ringIds) }
}

private fun calculateWinningQuads(points: List<LogicalPoint>): List<List<Int>> {
    val quads = mutableListOf<List<Int>>()
    for (i in 0 until 5) {
        for (j in i + 1 until 5) {
            val cluster = points.filter { p ->
                val doubleRingIndices = p.ringIds.map { it / 2 }.toSet()
                doubleRingIndices.size == 2 && doubleRingIndices.contains(i) && doubleRingIndices.contains(j)
            }
            if (cluster.size >= 4) {
                if (cluster.size > 4) { // Разделяем на 2 квада
                    val seedPoint = cluster.first()
                    val sortedCluster = cluster.sortedBy { p1 ->
                        // Это очень упрощенная эмуляция расстояния без координат
                        (p1.ringIds.sum() - seedPoint.ringIds.sum()).absoluteValue
                    }
                    quads.add(sortedCluster.take(4).map { it.id })
                    quads.add(sortedCluster.takeLast(4).map { it.id })
                } else {
                    quads.add(cluster.map { it.id })
                }
            }
        }
    }
    return quads.filter { it.size == 4 }.distinct()
}