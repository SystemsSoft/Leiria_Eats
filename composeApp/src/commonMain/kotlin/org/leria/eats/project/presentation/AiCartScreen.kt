package org.leria.eats.project.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.leria.eats.project.data.Address
import org.leria.eats.project.data.DeliveryFeeResponse
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.presentation.util.formatCurrency
import org.leria.eats.project.theme.*

// ─── Aliases locais → paleta central ─────────────────────────────────────────
private val CartDeepBg    = KomaBg
private val CartSurface   = KomaSurface
private val CartCard      = KomaCard
private val CartPrimary   = KomaGold
private val CartSecondary = KomaBrandGreen
private val CartAccent    = KomaGoldDark
private val CartText      = KomaTextPrimary
private val CartMuted     = KomaTextSec

@Composable
fun AiCartScreen(
    cartItems: List<Product>,
    cartRestaurants: List<Restaurant>,
    onRemoveItem: (Product) -> Unit,
    onAddItem: (Product) -> Unit = {},
    onCheckout: (Address, Double, Double, String, Map<String, Double>) -> Unit,
    userAddresses: List<Address> = emptyList(),
    onGetAddressFromMap: (Double, Double) -> String? = { _, _ -> null },
    onGoToRestaurant: ((Restaurant) -> Unit)? = null,
    cartAiMessage: String? = null,
    cartAiMessageSpoken: Boolean = false,
    onDismissAiMessage: () -> Unit = {},
    onSuggestAnotherRestaurant: () -> Unit = {},
    onAddMoreFromSame: () -> Unit = {},
    onMarkAiMessageAsSpoken: () -> Unit = {},
    onClearCart: () -> Unit = {},
    isMuted: Boolean = false,
    onGetDeliveryFee: (suspend (Double, Double, Double, Double, String) -> DeliveryFeeResponse?)? = null,
    onGoToHome: (() -> Unit)? = null
) {
    val total = cartItems.sumOf { it.price * it.quantity }
    var showServiceFeeSheet by remember { mutableStateOf(false) }
    var showUndoConfirmDialog by remember { mutableStateOf(false) }
    
    // Group items by restaurant_gid
    val groupedItems = remember(cartItems) {
        cartItems.groupBy { it.restaurant_gid }
    }

    if (showUndoConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUndoConfirmDialog = false },
            containerColor = CartCard,
            titleContentColor = CartText,
            textContentColor = CartMuted,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Desfazer pedido?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Se voltar agora, os itens adicionados pela IA serão removidos da sua sacola. Deseja continuar?")
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(KomaSoftRed)
                        .clickable {
                            showUndoConfirmDialog = false
                            onClearCart()
                        }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text("Sim, desfazer", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUndoConfirmDialog = false }) {
                    Text("Cancelar", color = CartMuted)
                }
            }
        )
    }

    if (showServiceFeeSheet) {
        AiServiceFeeBottomSheet(
            cartTotal = total,
            userAddresses = userAddresses,
            onGetAddressFromMap = onGetAddressFromMap,
            restaurants = cartRestaurants,
            onGetDeliveryFee = onGetDeliveryFee,
            onDismiss = { showServiceFeeSheet = false },
            onConfirm = { address, deliveryFee, serviceFee, deliveryType, feesMap ->
                showServiceFeeSheet = false
                onCheckout(address, deliveryFee, serviceFee, deliveryType, feesMap)
            }
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
                IconButton(
                    onClick = { showUndoConfirmDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(CartCard, CircleShape)
                        .border(1.dp, CartMuted.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = CartText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
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
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CartPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Minha Sacola IA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CartText)
                    Text(
                        "${cartItems.size} itens de ${groupedItems.size} locais",
                        fontSize = 12.sp, color = CartMuted
                    )
                }
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
                        onGoToRestaurant = { 
                            // Navega para o primeiro restaurante da lista se solicitado
                            cartRestaurants.firstOrNull()?.let { onGoToRestaurant?.invoke(it) }
                        },
                        isMuted = isMuted,
                        alreadySpoken = cartAiMessageSpoken,
                        onMarkAsSpoken = onMarkAiMessageAsSpoken,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
            }

            if (cartItems.isEmpty()) {
                // Empty state logic (similar to CartScreen)
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ShoppingBag, null, tint = CartMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sua sacola da IA está vazia", color = CartText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (onGoToHome != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onGoToHome, colors = ButtonDefaults.buttonColors(containerColor = CartPrimary)) {
                                Text("Voltar ao Início", color = Color.Black)
                            }
                        }
                    }
                }
            } else {
                // ── Item list grouped by restaurant ───────────────────────
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedItems.forEach { (restaurantGid, products) ->
                        val restaurant = cartRestaurants.find { it.gid == restaurantGid }
                        
                        // Header do Restaurante
                        item(key = "header_$restaurantGid") {
                            AiCartRestaurantHeader(restaurant, restaurantGid, products.sumOf { it.price * it.quantity })
                        }
                        
                        // Itens deste restaurante
                        items(products, key = { it.gid + "_" + it.name + "_" + restaurantGid }) { product ->
                            CartItemRow(
                                product = product,
                                onRemove = { onRemoveItem(product) },
                                onClick = { /* Detalhes não implementados na AiCartScreen por simplicidade */ }
                            )
                        }
                        
                        item { Spacer(modifier = Modifier.height(8.dp)) }
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
                            listOf(CartPrimary, KomaOrangeEnd)
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
                        contentDescription = "Finalizar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Iniciar check-out",
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
fun AiCartRestaurantHeader(restaurant: Restaurant?, restaurantGid: String?, subtotal: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CartCard.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (restaurant != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
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
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = restaurant.name,
                    color = CartText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = restaurant.category,
                    color = CartMuted,
                    fontSize = 11.sp
                )
            }
        } else {
            Icon(Icons.Default.Store, contentDescription = null, tint = CartMuted, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                if (restaurantGid != null) "Loja #$restaurantGid" else "Loja Desconhecida", 
                color = CartText, 
                fontWeight = FontWeight.ExtraBold, 
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text("Subtotal", color = CartMuted, fontSize = 10.sp)
            Text(formatCurrency(subtotal), color = CartPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ─── AI Service Fee Bottom Sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiServiceFeeBottomSheet(
    cartTotal: Double,
    userAddresses: List<Address>,
    onGetAddressFromMap: (Double, Double) -> String?,
    onDismiss: () -> Unit,
    onConfirm: (Address, Double, Double, String, Map<String, Double>) -> Unit,
    restaurants: List<Restaurant>,
    onGetDeliveryFee: (suspend (Double, Double, Double, Double, String) -> DeliveryFeeResponse?)? = null
) {
    val serviceFee = (cartTotal * 0.05).coerceIn(0.49, 1.99)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedAddress by remember { mutableStateOf(userAddresses.firstOrNull()) }
    var showAddressPicker by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }
    var selectedDeliveryType by remember { mutableStateOf("delivery") }
    val isPickup = selectedDeliveryType == "pickup"

    // Pending map coordinates waiting for geocoding
    var pendingMapCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isResolvingAddress by remember { mutableStateOf(false) }

    // Resolve address from coordinates
    LaunchedEffect(pendingMapCoords) {
        val coords = pendingMapCoords ?: return@LaunchedEffect
        isResolvingAddress = true
        val addressStr: String? = withContext(Dispatchers.Default) {
            onGetAddressFromMap(coords.first, coords.second)
        }
        if (addressStr != null) {
            selectedAddress = Address(
                name = "Localização personalizada",
                address = addressStr,
                latitude = coords.first,
                longitude = coords.second
            )
        }
        isResolvingAddress = false
        pendingMapCoords = null
    }

    // Delivery fees state: map of restaurant gid to its fee details
    var deliveryFeesMap by remember { mutableStateOf<Map<String, DeliveryFeeResponse>>(emptyMap()) }
    var feesLoading by remember { mutableStateOf(false) }
    var feesError by remember { mutableStateOf<String?>(null) }

    val totalDeliveryFee = if (isPickup) 0.0 else deliveryFeesMap.values.sumOf { it.delivery_fee }
    val grandTotal = cartTotal + serviceFee + totalDeliveryFee

    // Auto-select first address if none is selected
    LaunchedEffect(userAddresses) {
        if (selectedAddress == null && userAddresses.isNotEmpty()) {
            selectedAddress = userAddresses.firstOrNull()
        }
    }

    // Fetch all delivery fees whenever selected address changes
    LaunchedEffect(selectedAddress, isPickup) {
        val addr = selectedAddress
        if (addr?.latitude != null && addr.longitude != null && onGetDeliveryFee != null && !isPickup) {
            feesLoading = true
            feesError = null
            try {
                val results = restaurants.filter { it.latitude != null && it.longitude != null }.map { restaurant ->
                    async {
                        val feeRes = onGetDeliveryFee(
                            addr.latitude, addr.longitude,
                            restaurant.latitude!!, restaurant.longitude!!,
                            restaurant.gid
                        )
                        restaurant.gid to feeRes
                    }
                }.awaitAll()

                val newFees = results.mapNotNull { (gid, feeRes) -> 
                    feeRes?.let { gid to it } 
                }.toMap()

                deliveryFeesMap = newFees
                if (newFees.size < restaurants.size) {
                    feesError = "Alguns restaurantes não entregam nesta área."
                }
            } catch (e: Exception) {
                feesError = e.message ?: "Erro ao calcular taxas de entrega."
            }
            feesLoading = false
        }
    }

    if (showMapDialog) {
        MapDialog(
            onDismiss = { showMapDialog = false },
            onLocationSelected = { lat, long ->
                pendingMapCoords = Pair(lat, long)
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
                    .verticalScroll(rememberScrollState())
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
                    text = "Resumo dos pedidos IA",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CartText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Pedidos de múltiplos restaurantes serão processados individualmente.",
                    fontSize = 13.sp,
                    color = CartMuted,
                    textAlign = TextAlign.Center,
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
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Delivery address ──────────────────────
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

                    if (selectedAddress == null && !isResolvingAddress) {
                        // Se não houver endereço, mostramos um botão chamativo para selecionar
                        Button(
                            onClick = { showAddressPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CartPrimary.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CartPrimary.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.AddLocation, null, tint = CartPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Selecionar endereço", color = CartPrimary)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CartSurface)
                                .border(1.dp, CartPrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .clickable { if (!isResolvingAddress) showAddressPicker = true }
                                .padding(14.dp)
                        ) {
                            if (isResolvingAddress) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CartPrimary)
                            } else if (selectedAddress != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = CartPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(selectedAddress!!.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CartText)
                                        Text(selectedAddress!!.address, fontSize = 12.sp, color = CartMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Detailed Delivery Fees by Restaurant ─────────────────────
                if (!isPickup && restaurants.isNotEmpty()) {
                    Text(
                        text = "Taxas de entrega por loja",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CartMuted,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CartSurface)
                            .border(1.dp, CartPrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            restaurants.forEach { restaurant ->
                                val feeRes = deliveryFeesMap[restaurant.gid]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(restaurant.name, fontSize = 13.sp, color = CartText, modifier = Modifier.weight(1f))
                                    if (feesLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CartPrimary)
                                    } else if (feeRes != null) {
                                        Text(formatCurrency(feeRes.delivery_fee), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CartPrimary)
                                    } else {
                                        Text("—", fontSize = 13.sp, color = CartMuted)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Breakdown card ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CartSurface)
                        .border(1.dp, CartPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal produtos", fontSize = 14.sp, color = CartMuted)
                            Text(formatCurrency(cartTotal), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CartText)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Taxa de serviço", fontSize = 14.sp, color = CartMuted)
                            Text(formatCurrency(serviceFee), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CartPrimary)
                        }
                        if (!isPickup) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total entrega", fontSize = 14.sp, color = CartMuted)
                                Text(formatCurrency(totalDeliveryFee), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CartPrimary)
                            }
                        }
                        HorizontalDivider(color = CartPrimary.copy(alpha = 0.12f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CartText)
                            Text(formatCurrency(grandTotal), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CartSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Confirm button ────────────────────────────────────────────
                val canConfirm = selectedAddress != null && !feesLoading && (isPickup || deliveryFeesMap.size == restaurants.size)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canConfirm) Brush.horizontalGradient(listOf(CartPrimary, KomaOrangeEnd)) else SolidColor(CartMuted.copy(alpha = 0.3f)))
                        .then(if (canConfirm) Modifier.clickable { 
                            val finalFeesMap = if (isPickup) emptyMap() else deliveryFeesMap.mapValues { it.value.delivery_fee }
                            onConfirm(selectedAddress!!, totalDeliveryFee, serviceFee, selectedDeliveryType, finalFeesMap) 
                        } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Confirmar todos os pedidos", color = if (canConfirm) Color.White else CartText, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, CartMuted.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancelar", color = CartMuted)
                }
            }
        } else {
            // ── Address Picker Page (Simplificada) ───────────────────────────
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Escolher endereço", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CartText)
                Spacer(modifier = Modifier.height(16.dp))
                userAddresses.forEach { addr ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CartSurface)
                            .border(1.dp, CartPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable { selectedAddress = addr; showAddressPicker = false }
                            .padding(16.dp)
                    ) {
                        Text(addr.name, fontWeight = FontWeight.Bold, color = CartText)
                    }
                }
                Button(onClick = { showAddressPicker = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Voltar")
                }
            }
        }
    }
}
