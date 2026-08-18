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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onCheckout: (Address, Double, Double, String) -> Unit,
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
        // Multi-restaurant service fee: for simplicity we use the first restaurant to check delivery area
        // but the service fee is 5% of the TOTAL.
        val firstRestaurantGid = cartItems.firstOrNull()?.restaurant_gid
        val representativeRestaurant = cartRestaurants.find { it.gid == firstRestaurantGid }
        
        ServiceFeeBottomSheet(
            cartTotal = total,
            userAddresses = userAddresses,
            onGetAddressFromMap = onGetAddressFromMap,
            restaurant = representativeRestaurant,
            onGetDeliveryFee = onGetDeliveryFee,
            onDismiss = { showServiceFeeSheet = false },
            onConfirm = { address, deliveryFee, serviceFee, deliveryType ->
                showServiceFeeSheet = false
                onCheckout(address, deliveryFee, serviceFee, deliveryType)
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
                        items(products, key = { it.id.toString() + "_" + it.name + "_" + restaurantGid }) { product ->
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
            
            // ── Resumo Geral ─────────────────────────────────────────────
            if (cartItems.isNotEmpty()) {
                HorizontalDivider(color = CartPrimary.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Geral", color = CartText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(formatCurrency(total), color = CartSecondary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
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
