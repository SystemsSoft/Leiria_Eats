package org.leria.eats.project.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
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
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
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
    onCheckout: () -> Unit,
    isLoading: Boolean = false,
    restaurantSelected: Restaurant?,
    onGoToRestaurant: ((Restaurant) -> Unit)? = null,
    cartAiMessage: String? = null,
    onDismissAiMessage: () -> Unit = {},
    onSuggestAnotherRestaurant: () -> Unit = {},
    onAddMoreFromSame: () -> Unit = {}
) {
    val total = cartItems.sumOf { it.price * it.quantity }
    var showConfirmDialog by remember { mutableStateOf(false) }

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
                    Text("A minha sacola", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CartText)
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
                        onAddMoreFromSame = onAddMoreFromSame,
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
                        Text("A sua sacola está vazia", color = CartText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Pesquise um prato ou restaurante", color = CartMuted, fontSize = 13.sp)
                    }
                }
            } else {
                // ── Item list ─────────────────────────────────────────────
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartItems) { product ->
                        CartItemRow(product, onRemove = { onRemoveItem(product) })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Order summary ─────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CartCard)
                        .border(1.dp, CartPrimary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total do Pedido", color = CartMuted, fontSize = 14.sp)
                            Text(
                                formatCurrency(total),
                                color = CartSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Checkout button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (!isLoading)
                                        Brush.horizontalGradient(listOf(CartPrimary, Color(0xFFE65100)))
                                    else
                                        Brush.horizontalGradient(listOf(CartCard, CartCard))
                                )
                                .clickable(enabled = !isLoading) { showConfirmDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = CartPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Preparando pagamento…", color = CartMuted, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Text("Finalizar Pedido ✦", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Confirmation dialog ────────────────────────────────────────────────────
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = CartCard,
            titleContentColor = CartText,
            textContentColor = CartMuted,
            title = { Text("Confirmar pagamento", fontWeight = FontWeight.Bold) },
            text = { Text("Será redirecionado para o pagamento. Deseja continuar?") },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(CartPrimary, Color(0xFFE65100))))
                        .clickable { showConfirmDialog = false; onCheckout() }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Continuar", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar", color = CartMuted)
                }
            }
        )
    }
}

@Composable
fun CartItemRow(product: Product, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CartCard)
            .border(1.dp, CartPrimary.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(12.dp)
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

@Composable
fun RestaurantHeader(
    restaurant: Restaurant,
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
    onAddMoreFromSame: () -> Unit,
    modifier: Modifier = Modifier,
    tts: TextToSpeechService = koinInject()
) {
    val displayedText = remember { mutableStateOf("") }
    var isSpeaking by remember { mutableStateOf(false) }

    // Stop TTS when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose { tts.stop() }
    }

    LaunchedEffect(message) {
        displayedText.value = ""
        tts.stop()
        isSpeaking = false
        for (i in message.indices) {
            displayedText.value = message.substring(0, i + 1)
            kotlinx.coroutines.delay(14)
        }
        // Auto-speak after typewriter finishes
        isSpeaking = true
        tts.speak(stripEmojisForTts(message))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Speaker toggle button
                    IconButton(
                        onClick = {
                            if (isSpeaking) {
                                tts.stop()
                                isSpeaking = false
                            } else {
                                isSpeaking = true
                                tts.speak(stripEmojisForTts(message))
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isSpeaking) "Parar voz" else "Ouvir mensagem",
                            tint = if (isSpeaking) CartAiSecondary else CartAiMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
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
                        .clickable { onSuggestAnotherRestaurant() },
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
                        .clickable { onAddMoreFromSame() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "➕ Mais do mesmo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CartAiSecondary
                    )
                }
            }
        }
    }
}
