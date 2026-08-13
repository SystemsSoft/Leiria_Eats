package org.leria.eats.project.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import komaai.composeapp.generated.resources.Res
import komaai.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.presentation.util.formatCurrency
import org.leria.eats.project.theme.*

// ─── Aliases locais → paleta central ─────────────────────────────────────────
private val AiDeepBg    = KomaBg
private val AiSurface   = KomaSurface
private val AiCard      = KomaCard
private val AiPrimary   = KomaGold
private val AiSecondary = KomaBrandGreen
private val AiText      = KomaTextPrimary
private val AiTextMuted = KomaTextSec
private val AiBotBubble = KomaMintLight

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
    // Pulsing glow animation
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.15f, targetValue = 0.55f, label = "glowAlpha",
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse)
    )

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
                AiTopBar(
                    glowAlpha = glowAlpha,
                    isListening = isListening,
                    showClearButton = false,
                    onClearChat = onClearSearch
                )

                val categories = remember(uiState.allRestaurants) {
                    uiState.allRestaurants
                        .map { it.category }
                        .flatMap { it.split(",").map { s -> s.trim() } }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                }

                val filteredRestaurants = remember(uiState.allRestaurants, uiState.selectedCategory) {
                    val selected = uiState.selectedCategory
                    if (selected == null) {
                        uiState.allRestaurants
                    } else {
                        uiState.allRestaurants.filter {
                            it.category.contains(selected, ignoreCase = true)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, end = 10.dp)
                        .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 24.dp))
                ) {
                    when {
                        uiState.isLoading && uiState.allRestaurants.isEmpty() ->
                            AiThinkingIndicator(modifier = Modifier.align(Alignment.Center))
                        else -> {
                            HomeRestaurantList(
                                restaurants = filteredRestaurants,
                                onRestaurantClick = onRestaurantClick
                            )
                        }
                    }
                }

                // ── CARROSSEL DE CATEGORIAS (Inferior) ──────────────────────
                if (categories.isNotEmpty()) {
                    CategoryCarousel(
                        categories = categories,
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelect = onCategorySelect
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCarousel(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Cabeçalho da seção similar ao de todos os restaurantes
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .background(AiPrimary, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Categorias",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AiText
            )
        }

        // Linha decorativa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(AiPrimary.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                CategoryCard(
                    category = "Tudo",
                    isSelected = selectedCategory == null,
                    onClick = { onCategorySelect(null) }
                )
            }
            
            items(categories) { category ->
                CategoryCard(
                    category = category,
                    isSelected = category == selectedCategory,
                    onClick = {
                        if (selectedCategory == category) onCategorySelect(null)
                        else onCategorySelect(category)
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) 
                    Brush.horizontalGradient(listOf(AiPrimary, KomaOrangeEnd))
                else 
                    Brush.linearGradient(listOf(AiCard, AiCard))
            )
            .border(
                width = 1.dp,
                brush = if (isSelected) 
                    Brush.horizontalGradient(listOf(AiPrimary, KomaOrangeEnd))
                else 
                    SolidColor(AiPrimary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category,
            color = if (isSelected) Color.Black else AiText,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// ─── AI Top Bar ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiTopBar(
    glowAlpha: Float,
    isListening: Boolean,
    showClearButton: Boolean,
    onClearChat: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(bottom = 14.dp)
                    .size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    AiPrimary.copy(alpha = if (isListening) glowAlpha * 0.7f else glowAlpha * 0.15f),
                                    Color.Transparent
                                ),
                                radius = 200f
                            )
                        )
                )
                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = "Koma",
                    modifier = Modifier.size(180.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }
        },
        title = {},
        actions = {
            if (showClearButton) {
                TextButton(
                    onClick = onClearChat,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "Limpar",
                        fontSize = 12.sp,
                        color = AiTextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AiSurface,
            titleContentColor = AiText
        )
    )
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

// ─── Aliases SMART → paleta central ──────────────────────────────────────────
private val SmartGold       = KomaSmartGold
private val SmartGoldDark   = KomaSmartGoldDark
private val SmartCardBg     = KomaSmartCardBg
private val SmartBorder     = KomaSmartGold

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

    val smartRestaurants = restaurants.filter { it.plan?.uppercase() == "SMART" }
    val otherRestaurants = restaurants.filter { it.plan?.uppercase() != "SMART" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Seção SMART em destaque ──────────────────────────────────────
        if (smartRestaurants.isNotEmpty()) {
            item {
                SmartHighlightSection(
                    smartRestaurants = smartRestaurants,
                    onRestaurantClick = onRestaurantClick
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // ── Todos os outros restaurantes ─────────────────────────────────
        if (otherRestaurants.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(16.dp)
                            .background(AiPrimary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Todos os restaurantes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AiText
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("(${otherRestaurants.size})", fontSize = 11.sp, color = AiTextMuted)
                }
            }
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 4000.dp)
                ) {
                    items(otherRestaurants) { restaurant ->
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

// ─── Seção de destaque SMART ──────────────────────────────────────────────────
@Composable
private fun SmartHighlightSection(
    smartRestaurants: List<Restaurant>,
    onRestaurantClick: (Restaurant) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Cabeçalho da seção
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(listOf(SmartGold, SmartGoldDark))
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "⭐",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF5C3D00),                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Destaques",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AiText
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("(${smartRestaurants.size})", fontSize = 11.sp, color = AiTextMuted)
        }

        // Linha dourada decorativa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(SmartGold.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Row horizontal com scroll
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(smartRestaurants) { restaurant ->
                SmartRestaurantCard(
                    restaurant = restaurant,
                    onClick = { onRestaurantClick(restaurant) }
                )
            }
        }
    }
}

// ─── Card de restaurante SMART ────────────────────────────────────────────────
@Composable
private fun SmartRestaurantCard(restaurant: Restaurant, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "smartGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        label = "glow",
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .width(270.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SmartCardBg)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(SmartGold.copy(alpha = glowAlpha), SmartGoldDark.copy(alpha = glowAlpha))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = restaurant.isClosed != true) { onClick() }
            .then(if (restaurant.isClosed == true) Modifier.alpha(0.6f) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.8f / 1f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            KamelImage(
                resource = asyncPainterResource(data = restaurant.image_url ?: ""),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = {
                    Box(
                        Modifier.fillMaxSize().background(AiCard),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = SmartGold
                        )
                    }
                },
                onFailure = { Box(Modifier.fillMaxSize().background(AiCard)) }
            )
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))
                    )
            )
            // Badge SMART no canto superior direito
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(listOf(SmartGold, SmartGoldDark))
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
            }
            // Badge FECHADO
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

        // Infos abaixo da imagem
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = restaurant.name,
                fontWeight = FontWeight.Bold,
                color = AiText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = restaurant.category,
                color = AiTextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = SmartGold,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${restaurant.rating ?: 5.0}",
                    color = SmartGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}



// ─── Restaurant Grid Item — padrão Glovo / Bolt Food ─────────────────────────
@Composable
fun RestaurantGridItem(restaurant: Restaurant, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AiCard)
            .clickable(enabled = restaurant.isClosed != true) { onClick() }
            .then(if (restaurant.isClosed == true) Modifier.alpha(0.55f) else Modifier)
    ) {
        // ── Imagem landscape
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3.2f / 1f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            KamelImage(
                resource = asyncPainterResource(data = restaurant.image_url ?: ""),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = {
                    Box(
                        Modifier.fillMaxSize().background(AiSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = AiPrimary
                        )
                    }
                },
                onFailure = { Box(Modifier.fillMaxSize().background(AiSurface)) }
            )
            // Gradient inferior suave
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                        )
                    )
            )
            // Badge FECHADO centralizado
            if (restaurant.isClosed == true) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFB71C1C).copy(alpha = 0.92f)
                    ) {
                        Text(
                            text = "🔒 FECHADO",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // ── Info abaixo da imagem ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = restaurant.name,
                fontWeight = FontWeight.Bold,
                color = AiText,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = restaurant.category,
                color = AiTextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB800),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${restaurant.rating ?: 5.0}",
                    color = AiText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
