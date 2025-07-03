package com.macsdev.fiverings

enum class Player { WHITE, BLACK, NONE }

// Логическое представление кольца, без координат
data class LogicalRing(val id: Int, val type: RingType)

enum class RingType { INNER, OUTER }

// Логическое представление точки, без координат
data class LogicalPoint(val id: Int, val ringIds: List<Int>)

data class Move(val ringId: Int, val direction: Int)

data class GameState(
    val boardState: Map<Int, Player>, // Карта состояний точек (ID точки -> игрок)
    val currentPlayer: Player,
    val lastMove: Move?,
    val scoreWhite: Int,
    val scoreBlack: Int
)
