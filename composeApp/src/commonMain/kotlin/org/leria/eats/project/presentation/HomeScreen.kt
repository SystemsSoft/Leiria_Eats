package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
    onCategorySelect: (String?) -> Unit,
    onClearSelection: () -> Unit,
    onAddToCart: (Product) -> Unit,
    onRemoveFromCart: (Product) -> Unit,
    onViewCart: () -> Unit,
    onClearSearch: () -> Unit
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF2C2C2C), Color(0xFF1E1E1E), Color(0xFF121212))
    )
    val goldColor = Color(0xFFFFD700)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- CABEÇALHO ---
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            KamelImage(
                resource = asyncPainterResource(data = "https://leiria-eats-repo.s3.us-east-2.amazonaws.com/logo-pato.png"),
                contentDescription = "Logo",
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = goldColor, fontWeight = FontWeight.Bold)) {
                        append("LEIRIA")
                    }
                    append(" ")
                    withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                        append("EATS")
                    }
                },
                style = MaterialTheme.typography.headlineMedium
            )
        }

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

        // --- MICROFONE (Oculto no Cardápio) ---
        if (uiState.selectedRestaurant == null) {
            CentralMicButton(
                status = permissionStatus,
                isRecording = isListening,
                onClick = onMicClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- CONTEÚDO DINÂMICO (Lista ou Cardápio) ---
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.selectedRestaurant != null) {
                RestaurantDetailScreen(
                    restaurant = uiState.selectedRestaurant!!,
                    cartItems = uiState.cartItems,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelect = onCategorySelect,
                    onBack = onClearSelection,
                    onAdd = onAddToCart,
                    onRemove = onRemoveFromCart,
                    onViewCart = onViewCart
                )
            } else {
                // EXIBE A LISTA DE RESTAURANTES EM GRID (iFood Style)
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFFBDBDBD),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (uiState.restaurants.isNotEmpty()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Sugestões encontradas:",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            Text(
                                "Limpar",
                                color = goldColor,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { onClearSearch() }
                            )
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.restaurants) { restaurant ->
                                RestaurantGridItem(
                                    restaurant = restaurant,
                                    onClick = { onRestaurantClick(restaurant) }
                                )
                            }
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
                            "Fale 'Pizza' ou 'Sushi'",
                            color = Color.Gray.copy(0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                onTextChange("ver todos")
                                onSendClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ver todos os restaurantes", color = Color.White)
                        }
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
fun RestaurantGridItem(restaurant: Restaurant, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageUrl = restaurant.image_url ?: "https://placehold.co/100x100.png"
        
        Card(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            KamelImage(
                resource = asyncPainterResource(data = imageUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) } },
                onFailure = { Box(Modifier.fillMaxSize().background(Color.DarkGray)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = restaurant.name,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Star, 
                contentDescription = null, 
                tint = Color(0xFFFFD700), 
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "${restaurant.rating ?: 5.0} • ${restaurant.category}",
                color = Color.Gray,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
