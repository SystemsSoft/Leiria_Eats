package org.leria.eats.project.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.StoreMallDirectory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.koinInject
import org.leria.eats.project.voice.TextToSpeechService
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.leria.eats.project.data.Address
import org.leria.eats.project.data.DeliveryFeeResponse
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.data.SavedPaymentMethod
import org.leria.eats.project.presentation.util.formatCurrency

// ─── Paleta KOMAAI (shared) ───────────────────────────────────────────────────
private val CartDeepBg    = Color(0xFF061510)   // Deep forest black-green
private val CartSurface   = Color(0xFF0A2218)   // Dark teal surface
private val CartCard      = Color(0xFF0E2E20)   // Card teal
private val CartPrimary   = Color(0xFFFFC107)   // KOMAAI Gold
private val CartSecondary = Color(0xFF4ADE80)   // Modern lime-green
private val CartAccent    = Color(0xFFFFD54F)   // Warm amber accent
private val CartText      = Color(0xFFF0FDF4)   // Near-white green tint
private val CartMuted     = Color(0xFF6EE7A0)   // Muted green

@Composable
fun CartScreen(
    cartItems: List<Product>,
    onRemoveItem: (Product) -> Unit,
    onAddItem: (Product) -> Unit = {},
    onCheckout: (Address, Double, Double, String) -> Unit,
    restaurantSelected: Restaurant?,
    userAddresses: List<Address> = emptyList(),
    onGetAddressFromMap: (Double, Double) -> String? = { _, _ -> null },
    onGoToRestaurant: ((Restaurant) -> Unit)? = null,
    cartAiMessage: String? = null,
    cartAiMessageSpoken: Boolean = false,
    onDismissAiMessage: () -> Unit = {},
    onSuggestAnotherRestaurant: () -> Unit = {},
    onAddMoreFromSame: () -> Unit = {},
    onMarkAiMessageAsSpoken: () -> Unit = {},
    isMuted: Boolean = false,
    onGetDeliveryFee: (suspend (Double, Double, Double, Double, Int) -> DeliveryFeeResponse?)? = null,
    onGoToHome: (() -> Unit)? = null
) {
    val total = cartItems.sumOf { it.price * it.quantity }
    var showServiceFeeSheet by remember { mutableStateOf(false) }
    var expandedProduct by remember { mutableStateOf<Product?>(null) }

    if (showServiceFeeSheet) {
        ServiceFeeBottomSheet(
            cartTotal = total,
            userAddresses = userAddresses,
            onGetAddressFromMap = onGetAddressFromMap,
            restaurant = restaurantSelected,
            onGetDeliveryFee = onGetDeliveryFee,
            onDismiss = { showServiceFeeSheet = false },
            onConfirm = { address, deliveryFee, serviceFee, deliveryType ->
                showServiceFeeSheet = false
                onCheckout(address, deliveryFee, serviceFee, deliveryType)
            }
        )
    }

    expandedProduct?.let { product ->
        val quantity = cartItems.find { it.id == product.id }?.quantity ?: 0
        CartExpandedProductDetailsModal(
            product = product,
            quantity = quantity,
            onDismiss = { expandedProduct = null },
            onAdd = { onAddItem(product) },
            onRemove = { onRemoveItem(product) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CartDeepBg)
    ) {
        // Ambient glow top
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .background(CartPrimary.copy(alpha = 0.07f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.radialGradient(listOf(CartPrimary.copy(alpha = 0.3f), Color.Transparent)),
                            CircleShape
                        )
                        .border(1.dp, CartPrimary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = CartPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Sacola", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CartText)
                    Text(
                        if (cartItems.isEmpty()) "Vazia" else "${cartItems.size} ${if (cartItems.size == 1) "item" else "itens"}",
                        fontSize = 12.sp, color = CartMuted
                    )
                }
            }

            // ── Restaurant header ─────────────────────────────────────────
            restaurantSelected?.let { restaurant ->
                RestaurantHeader(
                    restaurant = restaurant,
                    total = total,
                    modifier = Modifier.padding(bottom = 16.dp),
                    showGoToRestaurant = cartItems.isNotEmpty() && onGoToRestaurant != null,
                    onGoToRestaurant = { onGoToRestaurant?.invoke(restaurant) }
                )
            }

            // ── AI Chat Bubble ────────────────────────────────────────────
            AnimatedVisibility(
                visible = cartAiMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                cartAiMessage?.let { message ->
                    CartAiChatBubble(
                        message = message,
                        onDismiss = onDismissAiMessage,
                        onSuggestAnotherRestaurant = onSuggestAnotherRestaurant,
                        onGoToRestaurant = { restaurantSelected?.let { onGoToRestaurant?.invoke(it) } },
                        isMuted = isMuted,
                        alreadySpoken = cartAiMessageSpoken,
                        onMarkAsSpoken = onMarkAiMessageAsSpoken,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
            }

            if (cartItems.isEmpty()) {
                // ── Empty state ───────────────────────────────────────────
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(CartCard, CircleShape)
                                .border(1.dp, CartPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = CartMuted, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sua sacola está vazia", color = CartText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Pesquise um prato ou restaurante", color = CartMuted, fontSize = 13.sp)
                        if (onGoToHome != null) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onGoToHome,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CartPrimary,
                                    contentColor = Color(0xFF061510)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Voltar ao Início",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // ── Item list ─────────────────────────────────────────────
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartItems) { product ->
                        CartItemRow(
                            product = product,
                            onRemove = { onRemoveItem(product) },
                            onClick = { expandedProduct = product }
                        )
                    }
                }
            }
        }

        // ── Floating Action Button ───────────────────────────────────────
        if (cartItems.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(CartPrimary, Color(0xFFE65100))
                        )
                    )
                    .clickable { showServiceFeeSheet = true }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Finalizar Pedido",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ir para o check-out",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(product: Product, onRemove: () -> Unit, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CartCard)
            .border(1.dp, CartPrimary.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(12.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(62.dp)) {
                product.image_url?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CartSurface)
                    ) {
                        KamelImage(
                            resource = asyncPainterResource(it),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                if (product.quantity > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(CartPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${product.quantity}",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    color = CartText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    product.description,
                    color = CartMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.rating != null && product.rating != 0.0) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val fullStars = product.rating.toInt()
                        val hasHalf = (product.rating - fullStars) >= 0.5f
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (star <= fullStars) Color(0xFFFFB800)
                                       else if (star == fullStars + 1 && hasHalf) Color(0xFFFFB800).copy(alpha = 0.5f)
                                       else Color(0xFF444444),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${(product.rating * 10).toInt() / 10.0}",
                            fontSize = 9.sp,
                            color = CartMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatCurrency(product.price * product.quantity),
                        color = CartSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (product.quantity > 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "(${product.quantity} × ${formatCurrency(product.price)})",
                            color = CartMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CartAccent.copy(alpha = 0.12f))
                    .border(1.dp, CartAccent.copy(alpha = 0.3f), CircleShape)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = CartAccent, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartExpandedProductDetailsModal(
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
        containerColor = CartDeepBg,
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
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CartCard)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text("X", color = CartText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                        Box(
                            Modifier.fillMaxSize().background(CartSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.dp,
                                color = CartSecondary
                            )
                        }
                    },
                    onFailure = { Box(Modifier.fillMaxSize().background(CartSurface)) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                        color = CartText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatCurrency(product.price),
                    fontWeight = FontWeight.Bold,
                    color = CartSecondary,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = CartCard, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            if (product.description.isNotEmpty()) {
                Text(
                    text = "Descricao",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CartPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = product.description,
                    fontSize = 14.sp,
                    color = CartText,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (product.preparationTime.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CartCard)
                        .padding(12.dp)
                ) {
                    Text("Tempo", fontSize = 12.sp, color = CartMuted)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = product.preparationTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CartText
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CartCard)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quantidade", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CartText)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(CartPrimary.copy(alpha = 0.14f), CartSecondary.copy(alpha = 0.10f))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(CartPrimary.copy(alpha = 0.55f), CartSecondary.copy(alpha = 0.45f))),
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
                            .background(CartAccent.copy(alpha = 0.18f))
                            .border(1.dp, CartAccent.copy(alpha = 0.45f), CircleShape)
                            .clickable { onRemove() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, null, tint = CartAccent, modifier = Modifier.size(15.dp))
                    }

                    Text(
                        text = "$quantity",
                        color = CartText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.width(30.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(CartPrimary, CartSecondary)))
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

@Composable
fun RestaurantHeader(
    restaurant: Restaurant,
    total: Double,
    modifier: Modifier = Modifier,
    showGoToRestaurant: Boolean = false,
    onGoToRestaurant: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CartCard)
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(CartPrimary.copy(alpha = 0.4f), CartSecondary.copy(alpha = 0.3f))),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CartSurface)
            ) {
                restaurant.image_url?.let {
                    KamelImage(
                        resource = asyncPainterResource(it),
                        contentDescription = restaurant.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = restaurant.name,
                    color = CartText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = restaurant.category,
                    color = CartMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Total: ",
                        color = CartMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatCurrency(total),
                        color = CartSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            if (showGoToRestaurant) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CartPrimary.copy(alpha = 0.15f))
                        .border(1.dp, CartPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { onGoToRestaurant() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = CartPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cardápio", color = CartPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── AI Chat Bubble in Cart ───────────────────────────────────────────────────

/** Remove emojis and special symbols so TTS reads only plain text. */
private fun stripEmojisForTts(text: String): String =
    text
        .replace(Regex("[\\p{So}\\p{Sm}\\p{Sk}\\p{Sc}]"), "") // Unicode symbols
        .replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]"), "") // surrogate emoji pairs
        .replace(Regex("[\u2600-\u27FF]"), "")  // misc symbols, dingbats, arrows
        .replace(Regex("[\uFE00-\uFE0F]"), "")  // variation selectors
        .replace(Regex("\\s{2,}"), " ")
        .trim()

private val CartAiBotBubble = Color(0xFF0D2419)
private val CartAiPrimary   = Color(0xFFFFC107)
private val CartAiSecondary = Color(0xFF4ADE80)
private val CartAiText      = Color(0xFFF0FDF4)
private val CartAiMuted     = Color(0xFF6EE7A0)
private val CartAiCard2     = Color(0xFF0E2E20)

@Composable
fun CartAiChatBubble(
    message: String,
    onDismiss: () -> Unit,
    onSuggestAnotherRestaurant: () -> Unit,
    onGoToRestaurant: () -> Unit,
    isMuted: Boolean = false,
    alreadySpoken: Boolean = false,
    onMarkAsSpoken: () -> Unit = {},
    modifier: Modifier = Modifier,
    tts: TextToSpeechService = koinInject()
) {
    val displayedText = remember { mutableStateOf("") }
    var showClearCartDialog by remember { mutableStateOf(false) }

    // ── Diálogo de confirmação de limpar sacola ───────────────────────────────
    if (showClearCartDialog) {
        AlertDialog(
            onDismissRequest = { showClearCartDialog = false },
            containerColor = CartCard,
            titleContentColor = CartText,
            textContentColor = CartMuted,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🍽️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trocar de restaurante?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("A sua sacola será esvaziada ao escolher outro restaurante.\n\nTem a certeza que deseja continuar?")
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(listOf(CartPrimary, Color(0xFFE65100)))
                        )
                        .clickable {
                            showClearCartDialog = false
                            onSuggestAnotherRestaurant()
                        }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text("Sim, continuar", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCartDialog = false }) {
                    Text("Cancelar", color = CartMuted)
                }
            }
        )
    }

    // Stop TTS when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
        }
    }

    LaunchedEffect(message) {
        displayedText.value = ""
        tts.stop()

        // Start speaking immediately, in parallel with the typewriter animation
        // Only speak if not muted AND message hasn't been spoken before
        if (!isMuted && !alreadySpoken) {
            tts.speak(stripEmojisForTts(message))
            onMarkAsSpoken() // Mark as spoken after TTS starts
        }

        // Type main message
        for (i in message.indices) {
            displayedText.value = message.substring(0, i + 1)
            kotlinx.coroutines.delay(14)
        }
    }

    // React to mute toggled while message is already showing
    LaunchedEffect(isMuted) {
        if (isMuted) {
            tts.stop()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
            .background(
                Brush.horizontalGradient(listOf(CartAiBotBubble, CartAiCard2))
            )
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(CartAiPrimary.copy(alpha = 0.35f), CartAiSecondary.copy(alpha = 0.25f))),
                RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            // Top row: AI icon + label + dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(CartAiPrimary.copy(alpha = 0.35f), Color.Transparent)
                                ),
                                CircleShape
                            )
                            .border(1.dp, CartAiPrimary.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CartAiPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KOMAAI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CartAiPrimary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = CartAiMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Typewriter message
            Text(
                text = displayedText.value,
                fontSize = 13.sp,
                color = CartAiText,
                lineHeight = 19.sp
            )


            Spacer(modifier = Modifier.height(14.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Suggest another restaurant
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(CartAiPrimary.copy(alpha = 0.18f), CartAiPrimary.copy(alpha = 0.08f))
                            )
                        )
                        .border(1.dp, CartAiPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .clickable {
                            showClearCartDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🍽️ Outro restaurante",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CartAiPrimary
                    )
                }

                // Add more from same restaurant
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(CartAiSecondary.copy(alpha = 0.18f), CartAiSecondary.copy(alpha = 0.08f))
                            )
                        )
                        .border(1.dp, CartAiSecondary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .clickable {
                            onGoToRestaurant()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "📋 Cardápio",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CartAiSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePaymentMethodSheet(
    onDismiss: () -> Unit,
    onConfirm: (savePaymentMethod: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CartCard,
        contentColor = CartText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(CartPrimary.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, CartPrimary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💳",
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = "Salvar cartão como padrão?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CartText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = "Deseja usar esse cartão como padrão para usarmos sempre que você fizer um pedido?",
                fontSize = 14.sp,
                color = CartMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // No button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CartSurface)
                        .border(1.dp, CartMuted.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { onConfirm(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Não, obrigado",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CartMuted
                    )
                }

                // Yes button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(CartPrimary, Color(0xFFE65100))
                            )
                        )
                        .clickable { onConfirm(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sim, salvar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─── Confirm Payment with Saved Card Sheet ────────────────────────────────────

private fun cardBrandEmoji(brand: String): String = when (brand.lowercase()) {
    "visa" -> "💳"
    "mastercard" -> "💳"
    "amex" -> "💳"
    "discover" -> "💳"
    else -> "💳"
}

private fun cardBrandLabel(brand: String): String =
    brand.replaceFirstChar { it.uppercaseChar() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentConfirmBottomSheet(
    savedCards: List<SavedPaymentMethod>,
    onDismiss: () -> Unit,
    onUseSavedCard: (SavedPaymentMethod) -> Unit,
    onUseOtherMethod: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Default: first card (primary saved card)
    val primaryCard = savedCards.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CartCard,
        contentColor = CartText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Icon ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(CartPrimary.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, CartPrimary.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = CartPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Title ───────────────────────────────────────────────────
            Text(
                text = "Confirmar pagamento",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CartText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Deseja pagar com o cartão guardado?",
                fontSize = 14.sp,
                color = CartMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Card details ─────────────────────────────────────────────
            if (primaryCard != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0E2E20), Color(0xFF0A2218))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(CartPrimary.copy(alpha = 0.5f), CartSecondary.copy(alpha = 0.3f))
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Card brand icon area
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CartPrimary.copy(alpha = 0.12f))
                                .border(1.dp, CartPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cardBrandEmoji(primaryCard.brand),
                                fontSize = 24.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cardBrandLabel(primaryCard.brand),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CartText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "•••• •••• •••• ${primaryCard.last4}",
                                fontSize = 14.sp,
                                color = CartMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Val.: ${primaryCard.expMonth.toString().padStart(2, '0')}/${primaryCard.expYear}",
                                fontSize = 11.sp,
                                color = CartMuted.copy(alpha = 0.7f)
                            )
                        }

                        // Active badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CartSecondary.copy(alpha = 0.15f))
                                .border(1.dp, CartSecondary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Ativo",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CartSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Confirm button ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(listOf(CartPrimary, Color(0xFFE65100)))
                        )
                        .clickable { onUseSavedCard(primaryCard) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pagar com ${cardBrandLabel(primaryCard.brand)} ••••${primaryCard.last4}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Other method button ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CartSurface)
                        .border(1.dp, CartMuted.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { onUseOtherMethod() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Usar outro método de pagamento",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CartMuted
                    )
                }
            }
        }
    }
}

// ─── Service Fee Bottom Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceFeeBottomSheet(
    cartTotal: Double,
    userAddresses: List<Address>,
    onGetAddressFromMap: (Double, Double) -> String?,
    onDismiss: () -> Unit,
    onConfirm: (Address, Double, Double, String) -> Unit,
    restaurant: Restaurant? = null,
    onGetDeliveryFee: (suspend (Double, Double, Double, Double, Int) -> DeliveryFeeResponse?)? = null
) {
    val serviceFee = (cartTotal * 0.05).coerceIn(0.49, 1.99)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedAddress by remember { mutableStateOf(userAddresses.firstOrNull()) }
    var showAddressPicker by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }
    var selectedDeliveryType by remember { mutableStateOf("delivery") }
    val isPickup = selectedDeliveryType == "pickup"

    // Delivery fee state
    var deliveryFee by remember { mutableStateOf<Double?>(null) }
    var deliveryFeeLoading by remember { mutableStateOf(false) }
    var deliveryFeeError by remember { mutableStateOf<String?>(null) }
    var deliveryDistanceKm by remember { mutableStateOf<Double?>(null) }

    val effectiveDeliveryFee = if (isPickup) 0.0 else (deliveryFee ?: 0.0)
    val grandTotal = cartTotal + serviceFee + effectiveDeliveryFee

    // Fetch delivery fee whenever the selected address changes
    val canCalculateFee = restaurant?.latitude != null && restaurant.longitude != null
    LaunchedEffect(selectedAddress) {
        val addr = selectedAddress
        if (canCalculateFee && addr?.latitude != null && addr.longitude != null && onGetDeliveryFee != null) {
            deliveryFeeLoading = true
            deliveryFeeError = null
            deliveryFee = null
            deliveryDistanceKm = null
            try {
                val result = onGetDeliveryFee(
                    addr.latitude, addr.longitude,
                    restaurant!!.latitude!!, restaurant.longitude!!,
                    restaurant.id
                )
                if (result != null) {
                    deliveryFee = result.delivery_fee
                    deliveryDistanceKm = result.distance_km
                } else {
                    deliveryFeeError = "Não foi possível calcular a taxa de entrega."
                }
            } catch (e: Exception) {
                deliveryFeeError = e.message ?: "Endereço fora da área de entrega."
            }
            deliveryFeeLoading = false
        }
    }

    if (showMapDialog) {
        MapDialog(
            onDismiss = { showMapDialog = false },
            onLocationSelected = { lat, long ->
                val addressStr = onGetAddressFromMap(lat, long)
                if (addressStr != null) {
                    selectedAddress = Address(
                        name = "Localização personalizada",
                        address = addressStr,
                        latitude = lat,
                        longitude = long
                    )
                }
                showMapDialog = false
                showAddressPicker = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CartCard,
        contentColor = CartText
    ) {
        if (!showAddressPicker) {
            // ── Summary Page ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(CartPrimary.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, CartPrimary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🧾", fontSize = 30.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Resumo do pedido",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CartText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Uma taxa de serviço é aplicada para suportar a plataforma.",
                    fontSize = 13.sp,
                    color = CartMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Delivery type selector ────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Entrega
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (!isPickup)
                                    Brush.verticalGradient(listOf(CartPrimary.copy(alpha = 0.22f), CartPrimary.copy(alpha = 0.08f)))
                                else
                                    Brush.verticalGradient(listOf(CartSurface, CartSurface))
                            )
                            .border(
                                1.dp,
                                if (!isPickup) CartPrimary.copy(alpha = 0.65f) else CartMuted.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDeliveryType = "delivery" }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                contentDescription = null,
                                tint = if (!isPickup) CartPrimary else CartMuted,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                "Entrega",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isPickup) CartPrimary else CartMuted
                            )
                            Text("Receba em casa", fontSize = 10.sp, color = CartMuted)
                        }
                    }
                    // Recolha
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isPickup)
                                    Brush.verticalGradient(listOf(CartSecondary.copy(alpha = 0.22f), CartSecondary.copy(alpha = 0.08f)))
                                else
                                    Brush.verticalGradient(listOf(CartSurface, CartSurface))
                            )
                            .border(
                                1.dp,
                                if (isPickup) CartSecondary.copy(alpha = 0.65f) else CartMuted.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDeliveryType = "pickup" }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.StoreMallDirectory,
                                contentDescription = null,
                                tint = if (isPickup) CartSecondary else CartMuted,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                "Recolha",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPickup) CartSecondary else CartMuted
                            )
                            Text("Levante no restaurante", fontSize = 10.sp, color = CartMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Delivery address (only for delivery) ──────────────────────
                if (!isPickup) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Endereço de entrega",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CartMuted
                    )
                    Text(
                        text = "Alterar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CartPrimary,
                        modifier = Modifier.clickable { showAddressPicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CartSurface)
                        .border(
                            1.dp,
                            if (selectedAddress != null) CartPrimary.copy(alpha = 0.3f)
                            else Color(0xFFF87171).copy(alpha = 0.4f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { showAddressPicker = true }
                        .padding(14.dp)
                ) {
                    if (selectedAddress != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CartPrimary.copy(alpha = 0.12f), CircleShape)
                                    .border(1.dp, CartPrimary.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = CartPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    selectedAddress!!.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CartText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    selectedAddress!!.address,
                                    fontSize = 12.sp,
                                    color = CartMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📍", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Selecione um endereço de entrega",
                                fontSize = 13.sp,
                                color = CartMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                } else {
                    // ── Pickup info card ──────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CartSurface)
                            .border(1.dp, CartSecondary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CartSecondary.copy(alpha = 0.12f), CircleShape)
                                    .border(1.dp, CartSecondary.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.StoreMallDirectory, null, tint = CartSecondary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Recolha no restaurante", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CartText)
                                Text("Levante o seu pedido no balcão", fontSize = 12.sp, color = CartMuted)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Breakdown card ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CartSurface)
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(CartPrimary.copy(alpha = 0.3f), CartSecondary.copy(alpha = 0.2f))
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Subtotal", fontSize = 14.sp, color = CartMuted)
                            Text(
                                text = formatCurrency(cartTotal),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CartText
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CartPrimary.copy(alpha = 0.12f)))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Taxa de serviço", fontSize = 14.sp, color = CartMuted)
                            Text(
                                text = formatCurrency(serviceFee),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CartPrimary
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CartPrimary.copy(alpha = 0.12f)))
                        // ── Delivery fee row ──────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Taxa de entrega", fontSize = 14.sp, color = CartMuted)
                                if (!isPickup && deliveryDistanceKm != null) {
                                    val km = deliveryDistanceKm!!
                                    val rounded = (km * 100).toInt()
                                    val intPart = rounded / 100
                                    val decPart = rounded % 100
                                    val decStr = if (decPart < 10) "0$decPart" else "$decPart"
                                    Text(text = "$intPart.$decStr km", fontSize = 11.sp, color = CartMuted.copy(alpha = 0.7f))
                                }
                            }
                            when {
                                isPickup -> Text("Grátis", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CartSecondary)
                                deliveryFeeLoading -> CircularProgressIndicator(color = CartPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                deliveryFeeError != null -> Text("⚠ Fora da área", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF87171))
                                deliveryFee != null -> Text(formatCurrency(deliveryFee!!), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CartPrimary)
                                else -> Text("—", fontSize = 14.sp, color = CartMuted)
                            }
                        }
                        if (!isPickup && deliveryFeeError != null) {
                            Text(text = deliveryFeeError!!, fontSize = 11.sp, color = Color(0xFFF87171).copy(alpha = 0.85f), lineHeight = 15.sp)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CartPrimary.copy(alpha = 0.12f)))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CartText)
                            Text(
                                text = formatCurrency(grandTotal),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CartSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Confirm button ────────────────────────────────────────────
                val canConfirm = if (isPickup) {
                    true
                } else {
                    selectedAddress != null
                        && !deliveryFeeLoading
                        && deliveryFeeError == null
                        && (deliveryFee != null || !canCalculateFee || selectedAddress?.latitude == null)
                }
                val confirmAddress = if (isPickup)
                    Address(name = "Recolha", address = "Recolha no restaurante")
                else
                    selectedAddress!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (canConfirm)
                                Brush.horizontalGradient(listOf(CartPrimary, Color(0xFFE65100)))
                            else
                                Brush.horizontalGradient(listOf(CartMuted.copy(alpha = 0.25f), CartMuted.copy(alpha = 0.15f)))
                        )
                        .then(
                            if (canConfirm) Modifier.clickable { onConfirm(confirmAddress, effectiveDeliveryFee, serviceFee, selectedDeliveryType) }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (canConfirm) Color.White else CartMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Confirmar pedido · ${formatCurrency(grandTotal)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canConfirm) Color.White else CartMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Cancel button ─────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CartSurface)
                        .border(1.dp, CartMuted.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancelar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CartMuted
                    )
                }
            }
        } else {
            // ── Address Picker Page ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp, top = 8.dp)
            ) {
                // Header with back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CartSurface)
                            .border(1.dp, CartMuted.copy(alpha = 0.3f), CircleShape)
                            .clickable { showAddressPicker = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = CartMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Endereço de entrega",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CartText
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (userAddresses.isNotEmpty()) {
                    Text(
                        text = "Endereços guardados",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CartMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    userAddresses.forEach { address ->
                        val isSelected = selectedAddress?.name == address.name &&
                                selectedAddress?.address == address.address
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) CartPrimary.copy(alpha = 0.1f) else CartSurface
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) CartPrimary.copy(alpha = 0.5f)
                                    else CartPrimary.copy(alpha = 0.1f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    selectedAddress = address
                                    showAddressPicker = false
                                }
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isSelected) CartPrimary.copy(alpha = 0.18f) else CartCard,
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) CartPrimary.copy(alpha = 0.5f)
                                            else CartMuted.copy(alpha = 0.2f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = if (isSelected) CartPrimary else CartMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        address.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CartPrimary else CartText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        address.address,
                                        fontSize = 12.sp,
                                        color = CartMuted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selecionado",
                                        tint = CartPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CartSurface)
                            .border(1.dp, CartPrimary.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Nenhum endereço guardado. Adicione um no Perfil ou utilize o mapa.",
                            color = CartMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Map option ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CartSecondary.copy(alpha = 0.07f))
                        .border(1.dp, CartSecondary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .clickable { showMapDialog = true }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(CartSecondary.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, CartSecondary.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = CartSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Personalizar no mapa",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CartSecondary
                            )
                            Text(
                                "Escolha um local novo no mapa",
                                fontSize = 12.sp,
                                color = CartMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Delivery Type Bottom Sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryTypeBottomSheet(
    onDismiss: () -> Unit,
    onDeliveryTypeSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CartCard,
        contentColor = CartText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(CartPrimary.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, CartPrimary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🛵", fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Como deseja receber?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CartText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Escolha se pretende entrega em casa ou recolha no restaurante.",
                fontSize = 14.sp,
                color = CartMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Entrega (Delivery) button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(CartPrimary.copy(alpha = 0.18f), CartPrimary.copy(alpha = 0.07f))
                            )
                        )
                        .border(1.dp, CartPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onDeliveryTypeSelected("delivery") }
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                            contentDescription = "Entrega",
                            tint = CartPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Entrega",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CartPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Receba em casa",
                            fontSize = 11.sp,
                            color = CartMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Recolha (Pickup) button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(CartSecondary.copy(alpha = 0.18f), CartSecondary.copy(alpha = 0.07f))
                            )
                        )
                        .border(1.dp, CartSecondary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onDeliveryTypeSelected("pickup") }
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.StoreMallDirectory,
                            contentDescription = "Recolha",
                            tint = CartSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recolha",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CartSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Levante no restaurante",
                            fontSize = 11.sp,
                            color = CartMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
