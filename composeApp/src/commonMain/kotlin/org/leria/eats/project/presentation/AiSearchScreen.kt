package org.leria.eats.project.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import komaai.composeapp.generated.resources.Res
import komaai.composeapp.generated.resources.logo
import org.leria.eats.project.data.Address
import org.leria.eats.project.data.DeliveryFeeResponse
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
fun AiSearchScreen(
    uiState: SearchUiState,
    isListening: Boolean,
    permissionStatus: PermissionStatus,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onAddToCart: (Product) -> Unit,
    onRemoveFromCart: (Product) -> Unit,
    onCheckout: (Address, Double, Double, String, Map<String, Double>) -> Unit,
    onViewCart: () -> Unit,
    onClearSearch: () -> Unit,
    onChooseProductInChat: (Product) -> Unit = {},
    onQuickPrompt: (String) -> Unit = {},
    onRequestSuggestions: () -> Unit = {},
    onIntroClick: () -> Unit = {},
    onToggleNav: () -> Unit = {},
    onGetDeliveryFee: (suspend (Double, Double, Double, Double, String) -> DeliveryFeeResponse?)? = null,
    onGetAddressFromMap: (Double, Double) -> String? = { _, _ -> null }
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var isCartExpanded by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // ── Dialog de Confirmação para Limpar Chat e Sacola ──────────────────────
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = AiCard,
            titleContentColor = AiText,
            textContentColor = AiTextMuted,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteSweep, null, tint = AiPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Limpar tudo?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Ao limpar a conversa, os itens que a IA adicionou à sua sacola também serão removidos. Deseja continuar?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearSearch()
                    }
                ) {
                    Text("Sim, Limpar", color = KomaSoftRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancelar", color = AiTextMuted)
                }
            }
        )
    }

    // Pulsing glow animation
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.15f, targetValue = 0.55f, label = "glowAlpha",
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    Scaffold(
        topBar = {
            Column {
                AiTopBar(
                    glowAlpha = glowAlpha,
                    isListening = isListening,
                    showClearButton = uiState.chatMessages.size > 1,
                    onClearChat = {
                        if (uiState.cartItems.isNotEmpty()) {
                            showClearConfirmDialog = true
                        } else {
                            onClearSearch()
                        }
                    }
                )
                AiQuickActionsRow(
                    enabled = !uiState.isLoading,
                    onQuickPrompt = onQuickPrompt,
                    onRequestSuggestions = onRequestSuggestions
                )
            }
        },
        bottomBar = {
            AiSemanticInputBar(
                value = uiState.textInput,
                isListening = isListening,
                isLoading = uiState.isLoading,
                isNavVisible = uiState.isBottomNavVisible,
                onValueChange = onTextChange,
                onSend = onSendClick,
                onMic = onMicClick,
                onToggleNav = onToggleNav
            )
        },
        containerColor = AiDeepBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // ── CHAT DE MENSAGENS ──────────────────────────────────
                ChatMessagesView(
                    messages = uiState.chatMessages,
                    isLoading = uiState.isLoading,
                    isStreaming = uiState.isStreaming,
                    onAddToCart = onAddToCart,
                    onProductClick = { product -> selectedProduct = product },
                    onChooseInChat = onChooseProductInChat,
                    modifier = Modifier.fillMaxSize()
                )

                // Boas-vindas centralizado se não houver mensagens e não estiver no fluxo da Sacola IA
                if (uiState.chatMessages.isEmpty() && !uiState.isLoading && !uiState.isAiCartFlow) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AiWelcomeButton(
                            onClick = onIntroClick,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }

            // ── SACOLA IA ITERATIVA (Abaixo do Chat) ─────────────────────────
            if (uiState.isAiCartFlow && uiState.cartItems.isNotEmpty()) {
                AiIterativeCartOverlay(
                    isExpanded = isCartExpanded,
                    onToggle = { isCartExpanded = !isCartExpanded },
                    cartItems = uiState.cartItems,
                    cartRestaurants = uiState.cartRestaurants,
                    userAddresses = uiState.userProfile.addresses,
                    onRemoveItem = onRemoveFromCart,
                    onCheckout = onCheckout,
                    onGetDeliveryFee = onGetDeliveryFee,
                    onGetAddressFromMap = onGetAddressFromMap
                )
            }
        }
    }

    selectedProduct?.let { product ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { selectedProduct = null },
            sheetState = sheetState,
            containerColor = AiSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            ProductDetailBottomSheet(
                product = product,
                onAddToCart = {
                    onAddToCart(product)
                    selectedProduct = null
                },
                onDismiss = { selectedProduct = null }
            )
        }
    }
}

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

@Composable
private fun AiQuickActionsRow(
    enabled: Boolean,
    onQuickPrompt: (String) -> Unit,
    onRequestSuggestions: () -> Unit
) {
    var showSurpriseInfoDialog by remember { mutableStateOf(false) }
    var showSuggestionsInfoDialog by remember { mutableStateOf(false) }

    if (showSurpriseInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSurpriseInfoDialog = false },
            containerColor = AiCard,
            titleContentColor = AiText,
            textContentColor = AiTextMuted,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎁 ", fontSize = 20.sp)
                    Text("Caixa Surpresa", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("A IA busca, para você, restaurantes que oferecem caixa surpresa disponíveis para agendamento — uma seleção de itens do dia por um preço especial, com data e horário marcados para retirada ou entrega.")
            },
            confirmButton = {
                TextButton(onClick = { showSurpriseInfoDialog = false }) {
                    Text("Entendi", color = AiPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showSuggestionsInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSuggestionsInfoDialog = false },
            containerColor = AiCard,
            titleContentColor = AiText,
            textContentColor = AiTextMuted,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡 ", fontSize = 20.sp)
                    Text("Pedir sugestões", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Baseada na personalização alimentar do seu perfil, a IA irá buscar as melhores sugestões para você.")
            },
            confirmButton = {
                TextButton(onClick = { showSuggestionsInfoDialog = false }) {
                    Text("Entendi", color = AiPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AiSurface)
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AiQuickActionChip(
            emoji = "🎁",
            label = "Caixa Surpresa",
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onQuickPrompt("Me surpreenda! Escolha algo saboroso para mim.") },
            onInfoClick = { showSurpriseInfoDialog = true }
        )
        AiQuickActionChip(
            emoji = "💡",
            label = "Pedir sugestões",
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = onRequestSuggestions,
            onInfoClick = { showSuggestionsInfoDialog = true }
        )
    }
}

@Composable
private fun AiQuickActionChip(
    emoji: String,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(AiCard)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(AiPrimary.copy(alpha = 0.5f), AiSecondary.copy(alpha = 0.3f))),
                shape = RoundedCornerShape(50.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 14.dp, end = if (onInfoClick != null) 6.dp else 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (enabled) AiText else AiTextMuted,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (onInfoClick != null) {
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Saber mais sobre $label",
                    tint = AiPrimary.copy(alpha = if (enabled) 0.8f else 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatMessagesView(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    isStreaming: Boolean,
    onAddToCart: (Product) -> Unit,
    onProductClick: (Product) -> Unit = {},
    onChooseInChat: (Product) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val lastMessageTextLength = messages.lastOrNull()?.text?.length ?: 0

    LaunchedEffect(messages.size, isLoading, isStreaming, lastMessageTextLength) {
        if (messages.isNotEmpty() || isLoading || isStreaming) {
            val lastIndex = if (isLoading || isStreaming) messages.size else messages.size - 1
            if (lastIndex >= 0) {
                // Scroll mais rápido/suave durante o streaming
                listState.animateScrollToItem(lastIndex)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            val isLastMessage = message == messages.lastOrNull()
            when (message.type) {
                ChatMessageType.USER -> UserMessageBubble(message = message)
                ChatMessageType.AI -> AiMessageBubble(
                    message = message,
                    isTyping = isLastMessage && isStreaming,
                    onAddToCart = onAddToCart,
                    onProductClick = onProductClick,
                    onChooseInChat = onChooseInChat
                )
            }
        }

        if (isLoading) {
            item { AiTypingIndicator() }
        }
    }
}

@Composable
private fun AiIterativeCartOverlay(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    cartItems: List<Product>,
    cartRestaurants: List<Restaurant>,
    userAddresses: List<Address>,
    onRemoveItem: (Product) -> Unit,
    onCheckout: (Address, Double, Double, String, Map<String, Double>) -> Unit,
    onGetDeliveryFee: (suspend (Double, Double, Double, Double, String) -> DeliveryFeeResponse?)? = null,
    onGetAddressFromMap: (Double, Double) -> String? = { _, _ -> null },
    modifier: Modifier = Modifier
) {
    val cartTotal = cartItems.sumOf { it.price * it.quantity }
    val cartCount = cartItems.sumOf { it.quantity }
    val scrollState = rememberScrollState()

    // ── Animação de "pulo" inicial para indicar scroll ───────────────────────
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            delay(800) // Espera a abertura do painel
            if (scrollState.maxValue > 0) {
                scrollState.animateScrollTo(50, tween(400, easing = EaseOutBack))
                delay(100)
                scrollState.animateScrollTo(0, tween(400, easing = EaseInSine))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .graphicsLayer { translationY = 0f } // Stay above input
    ) {
        // ── Painel Expandível (Glassmorphic) ──────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AiCard.copy(alpha = 0.92f)) // Efeito de vidro (leve transparência)
                    .border(1.dp, AiPrimary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .padding(2.dp)
            ) {
                // Conteúdo da Sacola
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = AiPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Minha Sacola IA", fontWeight = FontWeight.Bold, color = AiText, fontSize = 16.sp)
                        }
                        IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = AiTextMuted)
                        }
                    }
                    
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        AiCartChatBubble(
                            cartItems = cartItems,
                            cartRestaurants = cartRestaurants,
                            userAddresses = userAddresses,
                            onRemoveItem = onRemoveItem,
                            onCheckout = onCheckout,
                            onGetDeliveryFee = onGetDeliveryFee,
                            onGetAddressFromMap = onGetAddressFromMap,
                            isIntegrated = true,
                            scrollState = scrollState
                        )

                        // ── Indicador de Scroll Pulsante ─────────────────────────
                        if (scrollState.value < scrollState.maxValue) {
                            val infiniteTransition = rememberInfiniteTransition(label = "scrollHint")
                            val bounce by infiniteTransition.animateFloat(
                                initialValue = 0f, targetValue = 8f,
                                animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
                                label = "bounce"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp)
                                    .graphicsLayer { translationY = bounce }
                                    .size(24.dp)
                                    .background(AiPrimary.copy(alpha = 0.8f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Pílula Flutuante (Estado Minimizado) ──────────────────────────────
        if (!isExpanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Brush.horizontalGradient(listOf(AiPrimary, KomaOrangeEnd)))
                    .clickable { onToggle() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingBag, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sacola IA: $cartCount itens • ${formatCurrency(cartTotal)}",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.Black.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp))
                .background(Brush.linearGradient(listOf(AiPrimary, KomaGoldAccent)))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(text = message.text, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: ChatMessage,
    isTyping: Boolean = false,
    onAddToCart: (Product) -> Unit,
    onProductClick: (Product) -> Unit = {},
    onChooseInChat: (Product) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (message.text.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(Brush.horizontalGradient(listOf(AiBotBubble, AiCard)))
                        .border(1.dp, AiPrimary.copy(alpha = 0.2f), RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = message.text, 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = AiText
                        )
                        if (isTyping) {
                            AiTypingCursor()
                        }
                    }
                }
            }

            if (message.products.isNotEmpty()) {
                message.products.forEach { product ->
                    ProductChatCard(
                        product = product,
                        onAddToCart = { onAddToCart(product) },
                        onClick = { onProductClick(product) },
                        onChooseInChat = { onChooseInChat(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiTypingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    Box(
        modifier = Modifier
            .padding(start = 4.dp, bottom = 2.dp)
            .size(width = 8.dp, height = 16.dp)
            .background(AiPrimary.copy(alpha = alpha), RoundedCornerShape(1.dp))
    )
}

@Composable
private fun AiTypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(AiBotBubble)
                .border(1.dp, AiPrimary.copy(alpha = 0.2f), RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "typing")
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..2).forEach { index ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f, targetValue = 1f, label = "dot$index",
                        animationSpec = infiniteRepeatable(tween(600, delayMillis = index * 180), RepeatMode.Reverse)
                    )
                    Box(modifier = Modifier.size(8.dp).background(AiPrimary.copy(alpha = alpha), CircleShape))
                }
            }
        }
    }
}

@Composable
private fun AiCartChatBubble(
    cartItems: List<Product>,
    cartRestaurants: List<Restaurant>,
    userAddresses: List<Address>,
    onRemoveItem: (Product) -> Unit,
    onCheckout: (Address, Double, Double, String, Map<String, Double>) -> Unit,
    onGetDeliveryFee: (suspend (Double, Double, Double, Double, String) -> DeliveryFeeResponse?)? = null,
    onGetAddressFromMap: (Double, Double) -> String? = { _, _ -> null },
    isIntegrated: Boolean = false, // Flag para remover o fundo redundante no modo overlay
    scrollState: ScrollState = rememberScrollState()
) {
    var selectedAddress by remember { mutableStateOf(userAddresses.firstOrNull()) }
    var selectedDeliveryType by remember { mutableStateOf("delivery") }
    val isPickup = selectedDeliveryType == "pickup"
    
    var deliveryFeesMap by remember { mutableStateOf<Map<String, DeliveryFeeResponse>>(emptyMap()) }
    var feesLoading by remember { mutableStateOf(false) }
    var feesError by remember { mutableStateOf<String?>(null) }

    // Estado para restaurantes fora da área
    var outOfAreaRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    
    val cartTotal = cartItems.sumOf { it.price * it.quantity }
    val serviceFee = (cartTotal * 0.05).coerceIn(0.49, 1.99)
    val totalDeliveryFee = if (isPickup) 0.0 else deliveryFeesMap.values.sumOf { it.delivery_fee }
    val grandTotal = cartTotal + serviceFee + totalDeliveryFee
    
    val groupedItems = remember(cartItems) { cartItems.groupBy { it.restaurant_gid } }

    if (outOfAreaRestaurant != null) {
        AlertDialog(
            onDismissRequest = { outOfAreaRestaurant = null },
            containerColor = AiCard,
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍 ", fontSize = 20.sp)
                    Text("Fora da área", fontWeight = FontWeight.Bold, color = AiText)
                }
            },
            text = {
                Text(
                    "Infelizmente, o restaurante \"${outOfAreaRestaurant?.name}\" não entrega nesta localização. Os itens desta loja serão removidos da sua sacola.",
                    color = AiTextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val restaurantToRemove = outOfAreaRestaurant
                        if (restaurantToRemove != null) {
                            cartItems.filter { it.restaurant_gid == restaurantToRemove.gid }.forEach {
                                onRemoveItem(it)
                            }
                        }
                        outOfAreaRestaurant = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AiPrimary)
                ) {
                    Text("Compreendido", color = Color.Black)
                }
            }
        )
    }

    LaunchedEffect(userAddresses) {
        if (selectedAddress == null && userAddresses.isNotEmpty()) {
            selectedAddress = userAddresses.firstOrNull()
        }
    }

    LaunchedEffect(selectedAddress, isPickup, cartRestaurants) {
        val addr = selectedAddress
        if (addr?.latitude != null && addr.longitude != null && onGetDeliveryFee != null && !isPickup) {
            feesLoading = true
            feesError = null
            try {
                val results = cartRestaurants.filter { it.latitude != null && it.longitude != null }.map { restaurant ->
                    async {
                        try {
                            val feeRes = onGetDeliveryFee(
                                addr.latitude, addr.longitude,
                                restaurant.latitude!!, restaurant.longitude!!,
                                restaurant.gid
                            )
                            restaurant.gid to feeRes
                        } catch (e: Exception) {
                            val errorMsg = e.message ?: ""
                            if (errorMsg.contains("fora da área", ignoreCase = true)) {
                                outOfAreaRestaurant = restaurant
                            }
                            restaurant.gid to null
                        }
                    }
                }.awaitAll()

                val newFees = results.mapNotNull { (gid, feeRes) -> 
                    feeRes?.let { gid to it } 
                }.toMap()

                deliveryFeesMap = newFees
                if (newFees.size < cartRestaurants.size && outOfAreaRestaurant == null) {
                    feesError = "Alguns restaurantes não entregam nesta área."
                }
            } catch (e: Exception) {
                feesError = e.message ?: "Erro ao calcular taxas."
            }
            feesLoading = false
        }
    }

    var showAddressSheet by remember { mutableStateOf(false) }
    if (showAddressSheet) {
        AddressSelectionBottomSheet(
            addresses = userAddresses,
            onAddressSelected = { selectedAddress = it; showAddressSheet = false },
            onDismiss = { showAddressSheet = false }
        )
    }

    // No modo integrado (Overlay), não renderizamos a estrutura de linha externa nem o header de boas vindas
    if (isIntegrated) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            groupedItems.forEach { (restaurantGid, products) ->
                val restaurant = cartRestaurants.find { it.gid == restaurantGid }
                AiCartChatSection(
                    restaurant = restaurant, 
                    restaurantGid = restaurantGid, 
                    products = products
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AiSurface)
                    .border(1.dp, AiPrimary.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AiCard)
                            .padding(4.dp)
                    ) {
                        listOf("delivery" to "Entrega", "pickup" to "Recolha").forEach { (type, label) ->
                            val isSelected = selectedDeliveryType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AiPrimary else Color.Transparent)
                                    .clickable { selectedDeliveryType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (isSelected) Color.Black else AiTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (!isPickup) {
                        Column {
                            Text("Entregar em:", fontSize = 11.sp, color = AiTextMuted, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AiCard)
                                    .clickable { showAddressSheet = true }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = AiPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedAddress?.name ?: "Selecionar endereço", fontSize = 13.sp, color = AiText, fontWeight = FontWeight.Bold, maxLines = 1)
                                    if (selectedAddress != null) {
                                        Text(selectedAddress!!.address, fontSize = 11.sp, color = AiTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Icon(Icons.Default.Edit, null, tint = AiTextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AiSurface)
                    .border(1.dp, AiPrimary.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRowIntegrated("Produtos", formatCurrency(cartTotal))
                    if (!isPickup) SummaryRowIntegrated("Total Entrega", formatCurrency(totalDeliveryFee))
                    SummaryRowIntegrated("Taxa de Serviço", formatCurrency(serviceFee))

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total a pagar", fontWeight = FontWeight.Bold, color = AiText, fontSize = 15.sp)
                        Text(formatCurrency(grandTotal), fontWeight = FontWeight.ExtraBold, color = AiSecondary, fontSize = 20.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    val canConfirm = selectedAddress != null && !feesLoading && (isPickup || deliveryFeesMap.size == cartRestaurants.size)
                    
                    Button(
                        onClick = { 
                            val finalFeesMap = if (isPickup) emptyMap() else deliveryFeesMap.mapValues { it.value.delivery_fee }
                            onCheckout(selectedAddress!!, totalDeliveryFee, serviceFee, selectedDeliveryType, finalFeesMap) 
                        },
                        enabled = canConfirm,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                if (canConfirm) Brush.horizontalGradient(listOf(AiPrimary, KomaOrangeEnd))
                                else SolidColor(AiTextMuted.copy(alpha = 0.1f))
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Confirmar e Pagar", fontWeight = FontWeight.Bold, color = if (canConfirm) Color.White else AiTextMuted, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    } else {
        // MANTÉM O LAYOUT DE BUBBLE ORIGINAL PARA O CHAT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(Brush.horizontalGradient(listOf(AiBotBubble, AiCard)))
                        .border(1.dp, AiPrimary.copy(alpha = 0.3f), RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = AiPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Minha Sacola IA", fontWeight = FontWeight.Bold, color = AiText, fontSize = 15.sp)
                        }
                        Text("Preparei a sua sacola com estes itens:", fontSize = 12.sp, color = AiTextMuted)
                    }
                }

            groupedItems.forEach { (restaurantGid, products) ->
                val restaurant = cartRestaurants.find { it.gid == restaurantGid }
                AiCartChatSection(
                    restaurant = restaurant, 
                    restaurantGid = restaurantGid, 
                    products = products
                )
            }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AiCard)
                        .border(1.dp, AiPrimary.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AiSurface)
                                .padding(4.dp)
                        ) {
                            listOf("delivery" to "Entrega", "pickup" to "Recolha").forEach { (type, label) ->
                                val isSelected = selectedDeliveryType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AiPrimary else Color.Transparent)
                                        .clickable { selectedDeliveryType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSelected) Color.Black else AiTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (!isPickup) {
                            Column {
                                Text("Entregar em:", fontSize = 11.sp, color = AiTextMuted, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AiSurface)
                                        .clickable { showAddressSheet = true }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, null, tint = AiPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(selectedAddress?.name ?: "Selecionar endereço", fontSize = 13.sp, color = AiText, fontWeight = FontWeight.Bold, maxLines = 1)
                                        if (selectedAddress != null) {
                                            Text(selectedAddress!!.address, fontSize = 11.sp, color = AiTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Icon(Icons.Default.Edit, null, tint = AiTextMuted, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AiCard)
                        .border(1.dp, AiPrimary.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isPickup && cartRestaurants.size > 1) {
                            cartRestaurants.forEach { restaurant ->
                                val fee = deliveryFeesMap[restaurant.gid]?.delivery_fee
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Entrega ${restaurant.name}", fontSize = 11.sp, color = AiTextMuted)
                                    if (feesLoading) CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 2.dp, color = AiPrimary)
                                    else Text(formatCurrency(fee ?: 0.0), fontSize = 11.sp, color = AiText)
                                }
                            }
                            HorizontalDivider(color = AiTextMuted.copy(alpha = 0.1f))
                        }

                        SummaryRowIntegrated("Produtos", formatCurrency(cartTotal))
                        if (!isPickup) SummaryRowIntegrated("Total Entrega", formatCurrency(totalDeliveryFee))
                        SummaryRowIntegrated("Taxa de Serviço", formatCurrency(serviceFee))

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total a pagar", fontWeight = FontWeight.Bold, color = AiText, fontSize = 15.sp)
                            Text(formatCurrency(grandTotal), fontWeight = FontWeight.ExtraBold, color = AiSecondary, fontSize = 18.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        val canConfirm = selectedAddress != null && !feesLoading && (isPickup || deliveryFeesMap.size == cartRestaurants.size)
                        
                        Button(
                            onClick = { 
                                val finalFeesMap = if (isPickup) emptyMap() else deliveryFeesMap.mapValues { it.value.delivery_fee }
                                onCheckout(selectedAddress!!, totalDeliveryFee, serviceFee, selectedDeliveryType, finalFeesMap) 
                            },
                            enabled = canConfirm,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(
                                    if (canConfirm) Brush.horizontalGradient(listOf(AiPrimary, KomaOrangeEnd))
                                    else SolidColor(AiTextMuted.copy(alpha = 0.1f))
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = if (canConfirm) Color.White else AiTextMuted, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Confirmar e Pagar", fontWeight = FontWeight.Bold, color = if (canConfirm) Color.White else AiTextMuted, fontSize = 14.sp)
                                }
                            }
                        }

                        if (feesError != null && !isPickup) {
                            Text(feesError!!, color = KomaSoftRed, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRowIntegrated(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = AiTextMuted)
        Text(value, fontSize = 12.sp, color = AiText, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AiCartChatSection(
    restaurant: Restaurant?,
    restaurantGid: String?,
    products: List<Product>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AiCard.copy(alpha = 0.5f))
            .border(1.dp, AiTextMuted.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cabeçalho compact do restaurante
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AiSurface)) {
                if (restaurant?.image_url != null) {
                    KamelImage(resource = asyncPainterResource(restaurant.image_url), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Restaurant, null, tint = AiPrimary, modifier = Modifier.padding(6.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(restaurant?.name ?: "Loja #$restaurantGid", fontWeight = FontWeight.Bold, color = AiText, fontSize = 13.sp)
                Text(restaurant?.category ?: "Delivery", color = AiTextMuted, fontSize = 10.sp)
            }
        }

        // Itens
        products.forEach { product ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text("x${product.quantity}", fontWeight = FontWeight.ExtraBold, color = AiPrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(product.name, color = AiText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatCurrency(product.price * product.quantity), color = AiSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ProductChatCard(
    product: Product,
    onAddToCart: () -> Unit,
    onClick: () -> Unit = {},
    onChooseInChat: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(AiCard)
            .border(1.dp, AiSecondary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, AiSecondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!product.image_url.isNullOrBlank()) {
                    KamelImage(
                        resource = asyncPainterResource(data = product.image_url),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onLoading = { Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(AiSecondary.copy(alpha = 0.2f), Color.Transparent))), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AiSecondary) } },
                        onFailure = { Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(AiSecondary.copy(alpha = 0.2f), Color.Transparent))), contentAlignment = Alignment.Center) { Text("🍕", fontSize = 24.sp) } }
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(AiSecondary.copy(alpha = 0.2f), Color.Transparent))), contentAlignment = Alignment.Center) { Text("🍕", fontSize = 24.sp) }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AiText)
                Text(text = "${product.price} €", style = MaterialTheme.typography.bodyMedium, color = AiSecondary, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = onChooseInChat,
                modifier = Modifier
                    .size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Escolher ${product.name}",
                    tint = AiSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun GeminiListeningFeedback() {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(animation = tween(1400, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(animation = tween(1400, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .padding(horizontal = 12.dp)
            .graphicsLayer {
                scaleX = pulseScale
                alpha = pulseAlpha / 0.35f
            }
            .clip(RoundedCornerShape(1.5.dp))
            .background(Brush.horizontalGradient(colors = listOf(AiCard.copy(alpha = pulseAlpha), AiSurface.copy(alpha = pulseAlpha * 1.2f), AiPrimary.copy(alpha = pulseAlpha * 0.8f), AiSecondary.copy(alpha = pulseAlpha * 0.7f), AiSurface.copy(alpha = pulseAlpha * 1.2f), AiCard.copy(alpha = pulseAlpha))))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSemanticInputBar(
    value: String,
    isListening: Boolean,
    isLoading: Boolean,
    isNavVisible: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit,
    onToggleNav: () -> Unit
) {
    val borderAlpha by rememberInfiniteTransition(label = "border").animateFloat(
        initialValue = 0.3f, targetValue = 0.9f, label = "borderAlpha",
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp, top = 4.dp, start = 8.dp, end = 8.dp)
            .imePadding()
    ) {
        AnimatedVisibility(visible = isListening, enter = fadeIn(tween(300)) + expandVertically(tween(300)), exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            Column {
                GeminiListeningFeedback()
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
            val dotScale by rememberInfiniteTransition(label = "dot").animateFloat(
                initialValue = 1f, targetValue = if (isListening) 1.4f else 1f,
                animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "dotScale"
            )
            Box(modifier = Modifier.size(6.dp).graphicsLayer { scaleX = dotScale; scaleY = dotScale }.background(if (isListening) AiAccent else AiSecondary, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = if (isListening) "🎤 A ouvir em linguagem natural..." else "Descreva o que deseja — a IA compreende", fontSize = 10.sp, color = if (isListening) AiAccent else AiTextMuted)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Ícone de Menu (Trigger do BottomBar) ─────────────────────────
            Box(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AiCard)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(AiPrimary.copy(alpha = 0.5f), AiSecondary.copy(alpha = 0.3f))),
                        shape = CircleShape
                    )
                    .clickable { onToggleNav() },
                contentAlignment = Alignment.Center
            ) {
                val rotation by animateFloatAsState(
                    targetValue = if (isNavVisible) 180f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "rotation"
                )
                Icon(
                    imageVector = if (isNavVisible) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = AiPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }

            // ── Barra de Input ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(AiCard)
                    .then(
                        if (isListening) {
                            val gradientShift by rememberInfiniteTransition(label = "gradientBorder").animateFloat(
                                initialValue = 0f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                                label = "shift"
                            )
                            Modifier.border(3.dp, Brush.sweepGradient(colors = listOf(AiAccent, AiPrimary, KomaGoldAccent, AiSecondary, KomaGreenDark, AiAccent), center = androidx.compose.ui.geometry.Offset(x = gradientShift * 1000f, y = gradientShift * 1000f)), RoundedCornerShape(28.dp))
                        } else {
                            Modifier.border(1.5.dp, Brush.horizontalGradient(listOf(AiPrimary.copy(alpha = borderAlpha * 0.6f), AiSecondary.copy(alpha = borderAlpha * 0.4f))), RoundedCornerShape(28.dp))
                        }
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AiPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                TextField(
                    value = value, onValueChange = onValueChange,
                    placeholder = { Text("Ex: \"Uma pizza de calabresa\"...", color = AiTextMuted, fontSize = 14.sp) },
                    enabled = !isLoading,
                    singleLine = false,
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = AiText,
                        unfocusedTextColor = AiText,
                        cursorColor = AiPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                )

                // ── Botão Enviar / Microfone (mesma posição, um substitui o outro) ──
                val trailingKey = when {
                    isLoading -> "loading"
                    value.isNotBlank() -> "send"
                    else -> "mic"
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = trailingKey,
                        transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                        label = "trailingAction"
                    ) { key ->
                        when (key) {
                            "loading" -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = AiPrimary,
                                    strokeWidth = 2.dp
                                )
                            }
                            "send" -> {
                                IconButton(
                                    onClick = onSend,
                                    modifier = Modifier
                                        .size(42.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Enviar",
                                        tint = AiAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            else -> {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isListening) {
                                        val infiniteMicTransition = rememberInfiniteTransition(label = "micWaves")
                                        val outerScale by infiniteMicTransition.animateFloat(initialValue = 1f, targetValue = 1.8f, animationSpec = infiniteRepeatable(animation = tween(1200, easing = EaseOutQuad), repeatMode = RepeatMode.Restart), label = "outerScale")
                                        val outerAlpha by infiniteMicTransition.animateFloat(initialValue = 0.6f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "outerAlpha")
                                        val middleScale by infiniteMicTransition.animateFloat(initialValue = 1f, targetValue = 1.6f, animationSpec = infiniteRepeatable(animation = tween(1200, 150, easing = EaseOutQuad), repeatMode = RepeatMode.Restart), label = "middleScale")
                                        val middleAlpha by infiniteMicTransition.animateFloat(initialValue = 0.5f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(1200, 150, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "middleAlpha")
                                        val innerScale by infiniteMicTransition.animateFloat(initialValue = 1f, targetValue = 1.4f, animationSpec = infiniteRepeatable(animation = tween(1200, 300, easing = EaseOutQuad), repeatMode = RepeatMode.Restart), label = "innerScale")
                                        val innerAlpha by infiniteMicTransition.animateFloat(initialValue = 0.4f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(1200, 300, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "innerAlpha")
                                        Box(modifier = Modifier.size(40.dp).graphicsLayer { scaleX = outerScale; scaleY = outerScale; alpha = outerAlpha }.background(Brush.radialGradient(colors = listOf(AiAccent.copy(alpha = 0.3f), AiPrimary.copy(alpha = 0.2f), Color.Transparent)), CircleShape))
                                        Box(modifier = Modifier.size(40.dp).graphicsLayer { scaleX = middleScale; scaleY = middleScale; alpha = middleAlpha }.background(Brush.radialGradient(colors = listOf(AiSecondary.copy(alpha = 0.4f), KomaGoldAccent.copy(alpha = 0.3f), Color.Transparent)), CircleShape))
                                        Box(modifier = Modifier.size(40.dp).graphicsLayer { scaleX = innerScale; scaleY = innerScale; alpha = innerAlpha }.background(Brush.radialGradient(colors = listOf(AiPrimary.copy(alpha = 0.5f), AiAccent.copy(alpha = 0.4f), Color.Transparent)), CircleShape))
                                    }
                                    IconButton(onClick = onMic, enabled = !isLoading, modifier = Modifier.size(40.dp)) {
                                        val micAlpha by rememberInfiniteTransition(label = "mic").animateFloat(initialValue = if (isListening) 0.4f else 1f, targetValue = 1f, label = "micPulse", animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse))
                                        if (isListening) { Box(modifier = Modifier.size(32.dp).background(Brush.radialGradient(colors = listOf(AiAccent.copy(alpha = 0.3f), Color.Transparent)), CircleShape)) }
                                        Icon(imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Microfone", tint = if (isListening) AiAccent.copy(alpha = micAlpha) else AiPrimary, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDetailBottomSheet(
    product: Product,
    onAddToCart: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(AiSurface).padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.width(44.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(AiTextMuted.copy(alpha = 0.3f)))
        Spacer(modifier = Modifier.height(20.dp))
        if (!product.image_url.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp))) {
                KamelImage(resource = asyncPainterResource(data = product.image_url), contentDescription = product.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, onLoading = { Box(Modifier.fillMaxSize().background(AiCard), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(40.dp), strokeWidth = 3.dp, color = AiSecondary) } }, onFailure = { Box(Modifier.fillMaxSize().background(AiCard), contentAlignment = Alignment.Center) { Text("🍕", fontSize = 80.sp) } })
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        Text(text = product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AiText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "${product.price} €", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AiSecondary)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (product.rating != null && product.rating > 0) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("⭐", fontSize = 16.sp); Text(text = product.rating.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AiText) } }
            if (product.preparationTime.isNotBlank()) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("⏱️", fontSize = 16.sp); Text(text = product.preparationTime, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AiText) } }
        }
        if (product.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AiCard).padding(16.dp)) { Column { Text(text = "Descrição", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AiPrimary); Spacer(modifier = Modifier.height(8.dp)); Text(text = product.description, style = MaterialTheme.typography.bodyMedium, color = AiText, lineHeight = 22.sp) } }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AiWelcomeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome")
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.05f, animationSpec = infiniteRepeatable(animation = tween(1500, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse), label = "scale")
    Box(modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }.background(Brush.radialGradient(listOf(AiPrimary.copy(alpha = 0.2f), Color.Transparent)), RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Brush.horizontalGradient(listOf(AiPrimary, KomaOrangeEnd))).clickable(onClick = onClick).padding(1.5.dp).clip(RoundedCornerShape(23.dp)).background(AiCard).padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(36.dp).background(Brush.radialGradient(listOf(AiPrimary.copy(alpha = 0.2f), Color.Transparent)), CircleShape).border(1.dp, AiPrimary.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AiPrimary, modifier = Modifier.size(20.dp)) }
            Column { Text(text = "Primeira vez aqui?", style = MaterialTheme.typography.labelSmall, color = AiPrimary, fontWeight = FontWeight.Bold); Text(text = "Clique para me conhecer! ✨", style = MaterialTheme.typography.bodyMedium, color = AiText, fontWeight = FontWeight.SemiBold) }
        }
    }
}
