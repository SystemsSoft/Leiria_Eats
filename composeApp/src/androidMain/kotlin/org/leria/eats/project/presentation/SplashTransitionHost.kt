package org.leria.eats.project.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * Reproduz [VideoSplashScreen] e, ao terminar, funde para [content]: um flash de brilho
 * rápido (evocando os brilhos do próprio vídeo) seguido de um fade-out do vídeo que
 * revela a tela seguinte por baixo.
 */
@Composable
fun SplashTransitionHost(content: @Composable () -> Unit) {
    var videoFinished by remember { mutableStateOf(false) }
    val splashAlpha = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(videoFinished) {
        if (videoFinished) {
            launch {
                glowAlpha.animateTo(1f, animationSpec = tween(durationMillis = 180))
                glowAlpha.animateTo(0f, animationSpec = tween(durationMillis = 500))
            }
            splashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 700))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (videoFinished) {
            content()
        }

        if (splashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = splashAlpha.value }
            ) {
                VideoSplashScreen(onFinished = { videoFinished = true })

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = glowAlpha.value }
                        .background(
                            Brush.radialGradient(colors = listOf(Color.White, Color.Transparent))
                        )
                )
            }
        }
    }
}
