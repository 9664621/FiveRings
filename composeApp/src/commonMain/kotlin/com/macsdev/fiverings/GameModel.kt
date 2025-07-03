package com.macsdev.fiverings

enum class Player { WHITE, BLACK, NONE }

data class LogicalRing(val id: Int, val type: RingType)

enum class RingType { INNER, OUTER }

data class LogicalPoint(val id: Int, val ringIds: List<Int>)

data class Move(val ringId: Int, val direction: Int)

data class GameState(
    val boardState: Map<Int, Player>,
    val currentPlayer: Player,
    val lastMove: Move?,
    val scoreWhite: Int,
    val scoreBlack: Int
)
