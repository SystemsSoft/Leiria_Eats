package org.leria.eats.project.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.presentation.components.CentralMicButton
import org.leria.eats.project.presentation.components.RestaurantCard

@Composable
fun HomeScreen(
    uiState: SearchUiState,
    isListening: Boolean,
    permissionStatus: PermissionStatus,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onRestaurantClick: (Restaurant) -> Unit
) {
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

        // --- BOTÃO "VER TODOS" ---
        // Só mostramos se não tiver texto digitado
        if (uiState.textInput.isEmpty()) {
            OutlinedButton(
                onClick = {
                    // Truque: Mandamos o texto "ver todos" para o Python
                    onTextChange("ver todos")
                    onSendClick()
                },
                border = BorderStroke(1.dp, Color(0xFFE94560)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE94560)),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver todos os restaurantes")
            }
        }

        // --- LISTA OU PLACEHOLDER ---
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.restaurants.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.restaurants) { restaurant ->
                        RestaurantCard(
                            restaurant = restaurant,
                            onClick = { onRestaurantClick(restaurant) }
                        )
                    }
                }
            } else if (!uiState.isLoading && uiState.textInput.isEmpty()) {
                // Estado vazio (Placeholder)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Não sabe o que pedir?",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Fale 'Pizza' ou clique em Ver Todos",
                        color = Color.Gray.copy(0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Botão Microfone
        CentralMicButton(
            status = permissionStatus,
            isRecording = isListening,
            onClick = onMicClick
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Campo de Texto
        OutlinedTextField(
            value = uiState.textInput,
            onValueChange = onTextChange,
            label = { Text("Seu pedido", color = Color.White.copy(0.8f)) },
            placeholder = { Text(if (isListening) "Ouvindo..." else "Digite...", color = Color.Gray) },
            enabled = !uiState.isLoading,
            trailingIcon = {
                IconButton(
                    onClick = onSendClick,
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

        // Status
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