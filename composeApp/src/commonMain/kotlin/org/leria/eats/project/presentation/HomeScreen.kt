package org.leria.eats.project.presentation

import androidx.compose.animation.*
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import org.leria.eats.project.presentation.util.StatusBarLightIcons
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
private val AiSurpriseBox = KomaSurpriseBox
private val AiTopBarGradient = Brush.verticalGradient(
    colors = listOf(KomaTopBarGreenStart, KomaTopBarGreenEnd)
)

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
    onClearSearch: () -> Unit
) {
    var isProductCategoryMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AiDeepBg)
    ) {
        // Mesma transição de "arrastar para o lado" usada entre as abas: abrir o
        // cardápio desliza da direita (avançar), voltar para a lista desliza da
        // esquerda — dá a sensação de navegação em pilha (push/pop).
        AnimatedContent(
            targetState = uiState.selectedRestaurant,
            transitionSpec = {
                if (targetState != null && initialState == null) {
                    (
                        slideInHorizontally(animationSpec = tween(350, easing = EaseInOut)) { it } +
                            fadeIn(animationSpec = tween(350))
                        ) togetherWith (
                        slideOutHorizontally(animationSpec = tween(350, easing = EaseInOut)) { -it / 3 } +
                            fadeOut(animationSpec = tween(250))
                        )
                } else {
                    (
                        slideInHorizontally(animationSpec = tween(350, easing = EaseInOut)) { -it } +
                            fadeIn(animationSpec = tween(350))
                        ) togetherWith (
                        slideOutHorizontally(animationSpec = tween(350, easing = EaseInOut)) { it / 3 } +
                            fadeOut(animationSpec = tween(250))
                        )
                }
            },
            label = "restaurant_detail_transition"
        ) { selectedRestaurant ->
        if (selectedRestaurant != null) {
            RestaurantDetailScreen(
                restaurant = selectedRestaurant,
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
            // Ícones claros na status bar enquanto a barra verde escura estiver visível
            // (só neste ramo — some ao abrir RestaurantDetailScreen, de fundo claro).
            StatusBarLightIcons(enabled = true)

            var showCategoriesSheet by remember { mutableStateOf(false) }
            var isProductCategoryMode by remember { mutableStateOf(false) }

            val restaurantCategories = remember(uiState.allRestaurants) {
                uiState.allRestaurants
                    .map { it.category }
                    .flatMap { it.split(",").map { s -> s.trim() } }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
            }
            
            val productCategories = remember(uiState.allRestaurants) {
                uiState.allRestaurants
                    .flatMap { it.products }
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
                        it.category.split(",").any { it.trim().equals(selected, ignoreCase = true) }
                    }
                }
            }
            
            val filteredProducts = remember(uiState.allRestaurants, uiState.selectedCategory) {
                val selected = uiState.selectedCategory
                val allProds = uiState.allRestaurants.flatMap { r -> r.products.map { p -> p to r } }
                if (selected == null) {
                    allProds
                } else {
                    allProds.filter { (p, _) ->
                        p.category.split(",").any { it.trim().equals(selected, ignoreCase = true) }
                    }
                }
            }

            Scaffold(
                containerColor = AiDeepBg,
                contentColor = AiText
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                            .background(AiTopBarGradient)
                    ) {
                        AiTopBar(
                            showClearButton = false,
                            onClearChat = onClearSearch
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 24.dp))
                    ) {
                        val listState = when {
                            uiState.isLoading && uiState.allRestaurants.isEmpty() -> 0
                            isProductCategoryMode -> 1
                            else -> 2
                        }
                        
                        Crossfade(
                            targetState = listState,
                            animationSpec = tween(durationMillis = 400),
                            label = "HomeListTransition"
                        ) { state ->
                            when (state) {
                                0 -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        AiThinkingIndicator()
                                    }
                                }
                                1 -> {
                                    HomeProductList(
                                        products = filteredProducts,
                                        categories = productCategories,
                                        selectedCategory = uiState.selectedCategory,
                                        isProductMode = true,
                                        onModeChange = {
                                            isProductCategoryMode = it
                                            onCategorySelect(null)
                                        },
                                        onCategorySelect = onCategorySelect,
                                        onViewAllCategoriesClick = { showCategoriesSheet = true },
                                        onProductClick = { product, restaurant ->
                                            val catToSelect = uiState.selectedCategory ?: product.category.split(",").firstOrNull()?.trim()
                                            onCategorySelect(catToSelect)
                                            onRestaurantClick(restaurant)
                                        }
                                    )
                                }
                                else -> {
                                    HomeRestaurantList(
                                        restaurants = filteredRestaurants,
                                        categories = restaurantCategories,
                                        selectedCategory = uiState.selectedCategory,
                                        isProductMode = false,
                                        onModeChange = {
                                            isProductCategoryMode = it
                                            onCategorySelect(null)
                                        },
                                        onCategorySelect = onCategorySelect,
                                        onViewAllCategoriesClick = { showCategoriesSheet = true },
                                        onRestaurantClick = onRestaurantClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            if (showCategoriesSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showCategoriesSheet = false },
                    containerColor = AiDeepBg,
                    contentColor = AiText,
                    dragHandle = {
                        BottomSheetDefaults.DragHandle(color = AiPrimary.copy(alpha = 0.3f))
                    },
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    ExpandableCategorySheet(
                        categories = if (isProductCategoryMode) productCategories else restaurantCategories,
                        selectedCategory = uiState.selectedCategory,
                        isProductMode = isProductCategoryMode,
                        onModeChange = { 
                            isProductCategoryMode = it
                            onCategorySelect(null) 
                        },
                        onCategorySelect = { 
                            onCategorySelect(it)
                            showCategoriesSheet = false
                        }
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ExpandableCategorySheet(
    categories: List<String>,
    selectedCategory: String?,
    isProductMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    onCategorySelect: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .background(AiPrimary, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Categorias",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AiText
                    )
                    Text(
                        text = "Escolha o que você quer encontrar",
                        fontSize = 12.sp,
                        color = AiTextMuted
                    )
                }
            }
            IconButton(onClick = { onCategorySelect(selectedCategory) }) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = AiText)
            }
        }
        
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AiCard.copy(alpha = 0.5f))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (!isProductMode) AiPrimary else Color.Transparent).clickable { onModeChange(false) }.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text(text = "Restaurantes", color = if (!isProductMode) Color.Black else AiTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isProductMode) AiPrimary else Color.Transparent).clickable { onModeChange(true) }.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text(text = "Produtos", color = if (isProductMode) Color.Black else AiTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { CategoryCard(category = "Tudo", isSelected = selectedCategory == null, onClick = { onCategorySelect(null) }) }
            items(categories) { category ->
                CategoryCard(category = category, isSelected = category.equals(selectedCategory, ignoreCase = true), onClick = { if (selectedCategory?.equals(category, ignoreCase = true) == true) onCategorySelect(null) else onCategorySelect(category) })
            }
        }
    }
}

@Composable
private fun CategoryCard(category: String, isSelected: Boolean, onClick: () -> Unit) {
    // Mapeamento básico de emojis para as categorias
    val icon = when (category.lowercase()) {
        "tudo" -> "🍲"
        "fast food" -> "🍔"
        "pizzaria" -> "🍕"
        "japonesa" -> "🍣"
        "mexicana" -> "🌮"
        "tailandesa" -> "🍜"
        "brasileira" -> "🍛"
        "saudável" -> "🥗"
        "sobremesas" -> "🍰"
        "bebidas" -> "🥤"
        "lanches" -> "🍟"
        "tacos" -> "🌮"
        else -> "🍽️"
    }

    Column(
        modifier = Modifier
            .width(80.dp)
            .aspectRatio(1f) // Força o formato quadrado
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) AiPrimary else Color.White) // Fundo amarelo se selecionado, branco se não
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category,
            color = if (isSelected) Color(0xFF1E293B) else Color(0xFF1E293B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiTopBar(showClearButton: Boolean, onClearChat: () -> Unit) {
    TopAppBar(navigationIcon = { Box(modifier = Modifier.padding(bottom = 14.dp).size(180.dp), contentAlignment = Alignment.CenterStart) { Image(painter = painterResource(Res.drawable.logo), contentDescription = "Koma", modifier = Modifier.fillMaxHeight(), contentScale = ContentScale.Fit) } }, title = {}, actions = { if (showClearButton) { TextButton(onClick = onClearChat, modifier = Modifier.padding(end = 8.dp)) { Text(text = "Limpar", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold) } } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = AiText))
}

@Composable
private fun AiThinkingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(AiBotBubble).border(1.dp, AiPrimary.copy(alpha = 0.2f), RoundedCornerShape(20.dp)).padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("A pensar", fontSize = 13.sp, color = AiTextMuted)
            (0..2).forEach { index ->
                val alpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1f, label = "dot$index", animationSpec = infiniteRepeatable(tween(600, delayMillis = index * 180), RepeatMode.Reverse))
                Box(modifier = Modifier.size(7.dp).background(AiPrimary.copy(alpha = alpha), CircleShape))
            }
        }
    }
}

private val SmartGold       = KomaSmartGold
private val SmartGoldDark   = KomaSmartGoldDark
private val SmartCardBg     = KomaSmartCardBg

@Composable
private fun CategoryHeaderWithToggle(
    categories: List<String>,
    selectedCategory: String?,
    isProductMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onViewAllCategoriesClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .background(AiPrimary, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Categorias",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AiText
                )
            }
            Text(
                text = "Ver todos >",
                fontSize = 13.sp,
                color = AiTextMuted,
                modifier = Modifier.clickable { onViewAllCategoriesClick() }
            )
        }
        
        Row(
            modifier = Modifier
                .padding(start = 4.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AiCard.copy(alpha = 0.5f))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (!isProductMode) AiPrimary else Color.Transparent).clickable { onModeChange(false) }.padding(horizontal = 10.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                Text(text = "Restaurantes", color = if (!isProductMode) Color.Black else AiTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isProductMode) AiPrimary else Color.Transparent).clickable { onModeChange(true) }.padding(horizontal = 10.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                Text(text = "Produtos", color = if (isProductMode) Color.Black else AiTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item { CategoryCard(category = "Tudo", isSelected = selectedCategory == null, onClick = { onCategorySelect(null) }) }
            items(categories) { category ->
                CategoryCard(category = category, isSelected = category.equals(selectedCategory, ignoreCase = true), onClick = { if (selectedCategory?.equals(category, ignoreCase = true) == true) onCategorySelect(null) else onCategorySelect(category) })
            }
        }
    }
}

@Composable
private fun HomeRestaurantList(
    restaurants: List<Restaurant>,
    categories: List<String>,
    selectedCategory: String?,
    isProductMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onViewAllCategoriesClick: () -> Unit,
    onRestaurantClick: (Restaurant) -> Unit
) {
    if (restaurants.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🍽️", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("A carregar restaurantes...", color = AiTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        return
    }

    val smartRestaurants = restaurants.filter { it.plan?.uppercase() == "SMART" }
    val otherRestaurants = restaurants.filter { it.plan?.uppercase() != "SMART" }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (smartRestaurants.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                SmartHighlightSection(smartRestaurants = smartRestaurants, onRestaurantClick = onRestaurantClick)
            }
            item(span = { GridItemSpan(3) }) { Spacer(modifier = Modifier.height(8.dp)) }
        }

        item(span = { GridItemSpan(3) }) {
            CategoryHeaderWithToggle(
                categories = categories,
                selectedCategory = selectedCategory,
                isProductMode = isProductMode,
                onModeChange = onModeChange,
                onCategorySelect = onCategorySelect,
                onViewAllCategoriesClick = onViewAllCategoriesClick
            )
        }
        
        item(span = { GridItemSpan(3) }) { Spacer(modifier = Modifier.height(8.dp)) }

        if (otherRestaurants.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(18.dp).background(AiPrimary, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Todos os restaurantes", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AiText)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("(${otherRestaurants.size})", fontSize = 11.sp, color = AiTextMuted)
                    }
                    Text(
                        text = "Ver todos >",
                        fontSize = 13.sp,
                        color = AiTextMuted,
                        modifier = Modifier.clickable { }
                    )
                }
            }
            items(otherRestaurants) { restaurant ->
                CompactRestaurantItem(restaurant = restaurant, onClick = { onRestaurantClick(restaurant) })
            }
        }
    }
}

@Composable
fun CompactRestaurantItem(restaurant: Restaurant, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AiCard).clickable(enabled = restaurant.isClosed != true) { onClick() }.then(if (restaurant.isClosed == true) Modifier.alpha(0.55f) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
            KamelImage(resource = asyncPainterResource(data = restaurant.image_url ?: ""), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, onLoading = { Box(Modifier.fillMaxSize().background(AiSurface), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AiPrimary) } }, onFailure = { Box(Modifier.fillMaxSize().background(AiSurface)) })
            if (restaurant.isClosed == true) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFB71C1C).copy(alpha = 0.9f)) {
                        Text("FECHADO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
        }
        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = restaurant.name, fontWeight = FontWeight.Bold, color = AiText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFB800), modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = "${restaurant.rating ?: 5.0}", color = AiText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SmartHighlightSection(smartRestaurants: List<Restaurant>, onRestaurantClick: (Restaurant) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)) {
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(listOf(SmartGold, SmartGoldDark))).padding(horizontal = 8.dp, vertical = 3.dp)) { Text(text = "⭐", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF5C3D00), letterSpacing = 1.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Destaques", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AiText)
            Spacer(modifier = Modifier.width(6.dp))
            Text("(${smartRestaurants.size})", fontSize = 11.sp, color = AiTextMuted)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(SmartGold.copy(alpha = 0.8f), Color.Transparent))))
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
            items(smartRestaurants) { restaurant ->
                SmartRestaurantCard(restaurant = restaurant, onClick = { onRestaurantClick(restaurant) })
            }
        }
    }
}

@Composable
private fun SmartRestaurantCard(restaurant: Restaurant, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "smartGlow")
    val glowAlpha by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 0.9f, label = "glow", animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    Column(modifier = Modifier.width(270.dp).clip(RoundedCornerShape(16.dp)).background(SmartCardBg).border(width = 1.5.dp, brush = Brush.linearGradient(listOf(SmartGold.copy(alpha = glowAlpha), SmartGoldDark.copy(alpha = glowAlpha))), shape = RoundedCornerShape(16.dp)).clickable(enabled = restaurant.isClosed != true) { onClick() }.then(if (restaurant.isClosed == true) Modifier.alpha(0.6f) else Modifier), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2.8f / 1f).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))) {
            KamelImage(resource = asyncPainterResource(data = restaurant.image_url ?: ""), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, onLoading = { Box(Modifier.fillMaxSize().background(AiCard), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = SmartGold) } }, onFailure = { Box(Modifier.fillMaxSize().background(AiCard)) })
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))))
            if (restaurant.isClosed == true) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFB71C1C).copy(alpha = 0.90f)) { Text(text = "🔒 FECHADO", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) } } }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = restaurant.name, fontWeight = FontWeight.Bold, color = AiText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = restaurant.category, color = AiTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Star, contentDescription = null, tint = SmartGold, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "${restaurant.rating ?: 5.0}", color = SmartGold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HomeProductList(
    products: List<Pair<Product, Restaurant>>,
    categories: List<String>,
    selectedCategory: String?,
    isProductMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onViewAllCategoriesClick: () -> Unit,
    onProductClick: (Product, Restaurant) -> Unit
) {
    if (products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🍔", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Nenhum produto encontrado...",
                    color = AiTextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(3) }) {
            CategoryHeaderWithToggle(
                categories = categories,
                selectedCategory = selectedCategory,
                isProductMode = isProductMode,
                onModeChange = onModeChange,
                onCategorySelect = onCategorySelect,
                onViewAllCategoriesClick = onViewAllCategoriesClick
            )
        }
        
        item(span = { GridItemSpan(3) }) { Spacer(modifier = Modifier.height(8.dp)) }

        items(products) { (product, restaurant) ->
            CompactHomeProductItem(
                product = product,
                restaurant = restaurant,
                onClick = { onProductClick(product, restaurant) }
            )
        }
    }
}

@Composable
private fun CompactHomeProductItem(
    product: Product,
    restaurant: Restaurant,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AiCard)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Imagem quadrada
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        ) {
            KamelImage(
                resource = asyncPainterResource(data = product.image_url ?: ""),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = {
                    Box(Modifier.fillMaxSize().background(AiSurface), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AiPrimary)
                    }
                },
                onFailure = { Box(Modifier.fillMaxSize().background(AiSurface)) }
            )

            if (product.isSurpriseBox) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = AiSurpriseBox.copy(alpha = 0.92f)
                ) {
                    Text(
                        text = "🎁",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                color = AiText,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = restaurant.name,
                color = AiTextMuted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatCurrency(product.price),
                color = AiSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RestaurantGridItem(restaurant: Restaurant, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AiCard).clickable(enabled = restaurant.isClosed != true) { onClick() }.then(if (restaurant.isClosed == true) Modifier.alpha(0.55f) else Modifier)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(3.2f / 1f).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))) {
            KamelImage(resource = asyncPainterResource(data = restaurant.image_url ?: ""), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, onLoading = { Box(Modifier.fillMaxSize().background(AiSurface), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = AiPrimary) } }, onFailure = { Box(Modifier.fillMaxSize().background(AiSurface)) })
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)))))
            if (restaurant.isClosed == true) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFB71C1C).copy(alpha = 0.92f)) { Text(text = "🔒 FECHADO", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) } } }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(text = restaurant.name, fontWeight = FontWeight.Bold, color = AiText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = restaurant.category, color = AiTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFB800), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "${restaurant.rating ?: 5.0}", color = AiText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
