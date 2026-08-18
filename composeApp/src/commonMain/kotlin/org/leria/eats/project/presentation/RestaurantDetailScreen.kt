package org.leria.eats.project.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import org.leria.eats.project.presentation.util.formatCurrency
import org.leria.eats.project.theme.*

// ─── Aliases locais → paleta central ─────────────────────────────────────────
private val RdDeepBg    = KomaBg
private val RdSurface   = KomaSurface
private val RdCard      = KomaCard
private val RdPrimary   = KomaGold
private val RdSecondary = KomaBrandGreen
private val RdAccent    = KomaGoldDark
private val RdText      = KomaTextPrimary
private val RdMuted     = KomaTextSec

@Composable
fun RestaurantDetailScreen(
    restaurant: Restaurant,
    cartItems: List<Product>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    onBack: () -> Unit,
    onBackAndClearCart: () -> Unit,
    onAdd: (Product) -> Unit,
    onRemove: (Product) -> Unit,
    onViewCart: () -> Unit
) {
    val totalParams = cartItems.sumOf { it.price * it.quantity }
    val totalCount = cartItems.sumOf { it.quantity }
    var showBackDialog by remember { mutableStateOf(false) }

    // ── Back confirmation dialog ──────────────────────────────────────────────
    if (showBackDialog) {
        AlertDialog(
            onDismissRequest = { showBackDialog = false },
            containerColor = RdCard,
            titleContentColor = RdText,
            textContentColor = RdMuted,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RdAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deseja voltar?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("A sua sacola tem produtos de ${restaurant.name}. Se voltar, a sacola será esvaziada.\n\nTem a certeza que deseja voltar?")
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(RdAccent, KomaOrangeEnd)))
                        .clickable { showBackDialog = false; onBackAndClearCart() }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text("Sim, voltar", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackDialog = false }) {
                    Text("Cancelar", color = RdMuted)
                }
            }
        )
    }

    val categories = remember(restaurant.products) {
        listOf("Todos") + restaurant.products
            .flatMap { it.category.split(",").map { s -> s.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredProducts = remember(restaurant.products, selectedCategory) {
        if (selectedCategory == null || selectedCategory == "Todos") {
            restaurant.products
        } else {
            restaurant.products.filter { product ->
                product.category.split(",").any { it.trim().equals(selectedCategory, ignoreCase = true) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RdDeepBg)
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-40).dp, y = (-40).dp)
                .background(RdPrimary.copy(alpha = 0.06f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (totalCount > 0) 80.dp else 0.dp)
        ) {
            // ── Cabeçalho ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RdSurface)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RdCard)
                            .border(1.dp, RdPrimary.copy(alpha = 0.3f), CircleShape)
                            .clickable {
                                if (cartItems.isNotEmpty()) showBackDialog = true else onBack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = RdText, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = restaurant.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = RdText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = restaurant.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = RdMuted
                            )
                            Text(" · ", color = RdMuted, fontSize = 12.sp)
                            Icon(Icons.Default.Star, contentDescription = null, tint = KomaStarYellow, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${restaurant.rating}",
                                style = MaterialTheme.typography.bodySmall,
                                color = RdMuted
                            )
                        }
                    }
                }
            }

            // ── Filtro de categorias ───────────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(categories) { category ->
                    val isSelected = (selectedCategory?.equals(category, ignoreCase = true) == true) || (selectedCategory == null && category == "Todos")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected)
                                    Brush.horizontalGradient(listOf(RdPrimary, KomaOrangeEnd))
                                else
                                    Brush.horizontalGradient(listOf(RdCard, RdCard))
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color.Transparent else RdPrimary.copy(alpha = 0.2f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onCategorySelect(if (category == "Todos") null else category) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else RdMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // ── Lista de pratos ────────────────────────────────────────────────
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (filteredProducts.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
                        ) {
                            Box(modifier = Modifier.width(3.dp).height(16.dp).background(RdPrimary, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cardápio", color = RdText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(filteredProducts) { product ->
                    val qty = cartItems.find { it.gid == product.gid }?.quantity ?: 0
                    ProductItemWithCounter(
                        product = product,
                        quantity = qty,
                        onAdd = { onAdd(product) },
                        onRemove = { onRemove(product) }
                    )
                }
            }
        }

        // ── Cart FAB ──────────────────────────────────────────────────────────
        if (totalCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(listOf(RdPrimary, KomaOrangeEnd)))
                        .clickable { onViewCart() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$totalCount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Ver Sacola", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text(formatCurrency(totalParams), color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItemWithCounter(
    product: Product,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val hasItems = quantity > 0
    val borderColor by animateColorAsState(
        targetValue = if (hasItems) RdPrimary.copy(alpha = 0.45f) else RdPrimary.copy(alpha = 0.12f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "borderColor"
    )
    var showExpandedDetails by remember { mutableStateOf(false) }

    // ── Expanded Details Modal ─────────────────────────────────────────
    if (showExpandedDetails) {
        ExpandedProductDetailsModal(
            product = product,
            quantity = quantity,
            onDismiss = { showExpandedDetails = false },
            onAdd = onAdd,
            onRemove = onRemove
        )
    }


    // ── Card ──────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RdCard)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(12.dp)
            .clickable { showExpandedDetails = true }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!product.image_url.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RdSurface)
                ) {
                    KamelImage(
                        resource = asyncPainterResource(data = product.image_url),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onLoading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = RdPrimary) } },
                        onFailure = { Box(Modifier.fillMaxSize().background(RdSurface)) }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // INFO DO PRODUTO
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    color = RdText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.description,
                    color = RdMuted,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatCurrency(product.price),
                        color = RdSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    if (product.preparationTime.isNotBlank()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = RdMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = product.preparationTime,
                            color = RdMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // ── Seletor de quantidade moderno ─────────────────────────────
            AnimatedContent(
                targetState = hasItems,
                transitionSpec = {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                            scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)))
                        .togetherWith(
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                    scaleOut(targetScale = 0.85f)
                        )
                },
                label = "qtySelector"
            ) { showCounter ->
                if (showCounter) {
                    // ── Pill counter [−] n [+] ─────────────────────────
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(RdPrimary.copy(alpha = 0.14f), RdSecondary.copy(alpha = 0.10f))
                                )
                            )
                            .border(
                                1.dp,
                                Brush.horizontalGradient(listOf(RdPrimary.copy(alpha = 0.55f), RdSecondary.copy(alpha = 0.45f))),
                                RoundedCornerShape(50.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Minus button
                        val minusSource = remember { MutableInteractionSource() }
                        val minusPressed by minusSource.collectIsPressedAsState()
                        val minusScale by animateFloatAsState(
                            targetValue = if (minusPressed) 0.80f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "minusScale"
                        )
                        Box(
                            modifier = Modifier
                                .scale(minusScale)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(RdAccent.copy(alpha = 0.18f))
                                .border(1.dp, RdAccent.copy(alpha = 0.45f), CircleShape)
                                .clickable(
                                    interactionSource = minusSource,
                                    indication = null
                                ) { onRemove() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Remove, null, tint = RdAccent, modifier = Modifier.size(15.dp))
                        }

                        // Quantity label
                        AnimatedContent(
                            targetState = quantity,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (fadeIn() + scaleIn(initialScale = 0.7f)).togetherWith(fadeOut() + scaleOut(targetScale = 0.7f))
                                } else {
                                    (fadeIn() + scaleIn(initialScale = 1.3f)).togetherWith(fadeOut() + scaleOut(targetScale = 1.3f))
                                }
                            },
                            label = "qtyNum"
                        ) { qty ->
                            Text(
                                text = "$qty",
                                color = RdText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(min = 26.dp)
                            )
                        }

                        // Plus button
                        val plusSource = remember { MutableInteractionSource() }
                        val plusPressed by plusSource.collectIsPressedAsState()
                        val plusScale by animateFloatAsState(
                            targetValue = if (plusPressed) 0.80f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "plusScale"
                        )
                        Box(
                            modifier = Modifier
                                .scale(plusScale)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(RdPrimary, RdSecondary)))
                                .clickable(
                                    interactionSource = plusSource,
                                    indication = null
                                ) { onAdd() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = KomaGoldOnDark, modifier = Modifier.size(15.dp))
                        }
                    }
                } else {
                    // ── Botão "Adicionar" pill ─────────────────────────
                    val addSource = remember { MutableInteractionSource() }
                    val addPressed by addSource.collectIsPressedAsState()
                    val addScale by animateFloatAsState(
                        targetValue = if (addPressed) 0.93f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "addScale"
                    )
                    Box(
                        modifier = Modifier
                            .scale(addScale)
                            .clip(RoundedCornerShape(50.dp))
                            .background(Brush.linearGradient(listOf(RdPrimary, KomaLimeGreen)))
                            .clickable(
                                interactionSource = addSource,
                                indication = null
                            ) { onAdd() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = KomaGoldOnDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Adicionar",
                                color = KomaGoldOnDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Expanded Product Details Modal ────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedProductDetailsModal(
    product: Product,
    quantity: Int,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RdDeepBg,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            // Fechar button
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RdCard)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = RdText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Imagem produto (maior)
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
                                .background(RdSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.dp,
                                color = RdSecondary
                            )
                        }
                    },
                    onFailure = { Box(Modifier.fillMaxSize().background(RdSurface)) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nome e preço
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
                        color = RdText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatCurrency(product.price),
                    fontWeight = FontWeight.Bold,
                    color = RdSecondary,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Separador
            HorizontalDivider(color = RdCard, thickness = 1.dp)

            Spacer(modifier = Modifier.height(16.dp))

            // Descrição completa
            if (product.description.isNotEmpty()) {
                Text(
                    text = "Descrição",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RdPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = product.description,
                    fontSize = 14.sp,
                    color = RdText,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Tempo de preparo
            if (product.preparationTime.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RdCard)
                        .padding(12.dp)
                ) {
                    Text("⏱️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Tempo de preparo",
                            fontSize = 12.sp,
                            color = RdMuted
                        )
                        Text(
                            text = product.preparationTime,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RdText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Categoria
            if (product.category.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RdCard)
                        .padding(12.dp)
                ) {
                    Text("🏷️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Categoria",
                            fontSize = 12.sp,
                            color = RdMuted
                        )
                        Text(
                            text = product.category,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RdText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seletor de quantidade
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RdCard)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quantidade", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RdText)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(RdPrimary.copy(alpha = 0.14f), RdSecondary.copy(alpha = 0.10f))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(RdPrimary.copy(alpha = 0.55f), RdSecondary.copy(alpha = 0.45f))),
                            RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(RdAccent.copy(alpha = 0.18f))
                            .border(1.dp, RdAccent.copy(alpha = 0.45f), CircleShape)
                            .clickable { onRemove() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, null, tint = RdAccent, modifier = Modifier.size(15.dp))
                    }

                    Text(
                        text = "$quantity",
                        color = RdText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(RdPrimary, RdSecondary))
                            )
                            .clickable { onAdd() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
