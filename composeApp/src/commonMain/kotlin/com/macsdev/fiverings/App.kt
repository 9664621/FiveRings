package com.macsdev.fiverings

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    // Используем единый экземпляр GameViewModel, чтобы сохранять состояние при повороте экрана
    val viewModel = GameViewModel

    // Получаем все необходимые данные из ViewModel
    val gameState = viewModel.gameState
    val rings = viewModel.rings
    val points = viewModel.points
    val isInitialized = rings.isNotEmpty() // Проверяем, была ли игра инициализирована

    // BoxWithConstraints позволяет получить реальные размеры экрана
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // LaunchedEffect запускает инициализацию игры только один раз,
        // когда размеры экрана становятся известны
        LaunchedEffect(Unit) {
            if (!isInitialized) {
                viewModel.setupGame(width, height)
            }
        }

        // Показываем игру, только когда все данные готовы
        if (isInitialized) {
            GameScreen(
                gameState = gameState,
                rings = rings,
                points = points,
                selectedRingId = viewModel.selectedRingId,
                rotationButtonInfo = viewModel.rotationButtonInfo,
                onExitClick = { viewModel.setupGame(width, height) },
                onCanvasTap = { offset -> viewModel.onCanvasTap(offset) }
            )
        } else {
            // В противном случае показываем индикатор загрузки
            CircularProgressIndicator()
        }
    }
}
