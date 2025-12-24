package org.leria.eats.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview

// --- CORES PERSONALIZADAS PARA O MOCK (Vibe Futurista) ---
val NeonCyan = Color(0xFF00E5FF)
val DeepPurple = Color(0xFF6200EA)
val DarkBackground = Color(0xFF121212)
val CardDarkSurface = Color(0xFF1E1E1E)

data class MockRestaurant(val name: String, val rating: String, val time: String, val type: String)

val mockPizzerias = listOf(
    MockRestaurant("Pizzaria Vesúvio", "4.8", "30-40 min", "Italiana • A melhor da cidade"),
    MockRestaurant("Leiria Pizza Express", "4.5", "20-30 min", "Rápida • Massa fina"),
    MockRestaurant("Forno à Lenha do Zé", "4.9", "45-60 min", "Tradicional • Ingredientes premium"),
    MockRestaurant("CyberSlice", "4.2", "15-25 min", "Moderna • Entregas por drone (mock)"),
)

@Composable
@Preview
fun App() {
    // Forçamos um tema escuro para este mock ficar moderno
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBackground,
            primary = NeonCyan,
            surface = CardDarkSurface
        )
    ) {
        MainScreenWithAI()
    }
}

@Composable
fun MainScreenWithAI() {
    var inputText by remember { mutableStateOf("") }
    // Estado para controlar se mostramos a "resposta" da IA ou o estado inicial
    var hasAskedForSomething by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding() // Importante para não ficar sob a barra de status
    ) {
        // 1. Cabeçalho Minimalista
        HeaderSection()

        // 2. Área Central (O Palco da IA)
        // O weight(1f) faz essa área ocupar todo o espaço disponível entre o topo e a base
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (!hasAskedForSomething) {
                // Estado Inicial: A IA esperando
                WaitingAIState()
            } else {
                // Estado de Resposta: Mock dos resultados
                MockResultsList(userQuery = inputText)
            }
        }

        // 3. Área de Input (A barra de comando)
        InputArea(
            value = inputText,
            onValueChange = { inputText = it },
            onSendClick = {
                if (inputText.isNotBlank()) {
                    // Ao clicar em enviar, ativamos o estado de resposta
                    hasAskedForSomething = true
                }
            }
        )
    }
}

// --- COMPONENTES DE UI ---

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "LeiriaEats AI",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = NeonCyan
            )
            Text(
                text = "Olá, o que deseja comer?",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        // Ícone de perfil mock
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CardDarkSurface)
                .border(2.dp, NeonCyan, CircleShape)
        )
    }
}

@Composable
fun WaitingAIState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // O "Orbe" da IA (Um gradiente bonitão)
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(NeonCyan, DeepPurple)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Icon",
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "O que vamos pedir hoje?",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Light),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ex: \"Pizzarias abertas agora\" ou \"Comida japonesa barata\"",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
fun MockResultsList(userQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Simulação da mensagem do usuário e da resposta da IA
        Text(
            text = "Você: $userQuery",
            style = MaterialTheme.typography.bodyMedium,
            color = NeonCyan,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Text(
            text = "AI: Encontrei estas opções excelentes para você:",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Lista de restaurantes mockados
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mockPizzerias) { restaurant ->
                RestaurantCard(restaurant)
            }
        }
    }
}

@Composable
fun RestaurantCard(restaurant: MockRestaurant) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder para imagem do restaurante
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text("IMG", color = Color.LightGray)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Text(
                        text = " ${restaurant.rating} • ${restaurant.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Text(
                    text = restaurant.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun InputArea(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 16.dp), // Espaço extra para navegação por gestos
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            placeholder = { Text("Peça à sua IA...", color = Color.Gray) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = NeonCyan
            ),
            singleLine = true
        )

        // Botão de Enviar
        IconButton(
            onClick = onSendClick,
            modifier = Modifier
                .size(50.dp)
                .background(NeonCyan, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Enviar",
                tint = DeepPurple
            )
        }
    }
}