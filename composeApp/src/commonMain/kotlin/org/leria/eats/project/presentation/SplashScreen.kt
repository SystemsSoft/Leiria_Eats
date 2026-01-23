package org.leria.eats.project.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fade in
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
        )
        
        delay(1000) // Mantém visível por 1 segundo
        
        // Fade out
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
        )
        
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)), // Fundo cinza muito escuro/preto conforme o tema
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "LEIRIA EATS",
            color = Color(0xFFFFD700), // Dourado
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha.value
            }
        )
    }
}
