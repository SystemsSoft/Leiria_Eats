package org.leria.eats.project.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
private val RdSurpriseBox = KomaSurpriseBox

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
                    IconButton(
                        onClick = {
                            if (cartItems.isNotEmpty()) showBackDialog = true else onBack()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RdCard)
                                .border(1.dp, RdPrimary.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = RdText, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = restaurant.name,
                            fontSize = 24.sp,
                            color = RdText,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = restaurant.category,
                                fontSize = 14.sp,
                                color = RdMuted
                            )
                            Text(" · ", color = RdMuted, fontSize = 14.sp)
                            Icon(Icons.Default.Star, contentDescription = null, tint = KomaStarYellow, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${restaurant.rating}",
                                fontSize = 14.sp,
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

            // ── Lista de pratos (GRELHA 3 COLUNAS) ─────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                if (filteredProducts.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
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
                    CompactProductItem(
                        product = product,
                        quantity = qty,
                        onAdd = { onAdd(product) },
                        onRemove = { onRemove(product) }
                    )
                }
            }
        }

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
fun CompactProductItem(
    product: Product,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val hasItems = quantity > 0
    var showExpandedDetails by remember { mutableStateOf(false) }

    if (showExpandedDetails) {
        ExpandedProductDetailsModal(
            product = product,
            quantity = quantity,
            onDismiss = { showExpandedDetails = false },
            onAdd = onAdd,
            onRemove = onRemove
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RdCard)
            .border(
                width = 1.dp,
                color = if (hasItems) RdPrimary.copy(alpha = 0.5f) else RdPrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { showExpandedDetails = true },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                    Box(Modifier.fillMaxSize().background(RdSurface), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = RdPrimary)
                    }
                },
                onFailure = { Box(Modifier.fillMaxSize().background(RdSurface)) }
            )

            if (product.isSurpriseBox) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = RdSurpriseBox.copy(alpha = 0.92f)
                ) {
                    Text(
                        text = "🎁",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            if (hasItems) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(RdSecondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$quantity", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                color = RdText,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = formatCurrency(product.price),
                color = RdSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // "−" aparece com fade ao lado do "+" quando o produto já está na sacola, para poder
            // diminuir a quantidade até zerar. Largura fixa: o "+" ocupa o resto da linha, então sem
            // itens ele continua com a largura toda.
            Row(
                modifier = Modifier.fillMaxWidth().height(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = hasItems,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(RdSecondary.copy(alpha = 0.15f))
                                .clickable { onRemove() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Remover ${product.name}",
                                tint = RdSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (hasItems) RdSecondary.copy(alpha = 0.15f)
                            else RdPrimary.copy(alpha = 0.1f)
                        )
                        .clickable { onAdd() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Adicionar ${product.name}",
                        tint = if (hasItems) RdSecondary else RdPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

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
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RdCard)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, tint = RdText, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        Box(Modifier.fillMaxSize().background(RdSurface), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp, color = RdSecondary)
                        }
                    },
                    onFailure = { Box(Modifier.fillMaxSize().background(RdSurface)) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = RdText,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatCurrency(product.price),
                    fontWeight = FontWeight.ExtraBold,
                    color = RdSecondary,
                    fontSize = 20.sp
                )
            }

            if (product.isSurpriseBox) {
                Spacer(modifier = Modifier.height(10.dp))
                val start = product.surpriseBoxPickupStart
                val end = product.surpriseBoxPickupEnd
                Surface(shape = RoundedCornerShape(8.dp), color = RdSurpriseBox.copy(alpha = 0.12f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(text = "🎁 ", fontSize = 13.sp)
                        Text(
                            text = if (!start.isNullOrBlank() && !end.isNullOrBlank()) {
                                "Caixa Surpresa — recolha entre $start e $end"
                            } else {
                                "Caixa Surpresa — restaurante ainda não definiu o horário de recolha"
                            },
                            color = RdSurpriseBox,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = RdCard, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            if (product.description.isNotEmpty()) {
                Text(text = "Descrição", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RdPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = product.description, fontSize = 14.sp, color = RdText, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (product.preparationTime.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(RdCard).padding(12.dp)) {
                    Text("⏱️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Tempo de preparo", fontSize = 12.sp, color = RdMuted)
                        Text(text = product.preparationTime, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RdText)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(RdCard).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quantidade", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RdText)
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(RdAccent.copy(alpha = 0.1f)).border(1.dp, RdAccent.copy(alpha = 0.2f), RoundedCornerShape(20.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onRemove() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Remove, null, tint = RdAccent, modifier = Modifier.size(16.dp))
                    }
                    Text(text = "$quantity", color = RdText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center)
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(RdPrimary, RdSecondary))).clickable { onAdd() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
