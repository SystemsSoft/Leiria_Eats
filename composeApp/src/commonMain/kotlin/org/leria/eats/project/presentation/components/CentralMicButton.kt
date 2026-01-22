package org.leria.eats.project.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.leria.eats.project.permissions.PermissionStatus

@Composable
fun CentralMicButton(
    status: PermissionStatus,
    isRecording: Boolean,
    onClick: () -> Unit
) {
    // Cores baseadas no novo tema cinza/dourado
    val goldColor = Color(0xFFFFD700)
    val darkGray = Color(0xFF333333)
    
    val targetColor = when {
        status == PermissionStatus.DENIED -> Color(0xFFF44336)
        isRecording -> goldColor
        else -> goldColor
    }
    val animatedColor by animateColorAsState(targetColor)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Ondas da esquerda (visíveis apenas gravando)
        if (isRecording) {
            WaveAnimation(color = goldColor)
            Spacer(modifier = Modifier.width(16.dp))
        }

        // Botão Central do Microfone
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(darkGray)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (status == PermissionStatus.DENIED) Icons.Default.Settings else Icons.Default.Mic,
                contentDescription = "Microfone",
                tint = animatedColor,
                modifier = Modifier.size(40.dp)
            )
        }

        // Ondas da direita (visíveis apenas gravando)
        if (isRecording) {
            Spacer(modifier = Modifier.width(16.dp))
            WaveAnimation(color = goldColor)
        }
    }
}

@Composable
fun WaveAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Criamos 3 barras com animações levemente defasadas para simular ondas sonoras
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            val heightScale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400 + (index * 150), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(30.dp * heightScale)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}