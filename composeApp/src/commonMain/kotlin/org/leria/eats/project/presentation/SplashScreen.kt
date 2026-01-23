package org.leria.eats.project.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val goldColor = Color(0xFFFFD700)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF2C2C2C), Color(0xFF121212))
    )

    // Animação de entrada (Escala do círculo)
    val scale = remember { Animatable(0f) }

    // Animação do Pato surgindo de dentro
    val duckOffsetY = remember { Animatable(100f) }

    // Animação para o "Piscar" do olho
    val blinkTransition = rememberInfiniteTransition()
    val eyeBlink by blinkTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500
                1.0f at 0
                1.0f at 2200
                0.1f at 2350 // Piscar rápido
                1.0f at 2500
            },
            repeatMode = RepeatMode.Restart
        )
    )

    LaunchedEffect(Unit) {
        // 1. Círculo aparece
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            )
        }

        // 2. Pato sobe de dentro do círculo
        delay(300)
        duckOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow)
        )

        delay(2500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Máscara Circular (Círculo Amarelo)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale.value)
                    .clip(CircleShape)
                    .background(goldColor),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Imagem do Pato com animação de Offset (Surgindo) e Piscar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = duckOffsetY.value.dp)
                ) {
                    KamelImage(
                        resource = asyncPainterResource("https://raw.githubusercontent.com/leria-eats/assets/main/pato_logo.png"),
                        contentDescription = "Logo Leiria Eats",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    )

                    // Desenho do "Piscar" por cima da imagem (simulado)
                    // Nota: Para um piscar perfeito, precisaríamos da posição exata do olho.
                    // Aplicamos um graphicsLayer na imagem inteira para simular o movimento da pálpebra.
                    KamelImage(
                        resource = asyncPainterResource("https://raw.githubusercontent.com/leria-eats/assets/main/pato_logo.png"),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .graphicsLayer {
                                // Aplica a animação de piscar reduzindo a escala Y focada no topo do olho
                                scaleY = eyeBlink
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Bem-vindo!",
                color = goldColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { alpha = scale.value }
            )
        }
    }
}
