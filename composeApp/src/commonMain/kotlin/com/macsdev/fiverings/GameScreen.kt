package com.macsdev.fiverings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Вспомогательный класс для информации о кнопках вращения из ViewModel
data class RotationButtonInfo(
    val ccwPos: Offset,
    val cwPos: Offset,
    val radius: Float
)

// Главный компонент экрана
@Composable
fun GameScreen(
    gameState: GameState,
    rings: List<Ring>,
    points: List<Point>,
    selectedRingId: Int?,
    rotationButtonInfo: RotationButtonInfo?,
    onExitClick: () -> Unit,
    onCanvasTap: (Offset) -> Unit
) {
    val highlightColor = Color(0xFF4A90E2)
    val backgroundColor = Color(0xFF1A202C)

    val colorScheme = darkColorScheme(
        primary = highlightColor,
        background = backgroundColor,
        surface = Color.White.copy(alpha = 0.05f),
        onPrimary = Color.White,
        onSurface = Color.White,
        onBackground = Color.White
    )

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
            BoxWithConstraints {
                val isPortrait = maxHeight > maxWidth

                if (isPortrait) {
                    // --- ПОРТРЕТНЫЙ РЕЖИМ ---
                    Column(
                        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Scoreboard(
                            scoreWhite = gameState.scoreWhite,
                            scoreBlack = gameState.scoreBlack,
                            currentPlayer = gameState.currentPlayer,
                            highlightColor = highlightColor
                        )
                        // Этот Box занимает оставшееся место и центрирует холст
                        Box(
                            modifier = Modifier.weight(1f).padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GameCanvas(
                                modifier = Modifier.fillMaxSize(),
                                rings = rings,
                                points = points,
                                gameState = gameState,
                                selectedRingId = selectedRingId,
                                rotationButtonInfo = rotationButtonInfo,
                                onCanvasTap = onCanvasTap
                            )
                        }
                        Button(
                            onClick = onExitClick,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text("Выйти", fontSize = 16.sp)
                        }
                    }
                } else {
                    // --- АЛЬБОМНЫЙ РЕЖИМ (с ошибкой растягивания) ---
                    Row(
                        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1.5f).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            GameCanvas(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                rings = rings,
                                points = points,
                                gameState = gameState,
                                selectedRingId = selectedRingId,
                                rotationButtonInfo = rotationButtonInfo,
                                onCanvasTap = onCanvasTap
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Scoreboard(
                                scoreWhite = gameState.scoreWhite,
                                scoreBlack = gameState.scoreBlack,
                                currentPlayer = gameState.currentPlayer,
                                highlightColor = highlightColor
                            )
                            Button(
                                onClick = onExitClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Выйти", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Scoreboard(
    modifier: Modifier = Modifier,
    scoreWhite: Int,
    scoreBlack: Int,
    currentPlayer: Player,
    highlightColor: Color
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val whiteModifier = if (currentPlayer == Player.WHITE) {
            Modifier.shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                .border(2.dp, highlightColor, RoundedCornerShape(12.dp))
        } else Modifier

        val blackModifier = if (currentPlayer == Player.BLACK) {
            Modifier.shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                .border(2.dp, highlightColor, RoundedCornerShape(12.dp))
        } else Modifier

        ScoreItem(modifier = whiteModifier, "Белые", scoreWhite)
        ScoreItem(modifier = blackModifier, "Черные", scoreBlack)
    }
}

@Composable
private fun ScoreItem(modifier: Modifier = Modifier, label: String, score: Int) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.7f))
        Text(score.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun GameCanvas(
    modifier: Modifier = Modifier,
    rings: List<Ring>,
    points: List<Point>,
    gameState: GameState,
    selectedRingId: Int?,
    rotationButtonInfo: RotationButtonInfo?,
    onCanvasTap: (Offset) -> Unit
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset -> onCanvasTap(offset) })
            }
    ) {
        rings.forEach { ring ->
            drawRing(ring, selectedRingId)
        }

        val stoneRadius = size.minDimension * 0.01f
        points.forEach { point ->
            val player = gameState.boardState[point.key]
            if (player != null && player != Player.NONE) {
                drawStone(point.x, point.y, stoneRadius, player)
            }
        }

        rotationButtonInfo?.let { info ->
            drawRotationButtons(info.ccwPos, info.cwPos, info.radius)
        }
    }
}

private fun DrawScope.drawRing(ring: Ring, selectedRingId: Int?) {
    val isSelected = ring.id == selectedRingId
    drawCircle(
        color = if (isSelected) Color(0xFF4A90E2) else Color.White.copy(alpha = 0.3f),
        radius = ring.radius,
        center = Offset(ring.x, ring.y),
        style = Stroke(width = if (isSelected) 3.dp.toPx() else 1.5.dp.toPx())
    )
}

private fun DrawScope.drawStone(x: Float, y: Float, radius: Float, player: Player) {
    drawCircle(
        color = if (player == Player.WHITE) Color.White else Color(0xFF212121),
        radius = radius,
        center = Offset(x, y)
    )
    if (player == Player.BLACK) {
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = radius,
            center = Offset(x, y),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun DrawScope.drawRotationButtons(ccwPos: Offset, cwPos: Offset, radius: Float) {
    val panelColor = Color.White.copy(alpha = 0.05f)
    val highlightColor = Color(0xFF4A90E2)
    drawCircle(color = panelColor, radius = radius, center = ccwPos)
    drawCircle(color = highlightColor, radius = radius, center = ccwPos, style = Stroke(2.dp.toPx()))
    drawCircle(color = panelColor, radius = radius, center = cwPos)
    drawCircle(color = highlightColor, radius = radius, center = cwPos, style = Stroke(2.dp.toPx()))
}