package org.leria.eats.project

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.koin.compose.koinInject
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.voice.VoiceRecognizer

@Composable
fun MainScreenWithAI(permissionManager: PermissionManager) {
    // 1. Injeção de Dependências
    val voiceRecognizer = koinInject<VoiceRecognizer>()

    // 2. Coleta de Estados (Observables)
    val status by permissionManager.status.collectAsState()
    val recognizedText by voiceRecognizer.results.collectAsState()
    val isListening by voiceRecognizer.isListening.collectAsState()
    val error by voiceRecognizer.error.collectAsState()

    // 3. Estado Local para Edição Manual
    var textInput by remember { mutableStateOf("") }

    // Efeito: Atualiza o campo de texto enquanto a voz é reconhecida
    LaunchedEffect(recognizedText) {
        if (isListening) {
            textInput = recognizedText
        }
    }

    // Efeito: Se perder a permissão, para de ouvir
    LaunchedEffect(status) {
        if (status != PermissionStatus.GRANTED) {
            voiceRecognizer.stopListening()
        }
    }

    // Design: Fundo Gradiente
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A1A2E), // Dark Blue
            Color(0xFF16213E), // Night Blue
            Color(0xFF0F3460)  // Deep Blue
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
        // --- CABEÇALHO ---
        Text(
            text = "Leria AI Assistant",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (isListening) "Ouvindo você..." else "O que você quer comer hoje?",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isListening) Color(0xFF4CB5F5) else Color.LightGray.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- BOTÃO CENTRAL ANIMADO ---
        CentralMicButton(
            status = status,
            isRecording = isListening,
            onClick = {
                when (status) {
                    PermissionStatus.IDLE -> permissionManager.askForPermission()
                    PermissionStatus.DENIED -> permissionManager.openSettings()
                    PermissionStatus.GRANTED -> {
                        if (isListening) {
                            voiceRecognizer.stopListening()
                        } else {
                            voiceRecognizer.startListening()
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- CAMPO DE TEXTO (Resultado + Edição) ---
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it }, // Permite editar o texto da IA
            label = { Text("Seu pedido", color = Color.White.copy(alpha = 0.8f)) },
            placeholder = { Text("Fale ou digite aqui...", color = Color.Gray) },
            enabled = status == PermissionStatus.GRANTED,
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFE94560),
                focusedBorderColor = Color(0xFFE94560), // Borda Vermelha Neon quando focado
                unfocusedBorderColor = Color(0xFF0F3460),
                disabledBorderColor = Color.Gray.copy(alpha = 0.3f),
                disabledTextColor = Color.Gray
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        // Exibe erro caso ocorra (ex: sem internet ou API indisponível)
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error ?: "",
                color = Color(0xFFEF5350),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- RODAPÉ ---
        StatusText(status, isListening)
    }
}

@Composable
fun CentralMicButton(
    status: PermissionStatus,
    isRecording: Boolean,
    onClick: () -> Unit
) {
    // Animação de Pulso (Scale)
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Cores baseadas no estado
    val targetColor = when {
        status == PermissionStatus.DENIED -> Color(0xFF53354A) // Roxo Escuro (Erro)
        isRecording -> Color(0xFFE94560)                       // Vermelho Neon (Gravando)
        status == PermissionStatus.GRANTED -> Color(0xFF4CAF50)// Verde (Pronto)
        else -> Color(0xFF0F3460)                              // Azul (Idle)
    }

    val animatedColor by animateColorAsState(targetColor)

    // Ícone baseado no estado
    val icon = when {
        status == PermissionStatus.DENIED -> Icons.Default.Settings
        isRecording -> Icons.Default.Stop
        else -> Icons.Default.Mic
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(140.dp)
    ) {
        // Camada de Brilho/Sombra pulsante
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color(0x66E94560)) // Transparente do vermelho
            )
        }

        // O Botão físico
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
                contentDescription = "Microfone",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun StatusText(status: PermissionStatus, isRecording: Boolean) {
    val (text, color) = when {
        status == PermissionStatus.DENIED -> "Permissão negada. Toque para abrir ajustes." to Color(0xFFEF5350)
        isRecording -> "Toque novamente para finalizar" to Color(0xFFE94560)
        status == PermissionStatus.GRANTED -> "Toque no microfone para falar" to Color(0xFF4CAF50)
        else -> "Toque para ativar o assistente" to Color.White
    }

    Text(
        text = text,
        color = color,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}