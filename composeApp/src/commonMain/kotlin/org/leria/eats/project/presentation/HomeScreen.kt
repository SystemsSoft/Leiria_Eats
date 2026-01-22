package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.presentation.components.CentralMicButton

@Composable
fun HomeScreen(
    uiState: SearchUiState,
    isListening: Boolean,
    permissionStatus: PermissionStatus,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onRestaurantClick: (Restaurant) -> Unit,
    onClearSelection: () -> Unit,
    onAddToCart: (Product) -> Unit,
    onRemoveFromCart: (Product) -> Unit,
    onViewCart: () -> Unit
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF2C2C2C), Color(0xFF1E1E1E), Color(0xFF121212))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- CABEÇALHO ---
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Leiria Eats",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        // Resposta da IA (Destaque)
        Text(
            text = if (uiState.selectedRestaurant == null) uiState.aiReply else "Cardápio de ${uiState.selectedRestaurant?.name}",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFBDBDBD),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth()
        )

        // --- MICROFONE ---
        CentralMicButton(
            status = permissionStatus,
            isRecording = isListening,
            onClick = onMicClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- CONTEÚDO DINÂMICO (Lista ou Cardápio) ---
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.selectedRestaurant != null) {
                // EXIBE O CARDÁPIO NO MESMO ESPAÇO
                RestaurantDetailScreen(
                    restaurant = uiState.selectedRestaurant!!,
                    cartItems = uiState.cartItems,
                    onBack = onClearSelection,
                    onAdd = onAddToCart,
                    onRemove = onRemoveFromCart,
                    onViewCart = onViewCart
                )
            } else {
                // EXIBE A LISTA DE RESTAURANTES
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFFBDBDBD),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (uiState.restaurants.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "Sugestões encontradas:",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(uiState.restaurants) { restaurant ->
                            RestaurantCardItem(
                                restaurant = restaurant,
                                onClick = { onRestaurantClick(restaurant) }
                            )
                        }
                    }
                } else {
                    // Estado Vazio
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Não sabe o que pedir?",
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Fale 'Pizza' ou 'Quero algo barato'",
                            color = Color.Gray.copy(0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- CAMPO DE TEXTO ---
        if (uiState.selectedRestaurant == null) {
            OutlinedTextField(
                value = uiState.textInput,
                onValueChange = onTextChange,
                label = { Text("Digite seu pedido...", color = Color.White.copy(0.6f)) },
                placeholder = { Text(if (isListening) "Ouvindo..." else "Ex: Hambúrguer...", color = Color.Gray) },
                enabled = !uiState.isLoading,
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    IconButton(
                        onClick = onSendClick,
                        enabled = uiState.textInput.isNotBlank() && !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar",
                            tint = if (uiState.textInput.isNotBlank()) Color(0xFFBDBDBD) else Color.Gray
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFBDBDBD),
                    focusedBorderColor = Color(0xFFBDBDBD),
                    unfocusedBorderColor = Color(0xFF424242),
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun RestaurantCardItem(restaurant: Restaurant, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            val imageUrl = restaurant.image_url ?: "https://placehold.co/100x100.png"
            KamelImage(
                resource = asyncPainterResource(data = imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray),
                contentScale = ContentScale.Crop,
                onLoading = { CircularProgressIndicator(modifier = Modifier.padding(20.dp)) },
                onFailure = {
                    Box(Modifier.fillMaxSize().background(Color.Red))
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = restaurant.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                        Text(
                            text = (restaurant.rating ?: 5.0).toString(),
                            color = Color(0xFFFFB300),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = restaurant.category,
                    color = Color.White.copy(0.6f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (restaurant.products.isNotEmpty()) {
                    Text(
                        text = "Encontrei: ${restaurant.products.joinToString { it.name }}",
                        color = Color(0xFFBDBDBD),
                        fontSize = 12.sp,
                        maxLines = 2,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}