package org.leria.eats.project.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import org.leria.eats.project.presentation.util.formatCurrency

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

    LaunchedEffect(permissionStatus) { /* no-op: keep compiler happy */ }

    LaunchedEffect(Unit) {
        if (uiState.restaurantResults.isEmpty() && uiState.productResults.isEmpty()) {
            onTextChange("ver todos")
            onSendClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 4.dp),
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
                    .size(180.dp)
                    .offset(
                        x = (80 * (1 - animProgress.value)).dp,
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

        Text(
            text = if (uiState.selectedRestaurant == null) uiState.aiReply else "Cardápio de ${uiState.selectedRestaurant.name}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 0.dp)
                .fillMaxWidth()
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.selectedRestaurant != null) {
                RestaurantDetailScreen(
                    restaurant = uiState.selectedRestaurant,
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
                } else if (uiState.restaurantResults.isNotEmpty() || uiState.productResults.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Sugestões encontradas:",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Ver todos",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    if (!uiState.isLoading) {
                                                        onTextChange("ver todos")
                                                        onSendClick()
                                                    }
                                                }
                                                .padding(end = 8.dp)
                                        )

                                        Text(
                                            "Limpar",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.clickable { onClearSearch() }
                                        )
                                    }
                                }
                            }

                            if (uiState.restaurantResults.isNotEmpty()) {
                                item {
                                    Text(
                                        "Restaurantes",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(
                                            start = 4.dp,
                                            top = 8.dp,
                                            bottom = 12.dp
                                        )
                                    )
                                }
                                item {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.heightIn(max = 1000.dp)
                                    ) {
                                        items(uiState.restaurantResults) { restaurant ->
                                            RestaurantGridItem(
                                                restaurant = restaurant,
                                                onClick = { onRestaurantClick(restaurant) }
                                            )
                                        }
                                    }
                                }
                            }

                            if (uiState.productResults.isNotEmpty()) {
                                item {
                                    Text(
                                        "Produtos",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(
                                            start = 4.dp,
                                            top = 16.dp,
                                            bottom = 12.dp
                                        )
                                    )
                                }
                                item {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.heightIn(max = 1000.dp)
                                    ) {
                                        items(uiState.productResults) { product ->
                                            ProductGridItem(
                                                product = product,
                                                onAddToCart = { onAddToCart(product) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (uiState.selectedRestaurant == null) {
            OutlinedTextField(
                value = uiState.textInput,
                onValueChange = onTextChange,
                label = { Text(if (isListening) "Ouvindo..." else "Peça com sua voz ou digite...", color = MaterialTheme.colorScheme.onSurface.copy(0.6f)) },
                placeholder = { Text(if (isListening) "Ouvindo..." else "Ex: Pizza", color = MaterialTheme.colorScheme.onSurface.copy(0.4f)) },
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
                                    imageVector = Icons.Filled.Send,
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
                    .background(MaterialTheme.colorScheme.surface)
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

@Composable
fun ProductGridItem(product: Product, onAddToCart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .aspectRatio(1f) // Changed from 1.5f to 1f to match RestaurantGridItem
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            KamelImage(
                resource = asyncPainterResource(data = product.image_url ?: ""),
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) } },
                onFailure = { Box(Modifier.fillMaxSize().background(Color.DarkGray)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = product.name,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp, // Changed from 14.sp
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = product.description,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) // Changed from onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(product.price),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp // Changed from 14.sp
                )
                IconButton(
                    onClick = onAddToCart,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "Adicionar ao carrinho",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
