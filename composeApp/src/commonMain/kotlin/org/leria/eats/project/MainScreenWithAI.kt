package org.leria.eats.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject // Para o VoiceRecognizer que é hardware
import org.koin.compose.viewmodel.koinViewModel
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.presentation.SearchViewModel
import org.leria.eats.project.presentation.components.CentralMicButton
import org.leria.eats.project.voice.VoiceRecognizer

@Composable
fun MainScreenWithAI(
    permissionManager: PermissionManager,
    // Injetamos o ViewModel aqui automaticamente
    viewModel: SearchViewModel = koinViewModel()
) {
    // 1. Estados
    val uiState by viewModel.uiState.collectAsState()

    // VoiceRecognizer continua aqui pois lida com Hardware/Permissões da Activity
    val voiceRecognizer = koinInject<VoiceRecognizer>()
    val voiceText by voiceRecognizer.results.collectAsState()
    val isListening by voiceRecognizer.isListening.collectAsState()
    val permissionStatus by permissionManager.status.collectAsState()

    // 2. Sincronização Voz -> ViewModel
    LaunchedEffect(voiceText) {
        if (isListening && voiceText.isNotEmpty()) {
            viewModel.updateInputFromVoice(voiceText)
        }
    }

    // Se perder permissão, para de ouvir
    LaunchedEffect(permissionStatus) {
        if (permissionStatus != PermissionStatus.GRANTED) voiceRecognizer.stopListening()
    }

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
            text = "Leria Eats AI",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        // Resposta da IA
        Text(
            text = uiState.aiReply,
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF4CB5F5),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        // --- LISTA DE RESTAURANTES (Temporária: Mostra apenas nomes) ---
        // Aqui entraremos com os Cards visuais no próximo passo
        if (uiState.restaurants.isNotEmpty()) {
            Text(
                text = "Encontrados: ${uiState.restaurants.joinToString { it.name }}",
                color = Color.Green,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Botão do Microfone (Hardware)
        CentralMicButton(
            status = permissionStatus,
            isRecording = isListening,
            onClick = {
                when (permissionStatus) {
                    PermissionStatus.IDLE -> permissionManager.askForPermission()
                    PermissionStatus.DENIED -> permissionManager.openSettings()
                    PermissionStatus.GRANTED -> {
                        if (isListening) voiceRecognizer.stopListening()
                        else {
                            viewModel.onQueryChange("") // Limpa texto antigo
                            voiceRecognizer.startListening()
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Campo de Texto
        OutlinedTextField(
            value = uiState.textInput,
            onValueChange = { viewModel.onQueryChange(it) },
            label = { Text("Seu pedido", color = Color.White.copy(0.8f)) },
            placeholder = { Text(if (isListening) "Ouvindo..." else "Digite...", color = Color.Gray) },
            enabled = !uiState.isLoading,
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (isListening) voiceRecognizer.stopListening()
                        viewModel.sendSearch()
                    },
                    enabled = uiState.textInput.isNotBlank() && !uiState.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = if (uiState.textInput.isNotBlank()) Color(0xFFE94560) else Color.Gray
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFE94560),
                focusedBorderColor = Color(0xFFE94560),
                unfocusedBorderColor = Color(0xFF0F3460)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Mensagens de Status/Erro
        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Processando IA...", color = Color(0xFF4CB5F5))
        }

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(uiState.error!!, color = Color.Red)
        }
    }
}