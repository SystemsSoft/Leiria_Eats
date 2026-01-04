package org.leria.eats.project

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.permissions.PermissionStatus

@Preview
@Composable
fun MainScreenWithAI(permissionManager: PermissionManager) {
    val status by permissionManager.status.collectAsState()

    // Gradiente de fundo escuro para dar um ar moderno/tech
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A1A2E), // Azul muito escuro
            Color(0xFF16213E), // Azul noite
            Color(0xFF0F3460)  // Azul deep
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Título Superior
        Text(
            text = "Leria AI Eatss",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "O que você quer comer hoje?",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.LightGray.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(60.dp))

        // O Componente Central (Botão Mágico)
        CentralMicButton(
            status = status,
            onClick = {
                when (status) {
                    PermissionStatus.IDLE -> permissionManager.askForPermission()
                    PermissionStatus.DENIED -> permissionManager.openSettings()
                    PermissionStatus.GRANTED -> { /* Lógica de começar a ouvir */ }
                }
            }
        )

        Spacer(modifier = Modifier.height(60.dp))

        // Texto de Instrução Inferior
        StatusText(status)
    }
}

@Composable
fun CentralMicButton(
    status: PermissionStatus,
    onClick: () -> Unit
) {
    // Animação de "Pulso" quando está ouvindo
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == PermissionStatus.GRANTED) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Configuração visual baseada no estado
    val (icon, color, shadowColor) = when (status) {
        PermissionStatus.GRANTED -> Triple(Icons.Default.Mic, Color(0xFFE94560), Color(0x66E94560)) // Vermelho Neon
        PermissionStatus.DENIED -> Triple(Icons.Default.MicOff, Color(0xFF53354A), Color.Transparent) // Cinza/Roxo apagado
        PermissionStatus.IDLE -> Triple(Icons.Default.Mic, Color(0xFF0F3460), Color(0xFF4CB5F5)) // Azul Tech
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(140.dp)
    ) {
        // Círculo de "brilho" externo (animação)
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(shadowColor)
        )

        // Botão Principal
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(color)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (status == PermissionStatus.DENIED) Icons.Default.Settings else icon,
                contentDescription = "Mic",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun StatusText(status: PermissionStatus) {
    val message = when (status) {
        PermissionStatus.GRANTED -> "Estou ouvindo... Pode falar!"
        PermissionStatus.DENIED -> "Toque no ícone para abrir as configurações e liberar o áudio."
        PermissionStatus.IDLE -> "Toque no microfone para ativar"
    }

    val textColor = when (status) {
        PermissionStatus.GRANTED -> Color(0xFF4CAF50) // Verde
        PermissionStatus.DENIED -> Color(0xFFEF5350)  // Vermelho
        PermissionStatus.IDLE -> Color.White
    }

    Text(
        text = message,
        color = textColor,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}