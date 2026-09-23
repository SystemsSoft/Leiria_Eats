package org.leria.eats.project.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import org.leria.eats.project.data.Address
import org.leria.eats.project.data.DeliveryFeeResponse
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.presentation.components.AiTopBar
import org.leria.eats.project.presentation.util.StatusBarLightIcons
import org.leria.eats.project.presentation.util.buildChargedFeesMap
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
private val AiSurpriseBox = KomaSurpriseBox
private val AiTopBarGradient = Brush.verticalGradient(
    colors = listOf(KomaTopBarGreenStart, KomaTopBarGreenEnd)
)
// Cor de fundo do Scaffold por trás do card arredondado: só aparece nos cantos
// que a curva do card (topStart/topEnd) deixa "de fora" — precisa casar com o
// verde escuro da base do gradiente do topo pra dar a impressão de continuidade.
private val AiScaffoldBg = KomaTopBarGreenEnd
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

    // Ícones claros na status bar enquanto a barra verde escura desta tela estiver visível
    StatusBarLightIcons(enabled = true)

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

    // Barra fixa (não recolhe ao rolar) — com "enterAlwaysScrollBehavior" a
    // TopAppBar encolhia sua altura medida durante a rolagem, mas o Column que a
    // envolve (fundo verde escuro + card arredondado abaixo) não acompanhava esse
    // encolhimento no mesmo frame, deixando uma faixa sem fundo (branca) visível
    // por trás durante a transição.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AiTopBarGradient)
            ) {
                AiTopBar(
                    compact = true,
                    showClearButton = uiState.chatMessages.size > 1,
                    onClearChat = {
                        if (uiState.cartItems.isNotEmpty()) {
                            showClearConfirmDialog = true
                        } else {
                            onClearSearch()
                        }
                    },
                    scrollBehavior = scrollBehavior
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
                onToggleNav = onToggleNav,
                modifier = Modifier.background(AiDeepBg)
            )
        },
        containerColor = AiScaffoldBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(AiDeepBg)
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
                    cartItems = uiState.cartItems,
                    onAddToCart = onAddToCart,
                    onProductClick = { product -> selectedProduct = product },
                    onChooseInChat = onChooseProductInChat,
                    modifier = Modifier.fillMaxSize()
                )

                // Boas-vindas se não houver mensagens e não estiver no fluxo da Sacola IA
                if (uiState.chatMessages.isEmpty() && !uiState.isLoading && !uiState.isAiCartFlow) {
                    AiWelcomeHero(
                        onQuickPrompt = onQuickPrompt,
                        onRequestSuggestions = onRequestSuggestions,
                        onIntroClick = onIntroClick,
                        modifier = Modifier.fillMaxSize()
                    )
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

@Composable
private fun AiWelcomeHero(
    onQuickPrompt: (String) -> Unit,
    onRequestSuggestions: () -> Unit,
    onIntroClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(text = "Olá! 👋", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "O que você gostaria\nde pedir hoje?",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AiText,
            lineHeight = 30.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Fale com o KomaAI e faça seu pedido de forma simples, rápida e do seu jeito.",
            fontSize = 13.sp,
            color = AiTextMuted,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))
        AiQuickActionsRow(
            enabled = true,
            onQuickPrompt = onQuickPrompt,
            onRequestSuggestions = onRequestSuggestions
        )

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AiBotBubble)
                .border(1.dp, AiSecondary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .clickable(onClick = onIntroClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AiCard),
                contentAlignment = Alignment.Center
            ) {
                Text("🍽️", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Seu garçom com IA", fontWeight = FontWeight.Bold, color = AiText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Descreva o que você quer, por áudio ou texto. Eu cuido do resto! ✨",
                    color = AiTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
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
                Text("A IA busca, para você, restaurantes que oferecem caixa surpresa disponíveis para agendamento — uma seleção de itens do dia por um preço especial, com data e horário marcados para retirada.")
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
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AiQuickActionChip(
            emoji = "🎁",
            label = "Caixa Surpresa",
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onQuickPrompt("Quero ver as opções de Caixa Surpresa disponíveis hoje.") },
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(AiCard)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(AiPrimary.copy(alpha = 0.5f), AiSecondary.copy(alpha = 0.3f))),
                shape = RoundedCornerShape(50.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(
                start = 14.dp,
                end = if (onInfoClick != null) 34.dp else 14.dp,
                top = 10.dp,
                bottom = 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 14.sp)
            Text(
                text = label,
                fontSize = 14.sp,
                color = if (enabled) AiText else AiTextMuted,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        if (onInfoClick != null) {
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .size(26.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Saber mais sobre $label",
                    tint = AiPrimary.copy(alpha = if (enabled) 0.8f else 0.4f),
                    modifier = Modifier.size(18.dp)
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
    cartItems: List<Product> = emptyList(),
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
                    cartItems = cartItems,
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
    cartItems: List<Product> = emptyList(),
    onAddToCart: (Product) -> Unit,
    onProductClick: (Product) -> Unit = {},
    onChooseInChat: (Product) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (message.text.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
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
        }

        if (message.products.isNotEmpty()) {
            AiSuggestionSections(
                products = message.products,
                restaurants = message.restaurants,
                cartItems = cartItems,
                onProductClick = onProductClick,
                onChooseInChat = onChooseInChat
            )
        }
    }
}

private fun resolvePlan(product: Product, restaurants: List<Restaurant>): String? {
    return product.restaurantPlan?.takeIf { it.isNotBlank() }
        ?: restaurants.find { it.gid == product.restaurant_gid }?.plan
}

// ── Separa as sugestões em dois blocos por plano do restaurante: os 6 primeiros
//    produtos ESSENCE em "Melhores sugestões" (destaque), e os produtos SMART
//    em "Outras sugestões", com cards um pouco menores.
@Composable
private fun AiSuggestionSections(
    products: List<Product>,
    restaurants: List<Restaurant>,
    cartItems: List<Product> = emptyList(),
    onProductClick: (Product) -> Unit,
    onChooseInChat: (Product) -> Unit
) {
    val melhoresSugestoes = remember(products, restaurants) {
        products.filter { resolvePlan(it, restaurants)?.uppercase() == "ESSENCE" }
            .sortedByDescending { it.rating ?: -1.0 }
            .take(6)
    }
    val outrasSugestoes = remember(products, restaurants) {
        products.filter { resolvePlan(it, restaurants)?.uppercase() == "SMART" }
            .sortedByDescending { it.rating ?: -1.0 }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (melhoresSugestoes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Melhores sugestões",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AiText
                )
                AiProductGrid(
                    products = melhoresSugestoes,
                    restaurants = restaurants,
                    cartItems = cartItems,
                    onProductClick = onProductClick,
                    onChooseInChat = onChooseInChat,
                    compact = false
                )
            }
        }

        if (outrasSugestoes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Outras sugestões",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AiText
                )
                AiProductGrid(
                    products = outrasSugestoes,
                    restaurants = restaurants,
                    cartItems = cartItems,
                    onProductClick = onProductClick,
                    onChooseInChat = onChooseInChat,
                    compact = true
                )
            }
        }
    }
}

// ── Grid de produtos (mesmo padrão visual do Explorar: fileiras de 3, com
//    imagem quadrada, nome, restaurante e avaliação) ──────────────────────────
@Composable
private fun AiProductGrid(
    products: List<Product>,
    restaurants: List<Restaurant>,
    cartItems: List<Product> = emptyList(),
    onProductClick: (Product) -> Unit,
    onChooseInChat: (Product) -> Unit,
    compact: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        products.chunked(3).forEach { rowProducts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowProducts.forEach { product ->
                    val quantityInCart = cartItems.find { it.gid == product.gid }?.quantity ?: 0
                    AiProductGridCard(
                        product = product,
                        restaurantName = product.restaurantName?.takeIf { it.isNotBlank() }
                            ?: restaurants.find { it.gid == product.restaurant_gid }?.name,
                        quantityInCart = quantityInCart,
                        onClick = { onProductClick(product) },
                        onChooseInChat = { onChooseInChat(product) },
                        compact = compact,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowProducts.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AiProductGridCard(
    product: Product,
    restaurantName: String?,
    onClick: () -> Unit,
    onChooseInChat: () -> Unit,
    quantityInCart: Int = 0,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardPressScale"
    )
    val isSelected = quantityInCart > 0

    // "Outras sugestões" (SMART) usa cards um pouco menores que "Melhores sugestões" (ESSENCE).
    val cornerRadius = if (compact) 12.dp else 14.dp
    val nameSize = if (compact) 9.sp else 10.sp
    val restaurantSize = if (compact) 8.sp else 9.sp
    val ratingSize = if (compact) 8.sp else 9.sp
    val priceSize = if (compact) 9.sp else 10.sp
    val surpriseSize = if (compact) 7.sp else 8.sp
    val textPadding = if (compact) 5.dp else 6.dp
    val fallbackEmojiSize = if (compact) 18.sp else 22.sp
    val addButtonSize = if (compact) 24.dp else 28.dp
    val addIconSize = if (compact) 14.dp else 16.dp
    val qtyBadgeSize = if (compact) 18.dp else 20.dp

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation = if (isPressed) 1.dp else 5.dp, shape = RoundedCornerShape(cornerRadius), clip = false)
            .clip(RoundedCornerShape(cornerRadius))
            .background(AiCard)
            .border(1.dp, AiSecondary.copy(alpha = 0.15f), RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
            ) {
                if (!product.image_url.isNullOrBlank()) {
                    KamelImage(
                        resource = asyncPainterResource(data = product.image_url),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onLoading = { Box(Modifier.fillMaxSize().background(AiSurface), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AiPrimary) } },
                        onFailure = { Box(Modifier.fillMaxSize().background(AiSurface), contentAlignment = Alignment.Center) { Text("🍕", fontSize = fallbackEmojiSize) } }
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(AiSurface), contentAlignment = Alignment.Center) { Text("🍕", fontSize = fallbackEmojiSize) }
                }

                if (product.isSurpriseBox) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = AiSurpriseBox.copy(alpha = 0.92f)
                    ) {
                        Text(text = "🎁", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }

                // Selo de quantidade — mesmo padrão do cardápio (RestaurantDetailScreen).
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(qtyBadgeSize)
                            .background(AiSecondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$quantityInCart", color = Color.White, fontSize = ratingSize, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(textPadding),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    color = AiText,
                    fontSize = nameSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
                if (!restaurantName.isNullOrBlank()) {
                    Text(
                        text = restaurantName,
                        color = AiTextMuted,
                        fontSize = restaurantSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
                if (product.rating != null && product.rating > 0) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("⭐", fontSize = ratingSize)
                        Text(text = product.rating.toString(), color = AiTextMuted, fontSize = ratingSize, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (product.isSurpriseBox) {
                    val start = product.surpriseBoxPickupStart
                    val end = product.surpriseBoxPickupEnd
                    Text(
                        text = if (!start.isNullOrBlank() && !end.isNullOrBlank()) "🎁 $start–$end" else "🎁 Caixa Surpresa",
                        color = AiSurpriseBox,
                        fontSize = surpriseSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(if (compact) 3.dp else 4.dp))

                // Linha inferior com preço à esquerda e botão amarelo quadrado (+) à direita
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatCurrency(product.price),
                        color = AiSecondary,
                        fontSize = priceSize,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Start
                    )

                    Box(
                        modifier = Modifier
                            .size(addButtonSize)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AiSecondary else AiPrimary)
                            .clickable { onChooseInChat() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Adicionar ${product.name}",
                            tint = if (isSelected) Color.White else Color(0xFF1E293B),
                            modifier = Modifier.size(addIconSize)
                        )
                    }
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
    // Um pedido com item de Caixa Surpresa é exclusivo e só pode ser recolhido no
    // restaurante — mesma regra aplicada no backend (gate em order_routes.py) e nos
    // demais checkouts do app (ServiceFeeBottomSheet, AiServiceFeeBottomSheet).
    val forcePickupOnly = cartItems.any { it.isSurpriseBox }
    var selectedDeliveryType by remember(forcePickupOnly) { mutableStateOf(if (forcePickupOnly) "pickup" else "delivery") }
    val isPickup = selectedDeliveryType == "pickup"
    
    var deliveryFeesMap by remember { mutableStateOf<Map<String, DeliveryFeeResponse>>(emptyMap()) }
    var feesLoading by remember { mutableStateOf(false) }
    var feesError by remember { mutableStateOf<String?>(null) }

    // Estado para restaurantes fora da área
    var outOfAreaRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    
    val cartTotal = cartItems.sumOf { it.price * it.quantity }
    val serviceFee = (cartTotal * 0.05).coerceIn(0.49, 1.99)
    // Primeiro restaurante da rota cobra a taxa de entrega normal; 2º e 3º cobram a taxa de
    // recolha (valor fixo por distância até o primeiro), em vez de somar todas as taxas.
    val chargedFeesMap: Map<String, Double> = remember(cartRestaurants, deliveryFeesMap, isPickup) {
        if (isPickup) emptyMap() else buildChargedFeesMap(cartRestaurants, deliveryFeesMap)
    }
    val totalDeliveryFee = if (isPickup) 0.0 else chargedFeesMap.values.sum()
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
                        val deliveryTypeOptions = if (forcePickupOnly) listOf("pickup" to "Recolha")
                            else listOf("delivery" to "Entrega", "pickup" to "Recolha")
                        deliveryTypeOptions.forEach { (type, label) ->
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
                    if (!isPickup && cartRestaurants.size > 1) {
                        cartRestaurants.forEachIndexed { index, restaurant ->
                            val fee = chargedFeesMap[restaurant.gid]
                            val label = if (index == 0) "Entrega" else "Recolha"
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("$label ${restaurant.name}", fontSize = 11.sp, color = AiTextMuted)
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
                        Text(formatCurrency(grandTotal), fontWeight = FontWeight.ExtraBold, color = AiSecondary, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val canConfirm = selectedAddress != null && !feesLoading && (isPickup || chargedFeesMap.size == cartRestaurants.size)

                    Button(
                        onClick = {
                            onCheckout(selectedAddress!!, totalDeliveryFee, serviceFee, selectedDeliveryType, chargedFeesMap)
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
                            val deliveryTypeOptions = if (forcePickupOnly) listOf("pickup" to "Recolha")
                            else listOf("delivery" to "Entrega", "pickup" to "Recolha")
                        deliveryTypeOptions.forEach { (type, label) ->
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
                            cartRestaurants.forEachIndexed { index, restaurant ->
                                val fee = chargedFeesMap[restaurant.gid]
                                val label = if (index == 0) "Entrega" else "Recolha"
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("$label ${restaurant.name}", fontSize = 11.sp, color = AiTextMuted)
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

                        val canConfirm = selectedAddress != null && !feesLoading && (isPickup || chargedFeesMap.size == cartRestaurants.size)
                        
                        Button(
                            onClick = { 
                                onCheckout(selectedAddress!!, totalDeliveryFee, serviceFee, selectedDeliveryType, chargedFeesMap)
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
    onToggleNav: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderAlpha by rememberInfiniteTransition(label = "border").animateFloat(
        initialValue = 0.3f, targetValue = 0.9f, label = "borderAlpha",
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    Column(
        modifier = modifier
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AiPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Nota: BasicTextField em vez de TextField — o TextField do Material3 tem
                // padding interno pensado pra formulário (altura mínima, espaço pra label),
                // o que desalinhava o texto verticalmente em relação aos ícones de 44dp ao
                // lado. BasicTextField não carrega esse padding, então o texto centraliza de
                // verdade com o Row inteiro em Alignment.CenterVertically — prática padrão
                // pra barras de busca/chat compactas em Compose.
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !isLoading,
                    singleLine = false,
                    maxLines = 5,
                    textStyle = LocalTextStyle.current.copy(color = AiText, fontSize = 14.sp),
                    cursorBrush = SolidColor(AiPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                Text("Ex: \"Uma pizza de calabresa\"...", color = AiTextMuted, fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    }
                )

                // ── Botão Enviar / Microfone (mesma posição, um substitui o outro) ──
                val trailingKey = when {
                    isLoading -> "loading"
                    value.isNotBlank() -> "send"
                    else -> "mic"
                }
                Box(
                    modifier = Modifier.size(44.dp),
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
        if (product.isSurpriseBox) {
            Spacer(modifier = Modifier.height(10.dp))
            val start = product.surpriseBoxPickupStart
            val end = product.surpriseBoxPickupEnd
            Surface(shape = RoundedCornerShape(8.dp), color = AiSurpriseBox.copy(alpha = 0.12f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(text = "🎁 ", fontSize = 13.sp)
                    Text(
                        text = if (!start.isNullOrBlank() && !end.isNullOrBlank()) {
                            "Caixa Surpresa — recolha entre $start e $end"
                        } else {
                            "Caixa Surpresa — restaurante ainda não definiu o horário de recolha"
                        },
                        color = AiSurpriseBox,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
