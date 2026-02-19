package org.leria.eats.project.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.permissions.PermissionStatus

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
    // Animação do Logo
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3500, easing = EaseInOutQuart)
        )
    }

    LaunchedEffect(Unit) {
        if (uiState.restaurants.isEmpty()) {
            onTextChange("ver todos")
            onSendClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 4.dp), // reduzida a padding vertical para aproximar o cabeçalho do topo
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            KamelImage(
                resource = asyncPainterResource(data = "https://leiria-eats-repo.s3.us-east-2.amazonaws.com/logo%3Dpato.png"),
                contentDescription = "Logo",
                modifier = Modifier
                    // menor para reduzir espaço abaixo do logo
                    .size(180.dp)
                    .offset(
                        x = (80 * (1 - animProgress.value)).dp,
                        // reduzido o deslocamento vertical da animação para evitar grande lacuna
                        y = ((-10) * (1 - animProgress.value)).dp
                    )
                    .graphicsLayer {
                        alpha = animProgress.value
                        scaleX = 0.5f + (0.5f * animProgress.value)
                        scaleY = 0.5f + (0.5f * animProgress.value)
                    },
                contentScale = ContentScale.Fit
            )
        }

        // Resposta da IA (Destaque)
        Text(
            text = if (uiState.selectedRestaurant == null) uiState.aiReply else "Cardápio de ${uiState.selectedRestaurant?.name}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                // remover espaçamento superior para ficar imediatamente abaixo do logo
                .padding(top = 0.dp)
                .fillMaxWidth()
        )
        
        // --- CONTEÚDO ---
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
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
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
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            Text(
                                "Limpar",
                                color = MaterialTheme.colorScheme.primary,
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
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- CAMPO DE TEXTO ---
        if (uiState.selectedRestaurant == null) {
            OutlinedTextField(
                value = uiState.textInput,
                onValueChange = onTextChange,
                label = { Text(if (isListening) "Ouvindo..." else "Peça com sua voz ou digite...", color = MaterialTheme.colorScheme.onSurface.copy(0.6f)) },
                placeholder = { Text(if (isListening) "Ouvindo..." else "Ex: Quero um hambúrguer com fritas", color = MaterialTheme.colorScheme.onSurface.copy(0.4f)) },
                enabled = !uiState.isLoading,
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onMicClick, enabled = !uiState.isLoading) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Gravar áudio",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (uiState.textInput.isNotBlank()) {
                            IconButton(
                                onClick = onSendClick,
                                enabled = !uiState.isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Enviar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
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
        Card(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            KamelImage(
                resource = asyncPainterResource(data = restaurant.image_url ?: ""),
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
            color = MaterialTheme.colorScheme.onBackground,
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
                tint = MaterialTheme.colorScheme.primary, 
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "${restaurant.rating ?: 5.0} • ${restaurant.category}",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}