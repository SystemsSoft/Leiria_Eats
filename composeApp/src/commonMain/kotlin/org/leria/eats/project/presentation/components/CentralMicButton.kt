package org.leria.eats.project.presentation.components


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.leria.eats.project.permissions.PermissionStatus

@Composable
fun CentralMicButton(
    status: PermissionStatus,
    isRecording: Boolean,
    onClick: () -> Unit
) {
    // 1. Animação de Pulso
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 2. Cores
    val targetColor = when {
        status == PermissionStatus.DENIED -> Color(0xFF53354A)
        isRecording -> Color(0xFFE94560)
        status == PermissionStatus.GRANTED -> Color(0xFF4CAF50)
        else -> Color(0xFF0F3460)
    }
    val animatedColor by animateColorAsState(targetColor)

    // 3. Ícone
    val icon = when {
        status == PermissionStatus.DENIED -> Icons.Default.Settings
        isRecording -> Icons.Default.Pause
        else -> Icons.Default.Mic
    }

    // 4. Layout
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(140.dp)
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color(0x66E94560))
            )
        }

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(animatedColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isRecording) "Pausar" else "Gravar",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}