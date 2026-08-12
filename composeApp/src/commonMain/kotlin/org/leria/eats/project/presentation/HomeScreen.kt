package org.leria.eats.project.presentation

import androidx.compose.animation.core.*
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
private val AiAccent    = KomaGoldDark
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AiDeepBg)
            .padding( bottom = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Image(
            painter = painterResource(Res.drawable.logo),
            contentDescription = "Koma",
            modifier = Modifier.size(180.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )
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
                .aspectRatio(16f / 9f)
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
                .padding(horizontal = 8.dp, vertical = 6.dp),
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
        // ── Imagem landscape 16:9 ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
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
                .padding(horizontal = 10.dp, vertical = 8.dp)
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

// ─── Expanded Product Modal ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedProductModal(
    product: Product,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AiDeepBg,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
            ) {
                // Fechar button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AiCard)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = AiText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Imagem produto (maior)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        KamelImage(
                            resource = asyncPainterResource(data = product.image_url ?: ""),
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onLoading = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(AiSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 2.dp,
                                        color = AiSecondary
                                    )
                                }
                            },
                            onFailure = { Box(Modifier.fillMaxSize().background(AiSurface)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Nome e preço
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AiText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (product.rating != null && product.rating != 0.0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val fullStars = product.rating.toInt()
                                    val hasHalf = (product.rating - fullStars) >= 0.5
                                    (1..5).forEach { star ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                            tint = if (star <= fullStars) Color(0xFFFFB800)
                                            else if (star == fullStars + 1 && hasHalf) Color(0xFFFFB800).copy(alpha = 0.5f)
                                            else Color(0xFFD1D5DB),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${(product.rating * 10).toInt() / 10.0}",
                                        fontSize = 13.sp,
                                        color = AiTextMuted
                                    )
                                }
                            }
                        }
                        Text(
                            text = formatCurrency(product.price),
                            fontWeight = FontWeight.Bold,
                            color = AiSecondary,
                            fontSize = 22.sp
                        )                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Separador
                item {
                    HorizontalDivider(color = AiCard, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Descrição completa
                if (product.description.isNotEmpty()) {
                    item {
                        Text(
                            text = "Descrição",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AiPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.description,
                            fontSize = 14.sp,
                            color = AiText,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Tempo de preparo
                if (product.preparationTime.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AiCard)
                                .padding(12.dp)
                        ) {
                            Text("⏱️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Tempo de preparo",
                                    fontSize = 12.sp,
                                    color = AiTextMuted
                                )
                                Text(
                                    text = product.preparationTime,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AiText
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Categoria
                if (product.category.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AiCard)
                                .padding(12.dp)
                        ) {
                            Text("🏷️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Categoria",
                                    fontSize = 12.sp,
                                    color = AiTextMuted
                                )
                                Text(
                                    text = product.category,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AiText
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
@Composable
fun ProductGridItem(product: Product, onAddToCart: () -> Unit) {
    var showExpandedView by remember { mutableStateOf(false) }

    if (showExpandedView) {
        ExpandedProductModal(
            product = product,
            onDismiss = { showExpandedView = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AiCard)
            .border(1.dp, AiSecondary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(bottom = 8.dp)
            .clickable { showExpandedView = true },
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
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))))
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
                               else Color(0xFFD1D5DB),
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
