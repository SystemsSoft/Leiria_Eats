package org.leria.eats.project.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

// ─── Paleta KOMAAI ────────────────────────────────────────────────────────────
private val AiDeepBg       = Color(0xFF061510)   // Deep forest black-green
private val AiSurface      = Color(0xFF0A2218)   // Dark teal surface
private val AiCard         = Color(0xFF0E2E20)   // Card teal
private val AiPrimary      = Color(0xFFFFC107)   // KOMAAI Gold
private val AiSecondary    = Color(0xFF4ADE80)   // Modern lime-green
private val AiAccent       = Color(0xFFFFD54F)   // Warm amber accent
private val AiText         = Color(0xFFF0FDF4)   // Near-white green tint
private val AiTextMuted    = Color(0xFF6EE7A0)   // Muted green
private val AiBotBubble    = Color(0xFF0D2419)   // Bot bubble dark teal

@OptIn(ExperimentalMaterial3Api::class)
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
    onClearSelectionAndCart: () -> Unit,
    onAddToCart: (Product) -> Unit,
    onRemoveFromCart: (Product) -> Unit,
    onViewCart: () -> Unit,
    onClearSearch: () -> Unit,
    onSearchTypeSelected: (showRestaurants: Boolean) -> Unit,
    onDismissSearchTypeSheet: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AiDeepBg)
    ) {
        if (uiState.selectedRestaurant != null) {
            // ── CARDÁPIO (tela de detalhe) ────────────────────────────────
            RestaurantDetailScreen(
                restaurant = uiState.selectedRestaurant,
                cartItems = uiState.cartItems,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = onCategorySelect,
                onBack = onClearSelection,
                onBackAndClearCart = onClearSelectionAndCart,
                onAdd = onAddToCart,
                onRemove = onRemoveFromCart,
                onViewCart = onViewCart
            )
        } else {
            // ── HOME: logo + lista completa de restaurantes ───────────────
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AiHeader()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(AiSurface)
                ) {
                    when {
                        uiState.isLoading && uiState.allRestaurants.isEmpty() ->
                            AiThinkingIndicator(modifier = Modifier.align(Alignment.Center))
                        else -> HomeRestaurantList(
                            restaurants = uiState.allRestaurants,
                            onRestaurantClick = onRestaurantClick
                        )
                    }
                }
            }
        }
    }
}

// ─── Header AI ────────────────────────────────────────────────────────────────
@Composable
private fun AiHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(AiSurface, AiDeepBg)))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            KamelImage(
                resource = asyncPainterResource("https://leiria-eats-repo.s3.us-east-2.amazonaws.com/logo%3Dpato.png"),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Fit,
                onFailure = {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AiPrimary, modifier = Modifier.size(24.dp))
                }
            )
        }
    }
}

// ─── Thinking indicator ───────────────────────────────────────────────────────
@Composable
private fun AiThinkingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AiBotBubble)
                .border(1.dp, AiPrimary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("A pensar", fontSize = 13.sp, color = AiTextMuted)
            (0..2).forEach { index ->
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f, targetValue = 1f, label = "dot$index",
                    animationSpec = infiniteRepeatable(
                        tween(600, delayMillis = index * 180),
                        RepeatMode.Reverse
                    )
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(AiPrimary.copy(alpha = alpha), CircleShape)
                )
            }
        }
    }
}

// ─── Home: lista completa de restaurantes ─────────────────────────────────────
@Composable
private fun HomeRestaurantList(
    restaurants: List<Restaurant>,
    onRestaurantClick: (Restaurant) -> Unit
) {
    if (restaurants.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🍽️", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "A carregar restaurantes...",
                    color = AiTextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            ) {
                Box(modifier = Modifier.width(3.dp).height(16.dp).background(AiPrimary, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Todos os restaurantes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AiText
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("(${restaurants.size})", fontSize = 11.sp, color = AiTextMuted)
            }
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 4000.dp)
            ) {
                items(restaurants) { restaurant ->
                    RestaurantGridItem(
                        restaurant = restaurant,
                        onClick = { onRestaurantClick(restaurant) }
                    )
                }
            }
        }
    }
}


// ─── Restaurant Grid Item ─────────────────────────────────────────────────────
@Composable
fun RestaurantGridItem(restaurant: Restaurant, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = restaurant.isClosed != true) { onClick() }
            .then(if (restaurant.isClosed == true) Modifier.alpha(0.6f) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, AiPrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
        ) {
            KamelImage(
                resource = asyncPainterResource(data = restaurant.image_url ?: ""),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { Box(Modifier.fillMaxSize().background(AiCard), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AiPrimary) } },
                onFailure = { Box(Modifier.fillMaxSize().background(AiCard)) }
            )
            // subtle gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, AiDeepBg.copy(alpha = 0.5f))))
            )
            // Badge "FECHADO" sobre a imagem
            if (restaurant.isClosed == true) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFB71C1C).copy(alpha = 0.90f)
                    ) {
                        Text(
                            text = "🔒 FECHADO",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = restaurant.name,
            fontWeight = FontWeight.SemiBold,
            color = AiText,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(9.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "${restaurant.rating ?: 5.0}",
                color = AiTextMuted,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

// ─── Product Grid Item ────────────────────────────────────────────────────────
@Composable
fun ProductGridItem(product: Product, onAddToCart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AiCard)
            .border(1.dp, AiSecondary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
        ) {
            KamelImage(
                resource = asyncPainterResource(data = product.image_url ?: ""),
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { Box(Modifier.fillMaxSize().background(AiSurface), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AiSecondary) } },
                onFailure = { Box(Modifier.fillMaxSize().background(AiSurface)) }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, AiDeepBg.copy(alpha = 0.5f))))
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
            Text(
                text = product.name,
                fontWeight = FontWeight.SemiBold,
                color = AiText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.description,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = AiTextMuted
            )
            if (product.rating != null && product.rating != 0.0) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val fullStars = product.rating.toInt()
                    val hasHalf = (product.rating - fullStars) >= 0.5
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (star <= fullStars) Color(0xFFFFB800)
                                   else if (star == fullStars + 1 && hasHalf) Color(0xFFFFB800).copy(alpha = 0.5f)
                                   else Color(0xFF444444),
                            modifier = Modifier.size(9.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${(product.rating * 10).toInt() / 10.0}",
                        fontSize = 8.sp,
                        color = AiTextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(product.price),
                    fontWeight = FontWeight.Bold,
                    color = AiSecondary,
                    fontSize = 11.sp
                )
                IconButton(
                    onClick = onAddToCart,
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            Brush.linearGradient(listOf(AiPrimary, AiSecondary)),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "Adicionar",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
