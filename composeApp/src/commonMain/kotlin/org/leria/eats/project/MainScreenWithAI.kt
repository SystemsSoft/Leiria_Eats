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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.leria.eats.project.data.LeriaApiClient
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.voice.VoiceRecognizer

@Composable
fun MainScreenWithAI(permissionManager: PermissionManager) {
    // 1. INJEÇÃO DE DEPENDÊNCIAS
    val voiceRecognizer = koinInject<VoiceRecognizer>()
    val apiClient = koinInject<LeriaApiClient>()

    // 2. COLETA DE ESTADOS
    val status by permissionManager.status.collectAsState()
    val recognizedText by voiceRecognizer.results.collectAsState()
    val isListening by voiceRecognizer.isListening.collectAsState()
    val error by voiceRecognizer.error.collectAsState()

    // 3. ESTADOS DA TELA
    var textInput by remember { mutableStateOf("") }
    var aiReply by remember { mutableStateOf("Olá! O que vamos comer hoje?") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // --- SINCRONIZAÇÃO VOZ -> TEXTO ---
    LaunchedEffect(recognizedText) {
        // Só atualiza se estiver ouvindo e tiver texto novo
        if (isListening && recognizedText.isNotEmpty()) {
            textInput = recognizedText
        }
    }

    // Se perder permissão, para tudo
    LaunchedEffect(status) {
        if (status != PermissionStatus.GRANTED) {
            voiceRecognizer.stopListening()
        }
    }

    // Fundo Gradiente
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título
        Text(
            text = "Leria AI Assistant",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        // Resposta do Garçom (IA)
        Text(
            text = aiReply,
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF4CB5F5),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- BOTÃO CENTRAL (GRAVAR / PAUSAR) ---
        CentralMicButton(
            status = status,
            isRecording = isListening,
            onClick = {
                when (status) {
                    PermissionStatus.IDLE -> permissionManager.askForPermission()
                    PermissionStatus.DENIED -> permissionManager.openSettings()
                    PermissionStatus.GRANTED -> {
                        if (isListening) {
                            // AÇÃO DE PAUSE: Para de ouvir, mas mantém o texto
                            voiceRecognizer.stopListening()
                        } else {
                            // AÇÃO DE INICIAR: Limpa o campo e começa a ouvir
                            textInput = ""
                            voiceRecognizer.startListening()
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- CAMPO DE TEXTO + BOTÃO ENVIAR ---
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it }, // Permite editar manualmente
            label = { Text("Seu pedido", color = Color.White.copy(alpha = 0.8f)) },
            placeholder = { Text(if (isListening) "Ouvindo..." else "Fale ou digite...", color = Color.Gray) },
            enabled = status == PermissionStatus.GRANTED && !isLoading,
            maxLines = 3,
            trailingIcon = {
                // Botão de Enviar (Aviãozinho)
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            // Garante que parou de ouvir antes de enviar
                            if (isListening) voiceRecognizer.stopListening()

                            scope.launch {
                                isLoading = true
                                try {
                                    val response = apiClient.sendChat(textInput)

                                    val nomesRestaurantes = response.results.joinToString { it.name }

                                    if (response.results.isNotEmpty()) {
                                        aiReply = "${response.reply}\n(Encontrados: $nomesRestaurantes)"
                                    } else {
                                        aiReply = response.reply
                                    }

                                    textInput = ""
                                } catch (e: Exception) {
                                    aiReply = "Erro: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = textInput.isNotBlank() && !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = if (textInput.isNotBlank()) Color(0xFFE94560) else Color.Gray
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFE94560),
                focusedBorderColor = Color(0xFFE94560),
                unfocusedBorderColor = Color(0xFF0F3460),
                disabledBorderColor = Color.Gray.copy(alpha = 0.3f),
                disabledTextColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Exibe erro do reconhecedor se houver
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error ?: "", color = Color(0xFFEF5350), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Texto de Status no rodapé
        StatusText(status, isListening, isLoading)
    }
}

@Composable
fun CentralMicButton(
    status: PermissionStatus,
    isRecording: Boolean,
    onClick: () -> Unit
) {
    // Animação de Pulso
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Cores
    val targetColor = when {
        status == PermissionStatus.DENIED -> Color(0xFF53354A) // Roxo Escuro
        isRecording -> Color(0xFFE94560)                       // Vermelho (Gravando)
        status == PermissionStatus.GRANTED -> Color(0xFF4CAF50)// Verde (Pronto)
        else -> Color(0xFF0F3460)
    }

    val animatedColor by animateColorAsState(targetColor)

    // ÍCONES: Aqui mudamos para PAUSE quando estiver gravando
    val icon = when {
        status == PermissionStatus.DENIED -> Icons.Default.Settings
        isRecording -> Icons.Default.Pause // <--- ÍCONE DE PAUSE
        else -> Icons.Default.Mic
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(140.dp)
    ) {
        // Efeito de onda
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color(0x66E94560))
            )
        }

        // Botão Físico
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

@Composable
fun StatusText(status: PermissionStatus, isRecording: Boolean, isLoading: Boolean) {
    val (text, color) = when {
        isLoading -> "Processando com IA..." to Color(0xFF4CB5F5)
        status == PermissionStatus.DENIED -> "Permissão negada." to Color(0xFFEF5350)
        isRecording -> "Toque para pausar" to Color(0xFFE94560) // Texto atualizado
        status == PermissionStatus.GRANTED -> "Toque para falar" to Color(0xFF4CAF50)
        else -> "Toque para ativar" to Color.White
    }

    Text(
        text = text,
        color = color,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}