package com.macsdev.fiverings

enum class Player { WHITE, BLACK, NONE }

data class Ring(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float,
    val type: RingType
)

enum class RingType { INNER, OUTER }

data class Point(
    val key: String,
    val x: Float,
    val y: Float,
    val ringIds: List<Int>
)

data class Move(
    val ringId: Int,
    val direction: Int
)

data class GameState(
    val boardState: Map<String, Player>,
    val currentPlayer: Player,
    val lastMove: Move?,
    val scoreWhite: Int,
    val scoreBlack: Int,
    val winningQuads: List<List<String>>
)
