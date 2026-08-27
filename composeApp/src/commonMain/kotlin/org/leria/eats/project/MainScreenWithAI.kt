package org.leria.eats.project

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.data.Address
import org.leria.eats.project.presentation.*
import org.leria.eats.project.presentation.components.WebView
import org.leria.eats.project.presentation.viewmodel.SearchViewModel
import org.leria.eats.project.service.LocationService
import org.leria.eats.project.voice.VoiceRecognizer
import org.leria.eats.project.voice.VoiceContext
import org.leria.eats.project.voice.TextToSpeechService
import org.leria.eats.project.theme.*

// ─── Função auxiliar para limpar texto para TTS ──────────────────────────────
/**
 * Prepara o texto para Text-to-Speech:
 * - Remove emojis e símbolos especiais
 * - Substitui "x1" por "uma"
 * - Normaliza espaços
 */
private fun prepareTextForTts(text: String): String {
    val processedText = text
        // Trata preços: "13.00 €", "13,00€", "13€" -> "13 euros" ou "13 euros e 50 cêntimos"
        // 1. Caso com decimais diferentes de zero: 13,50 € -> 13 euros e 50 cêntimos
        .replace(Regex("""(\d+)[.,]([1-9]\d|0[1-9])\s*€""")) { match ->
            "${match.groupValues[1]} euros e ${match.groupValues[2]} cêntimos"
        }
        // 2. Caso com decimais zero ou sem decimais: 13.00 € ou 13 € -> 13 euros
        .replace(Regex("""(\d+)(?:[.,]00)?\s*€""")) { match ->
            "${match.groupValues[1]} euros"
        }
        // 3. Caso o preço apareça sem o símbolo no final mas com formato de preço: 13,00 -> 13 euros
        .replace(Regex("""\b(\d+)[.,]00\b""")) { match ->
            "${match.groupValues[1]} euros"
        }

    return processedText
        // Remove caracteres de formatação (Markdown) e parênteses que poluem a fala
        .replace("*", "")
        .replace("_", "")
        .replace("#", "")
        .replace("(", " ")
        .replace(")", " ")
        // Remove emojis e símbolos Unicode (exceto pontuação básica como . , ? !)
        .replace(Regex("[\\p{So}\\p{Sm}\\p{Sk}\\p{Sc}]"), "")
        .replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]"), "") // surrogate emoji pairs
        .replace(Regex("[\u2600-\u27FF]"), "")  // misc symbols, dingbats, arrows
        .replace(Regex("[\uFE00-\uFE0F]"), "")  // variation selectors
        // Ajustes de fala comuns
        .replace(Regex("\\bx1\\b", RegexOption.IGNORE_CASE), "uma")
        .replace(Regex("\\bx(\\d+)\\b", RegexOption.IGNORE_CASE), "$1 unidades de")
        // Normaliza espaços múltiplos
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}

// ─── Aliases locais → paleta central ─────────────────────────────────────────
private val KomaDeepBg = KomaBg
private val KomaNavGold  = KomaGold
private val KomaNavGreen = KomaBrandGreen
private val KomaMuted    = KomaTextSec

@Composable
fun MainScreenWithAI(
    permissionManager: PermissionManager,
    viewModel: SearchViewModel = koinViewModel(),
    locationService: LocationService = koinInject()
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val voiceRecognizer = koinInject<VoiceRecognizer>()
    val tts = koinInject<TextToSpeechService>()
    val voiceText by voiceRecognizer.results.collectAsState()
    val isListening by voiceRecognizer.isListening.collectAsState()
    val voiceContext by voiceRecognizer.currentContext.collectAsState()
    val permissionStatus by permissionManager.status.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isMuted by remember { mutableStateOf(false) }

    // Reproduz automaticamente a última mensagem da IA no chat
    LaunchedEffect(uiState.chatMessages.size) {
        if (!isMuted && uiState.chatMessages.isNotEmpty()) {
            val lastMessage = uiState.chatMessages.lastOrNull()
            if (lastMessage?.type == ChatMessageType.AI && lastMessage.text.isNotBlank()) {
                val cleanedText = prepareTextForTts(lastMessage.text)
                if (cleanedText.isNotBlank()) {
                    tts.speak(cleanedText)
                }
            }
        }
    }


    LaunchedEffect(uiState.orderJustPlaced) {
        if (uiState.orderJustPlaced && !isMuted) {
            delay(500)

            val successMessage = "Pedido realizado com sucesso! Acompanhe agora o seu estado em Os Meus Pedidos."
            tts.speak(successMessage)

            delay(1000)
            viewModel.resetOrderJustPlacedFlag()
        }
    }

    LaunchedEffect(isMuted) {
        if (isMuted) tts.stop()
    }

    LaunchedEffect(uiState.cartError) {
        uiState.cartError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearCartError()
        }
    }

    // Só processa voz se o contexto for AI ou Home Search
    LaunchedEffect(voiceText, voiceContext) {
        // Só atualiza o input se o contexto for de IA ou Home Search
        if (voiceText.isNotEmpty() && (voiceContext == VoiceContext.AI_SEARCH || voiceContext == VoiceContext.HOME_SEARCH)) {
            viewModel.updateInputFromVoice(voiceText)
        }
    }

    // Auto-envia assim que o microfone fecha (seja por pausa detectada automaticamente
    // -estilo Gemini- ou por clique manual no botão) - apenas para AI e Home Search.
    // Única fonte de envio automático: evita disparos duplicados de sendSearch().
    LaunchedEffect(isListening, voiceContext) {
        if (!isListening && (voiceContext == VoiceContext.AI_SEARCH || voiceContext == VoiceContext.HOME_SEARCH)) {
            val capturedText = voiceText.trim()
            if (capturedText.isNotEmpty()) {
                viewModel.updateInputFromVoice(capturedText)
                delay(300)
                viewModel.sendSearch()
            }
        }
    }

    LaunchedEffect(permissionStatus) {
        if (permissionStatus != PermissionStatus.GRANTED) voiceRecognizer.stopListening()
    }

    // Limpa os resultados de voz ao trocar de tab.
    // Importante: limpar ANTES de parar o microfone, para que o texto já
    // esteja vazio no instante em que isListening vira false — evitando que
    // o efeito de auto-envio dispare uma busca com um resto de transcrição.
    LaunchedEffect(uiState.currentTab) {
        voiceRecognizer.clearResults()
        voiceRecognizer.stopListening()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        var webViewLoading by remember { mutableStateOf(false) }


        AnimatedVisibility(
            visible = uiState.checkoutUrl != null,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
        ) {
            uiState.checkoutUrl?.let { url ->
                WebView(
                    modifier = Modifier.fillMaxSize(),
                    url = url,
                    onSuccess = { orderId -> viewModel.onPaymentResult(isSuccess = true, orderId = orderId) },
                    onCancel = { viewModel.onPaymentResult(isSuccess = false, orderId = null) },
                    onLoadingChanged = { loading -> webViewLoading = loading }
                )
            }
        }

        if (webViewLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        AnimatedVisibility(
            visible = uiState.isLoading && uiState.checkoutUrl == null && !uiState.isAddressSheetVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Preparando pagamento...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.isProcessingAutoPayment,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Processando pagamento...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Aguarde enquanto confirmamos o pagamento com o cartão salvo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Optional cancel button
                    TextButton(
                        onClick = { viewModel.cancelAutoPayment() }
                    ) {
                        Text("Voltar", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.checkoutUrl == null,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
        ) {
            // Address sheet and main scaffold when there's no checkout URL
            // Validamos isAddressSheetVisible apenas se não for o fluxo de Sacola IA,
            // já que a AiCartScreen lida com seu próprio seletor de endereço.
            if (uiState.isAddressSheetVisible && !uiState.isAiCartFlow) {
                AddressSelectionBottomSheet(
                    addresses = uiState.userProfile.addresses,
                    onAddressSelected = { address ->
                        val hasSavedPaymentMethods = uiState.userProfile.savedPaymentMethods.isNotEmpty()
                        if (hasSavedPaymentMethods) {
                            viewModel.showPaymentConfirmForAddress(address)
                        } else {
                            viewModel.showSavePaymentSheetForAddress(address)
                        }
                    },
                    onDismiss = {
                        viewModel.dismissAddressSheet()
                    }
                )
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    val showBottomBar = if (uiState.currentTab == MainTab.AI) {
                        uiState.isBottomNavVisible
                    } else {
                        !(uiState.currentTab == MainTab.HOME && uiState.selectedRestaurant != null)
                                        && uiState.currentTab != MainTab.CART
                    }
                    
                    AnimatedVisibility(
                        visible = showBottomBar,
                        enter = slideInVertically(
                            animationSpec = tween(durationMillis = 400, easing = EaseInOut),
                            initialOffsetY = { it }
                        ) + fadeIn(animationSpec = tween(durationMillis = 400, easing = EaseInOut)),
                        exit = slideOutVertically(
                            animationSpec = tween(durationMillis = 350, easing = EaseInOut),
                            targetOffsetY = { it }
                        ) + fadeOut(animationSpec = tween(durationMillis = 350, easing = EaseInOut))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, end = 32.dp, bottom = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = KomaDeepBg.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(28.dp),
                                modifier = Modifier
                                    .widthIn(max = 500.dp)
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                KomaNavGold.copy(alpha = 0.1f), 
                                                KomaNavGold.copy(alpha = 0.5f), 
                                                KomaNavGreen.copy(alpha = 0.4f), 
                                                KomaNavGold.copy(alpha = 0.1f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(28.dp)
                                    ),
                                shadowElevation = 12.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp) // Altura ideal para conter ícone + texto sem corte
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val tabs = listOf(
                                        Triple(MainTab.AI, "IA", Icons.Default.AutoAwesome),
                                        Triple(MainTab.HOME, "Explorar", Icons.Default.Restaurant),
                                        Triple(MainTab.ORDERS, "Pedidos", Icons.AutoMirrored.Filled.List),
                                        Triple(MainTab.PROFILE, "Perfil", Icons.Default.Person)
                                    )

                                    tabs.forEach { (tab, label, icon) ->
                                        val isSelected = uiState.currentTab == tab
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) { viewModel.onTabSelected(tab) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                val contentColor by animateColorAsState(
                                                    targetValue = if (isSelected) KomaNavGold else KomaMuted.copy(alpha = 0.5f),
                                                    label = "color"
                                                )
                                                val scale by animateFloatAsState(
                                                    targetValue = if (isSelected) 1.12f else 1f,
                                                    label = "scale"
                                                )

                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    tint = contentColor,
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .graphicsLayer {
                                                            scaleX = scale
                                                            scaleY = scale
                                                        }
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = label,
                                                    fontSize = 9.sp,
                                                    color = contentColor,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1
                                                )
                                                
                                                // Indicador de seleção inferior (dot)
                                                AnimatedVisibility(
                                                    visible = isSelected,
                                                    enter = expandVertically() + fadeIn(),
                                                    exit = shrinkVertically() + fadeOut()
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(top = 2.dp)
                                                            .size(4.dp)
                                                            .background(KomaNavGold, CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } // Row
                            } // Surface
                        } // Box
                    } // AnimatedVisibility
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Animated content transition between tabs
                    AnimatedContent(
                        targetState = uiState.currentTab,
                        transitionSpec = {
                            when {
                                initialState == MainTab.PROFILE && targetState == MainTab.AI -> {
                                    fadeIn(
                                        animationSpec = tween(
                                            durationMillis = 800,
                                            easing = EaseInOut
                                        )
                                    ) togetherWith fadeOut(
                                        animationSpec = tween(
                                            durationMillis = 800,
                                            easing = EaseInOut
                                        )
                                    )
                                }

                                // Profile comes from the right (end of nav bar)
                                targetState == MainTab.PROFILE && initialState != MainTab.PROFILE -> {
                                    slideInHorizontally(
                                        animationSpec = tween(400),
                                        initialOffsetX = { it }
                                    ) + fadeIn(animationSpec = tween(400)) togetherWith
                                    slideOutHorizontally(
                                        animationSpec = tween(400),
                                        targetOffsetX = { -it / 3 }
                                    ) + fadeOut(animationSpec = tween(400))
                                }

                                // Going back from Profile to other screens (except AI which is handled above)
                                initialState == MainTab.PROFILE && targetState != MainTab.PROFILE && targetState != MainTab.AI -> {
                                    slideInHorizontally(
                                        animationSpec = tween(400),
                                        initialOffsetX = { -it / 3 }
                                    ) + fadeIn(animationSpec = tween(400)) togetherWith
                                    slideOutHorizontally(
                                        animationSpec = tween(400),
                                        targetOffsetX = { it }
                                    ) + fadeOut(animationSpec = tween(400))
                                }

                                // Default smooth fade for other tab transitions
                                else -> {
                                    fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                                }
                            }
                        },
                        label = "tab_transition"
                    ) { currentTab ->
                        when (currentTab) {
                            MainTab.HOME -> {
                                HomeScreen(
                                    // ...existing code...
                                    uiState = uiState,
                                    isListening = isListening && voiceContext == VoiceContext.HOME_SEARCH,
                                    permissionStatus = permissionStatus,
                                    onMicClick = {
                                        when (permissionStatus) {
                                            PermissionStatus.IDLE -> permissionManager.askForPermission()
                                            PermissionStatus.DENIED -> permissionManager.openSettings()
                                            PermissionStatus.GRANTED -> {
                                                if (isListening) voiceRecognizer.stopListening()
                                                else {
                                                    viewModel.onQueryChange("")
                                                    voiceRecognizer.startListening(VoiceContext.HOME_SEARCH)
                                                }
                                            }
                                        }
                                    },
                                    onSendClick = {
                                        if (isListening) voiceRecognizer.stopListening()
                                        viewModel.sendSearch()
                                    },
                                    onTextChange = { viewModel.onQueryChange(it) },
                                    onRestaurantClick = { restaurant -> viewModel.selectRestaurantOrAddToCart(restaurant) },
                                    onCategorySelect = { category -> viewModel.selectCategory(category) },
                                    onClearSelection = { viewModel.clearSelection() },
                                    onClearSelectionAndCart = { viewModel.clearSelectionAndCart() },
                                    onAddToCart = { product -> viewModel.addToCart(product) },
                                    onRemoveFromCart = { product -> viewModel.removeFromCart(product) },
                                    onViewCart = { viewModel.onTabSelected(MainTab.CART) },
                                    onClearSearch = { viewModel.clearSearch() }
                                )
                            }
                            MainTab.AI -> {
                                AiSearchScreen(
                                    uiState = uiState,
                                    isListening = isListening && voiceContext == VoiceContext.AI_SEARCH,
                                    permissionStatus = permissionStatus,
                                    onMicClick = {
                                        when (permissionStatus) {
                                            PermissionStatus.IDLE -> permissionManager.askForPermission()
                                            PermissionStatus.DENIED -> permissionManager.openSettings()
                                            PermissionStatus.GRANTED -> {
                                                if (isListening) voiceRecognizer.stopListening()
                                                else {
                                                    viewModel.onQueryChange("")
                                                    voiceRecognizer.startListening(VoiceContext.AI_SEARCH)
                                                }
                                            }
                                        }
                                    },
                                    onSendClick = {
                                        if (isListening) voiceRecognizer.stopListening()
                                        viewModel.sendSearch()
                                    },
                                    onTextChange = { viewModel.onQueryChange(it) },
                                    onAddToCart = { product -> viewModel.addToCart(product) },
                                    onRemoveFromCart = { product -> viewModel.removeFromCart(product) },
                                    onCheckout = { address, deliveryFee, serviceFee, deliveryType, feesMap ->
                                        viewModel.checkoutWithAddress(address, deliveryFee, serviceFee, deliveryType, feesMap)
                                    },
                                    onViewCart = { viewModel.onTabSelected(MainTab.CART) },
                                    onClearSearch = { 
                                        viewModel.clearSearch()
                                        viewModel.clearCart()
                                    },
                                    onToggleNav = { viewModel.toggleBottomNav() },
                                    onGetDeliveryFee = { custLat, custLon, restLat, restLon, restGid ->
                                        viewModel.getDeliveryFee(custLat, custLon, restLat, restLon, restGid)
                                    },
                                    onGetAddressFromMap = { lat, long ->
                                        locationService.getAddressFromCoordinates(lat, long)
                                    },
                                    onIntroClick = {
                                        val introText = """
                                            Olá! Que bom ter você por aqui! 👋
                                            Esqueça a ideia de ficar rolando cardápios infinitos. Eu sou o seu assistente pessoal de delivery e estou aqui para facilitar a sua vida.
                                            
                                            Veja como é fácil pedir comigo:
                                            
                                            🧠 Eu entendo o seu jeito de falar. Você não precisa buscar pelo nome 
                                            exato do prato. Pode me dizer coisas como: "Quero uma pizza para 3 pessoas",
                                             "Me mostre opções vegetarianas" ou "Estou com vontade de comer um doce sem lactose". Eu vasculho os restaurantes cadastrados no meu banco de dados e 
                                             te respondo em segundos, como um garçom de verdade, a melhor opção pra você. 
                                            Fique à vontade para pedir o que gosta e pedir sugestões!
                                        
                                                                       
                                            Me diga, o que você está com vontade de comer hoje?
                                        """.trimIndent()
                                        
                                        viewModel.addAiMessage(introText)
                                    }
                                )
                            }
                            MainTab.CART -> {
                                val uniqueRestaurantsInCart = uiState.cartItems.map { it.restaurant_gid }.distinct().size
                                if (uiState.isAiCartFlow || uniqueRestaurantsInCart > 1 || uiState.cartRestaurants.size > 1) {
                                    AiCartScreen(
                                        cartItems = uiState.cartItems,
                                        cartRestaurants = uiState.cartRestaurants,
                                        onAddItem = { product -> viewModel.addToCart(product) },
                                        onRemoveItem = { product -> viewModel.removeFromCart(product) },
                                        onCheckout = { address, deliveryFee, serviceFee, deliveryType, feesMap ->
                                            viewModel.checkoutWithAddress(address, deliveryFee, serviceFee, deliveryType, feesMap)
                                        },
                                        userAddresses = uiState.userProfile.addresses,
                                        onGetAddressFromMap = { lat, long ->
                                            locationService.getAddressFromCoordinates(lat, long)
                                        },
                                        onGetDeliveryFee = { custLat, custLon, restLat, restLon, restGid ->
                                            viewModel.getDeliveryFee(custLat, custLon, restLat, restLon, restGid)
                                        },
                                        onGoToRestaurant = { restaurant ->
                                            viewModel.selectRestaurant(restaurant)
                                            viewModel.onTabSelected(MainTab.HOME)
                                        },
                                        cartAiMessage = uiState.cartAiMessage,
                                        cartAiMessageSpoken = uiState.cartAiMessageSpoken,
                                        onDismissAiMessage = { viewModel.clearCartAiMessage() },
                                        onSuggestAnotherRestaurant = {
                                            viewModel.suggestAnotherRestaurant()
                                        },
                                        onMarkAiMessageAsSpoken = { viewModel.markCartAiMessageAsSpoken() },
                                        onClearCart = {
                                            viewModel.clearSelectionAndCart()
                                            viewModel.onTabSelected(MainTab.AI)
                                        },
                                        isMuted = isMuted,
                                        onGoToHome = { viewModel.onTabSelected(MainTab.HOME) }
                                    )
                                } else {
                                    CartScreen(
                                        cartItems = uiState.cartItems,
                                        restaurantSelected = uiState.selectedRestaurant,
                                        onAddItem = { product -> viewModel.addToCart(product) },
                                        onRemoveItem = { product -> viewModel.removeFromCart(product) },
                                        onCheckout = { address, deliveryFee, serviceFee, deliveryType, feesMap ->
                                            viewModel.checkoutWithAddress(address, deliveryFee, serviceFee, deliveryType, feesMap)
                                        },
                                        userAddresses = uiState.userProfile.addresses,
                                        onGetAddressFromMap = { lat, long ->
                                            locationService.getAddressFromCoordinates(lat, long)
                                        },
                                        onGetDeliveryFee = { custLat, custLon, restLat, restLon, restGid ->
                                            viewModel.getDeliveryFee(custLat, custLon, restLat, restLon, restGid)
                                        },
                                        onGoToRestaurant = { restaurant ->
                                            viewModel.selectRestaurant(restaurant)
                                            viewModel.onTabSelected(MainTab.HOME)
                                        },
                                        cartAiMessage = uiState.cartAiMessage,
                                        cartAiMessageSpoken = uiState.cartAiMessageSpoken,
                                        onDismissAiMessage = { viewModel.clearCartAiMessage() },
                                        onSuggestAnotherRestaurant = {
                                            viewModel.suggestAnotherRestaurant()
                                        },
                                        onAddMoreFromSame = {
                                            viewModel.clearCartAiMessage()
                                            uiState.selectedRestaurant?.let { r ->
                                                viewModel.selectRestaurant(r)
                                            }
                                            viewModel.onTabSelected(MainTab.HOME)
                                        },
                                        onMarkAiMessageAsSpoken = { viewModel.markCartAiMessageAsSpoken() },
                                        onClearCart = {
                                            viewModel.clearSelectionAndCart()
                                            viewModel.onTabSelected(MainTab.AI)
                                        },
                                        isMuted = isMuted,
                                        onGoToHome = { viewModel.onTabSelected(MainTab.HOME) }
                                    )
                                }
                            }

                            MainTab.ORDERS -> {
                                OrdersScreen(
                                    orders = uiState.orderHistory,
                                    isLoading = uiState.isLoading,
                                    selectedOrder = uiState.selectedOrder,
                                    onRefresh = { viewModel.refreshOrders() },
                                    onOrderClick = { order -> viewModel.selectOrder(order) },
                                    onBackToList = { viewModel.clearOrderSelection() },
                                    isFiltered = uiState.isFilterEnabled,
                                    onFilterToggle = { viewModel.toggleFilter() },
                                    orderItemRatings = uiState.orderItemRatings,
                                    onRateItem = { orderId, productGid, restaurantGid, productName, rating ->
                                        viewModel.rateOrderItem(orderId, productGid, restaurantGid, productName, rating)
                                    },
                                    onMarkDelivered = { orderId -> viewModel.markOrderAsDelivered(orderId) }
                                )
                            }

                            MainTab.PROFILE -> {
                                 // Verificar se o usuário tem perfil
                                if (uiState.userProfile.name.isEmpty()) {
                                    // Onboarding: usuário novo sem perfil
                                    OnboardingChatScreen(
                                        isListening = isListening && voiceContext == VoiceContext.ONBOARDING,
                                        recognizedText = if (voiceContext == VoiceContext.ONBOARDING) voiceText else "",
                                        onMicClick = {
                                            if (permissionStatus == PermissionStatus.GRANTED) {
                                                if (!isListening) voiceRecognizer.startListening(VoiceContext.ONBOARDING)
                                                else voiceRecognizer.stopListening()
                                            } else {
                                                permissionManager.askForPermission()
                                            }
                                        },
                                        onComplete = { name: String, email: String, phone: String, address: Address? ->
                                            // Salvar perfil do usuário e redirecionar para a tela de IA
                                            val addresses: List<Address> = if (address != null) listOf(address) else emptyList()
                                            viewModel.updateUserProfile(name, email, phone, addresses)
                                            viewModel.onTabSelected(MainTab.AI)
                                        },
                                        onGetAddressFromMap = { lat: Double, long: Double ->
                                            locationService.getAddressFromCoordinates(lat, long)
                                        },
                                        tts = tts,
                                        isMuted = isMuted
                                    )
                                } else {
                                    // Usuário já tem perfil: tela de edição
                                    ProfileScreen(
                                        userProfile = uiState.userProfile,
                                        onSave = { name, email, phone, addresses, photoUrl, allergies, lifestyles ->
                                            viewModel.updateUserProfile(name, email, phone, addresses, photoUrl, allergies, lifestyles)
                                            viewModel.onTabSelected(MainTab.HOME)
                                        },
                                        onGetLocation = { callbackUpdateAddress ->
                                            if (permissionStatus == PermissionStatus.GRANTED) {
                                                scope.launch {
                                                    val addressFound = locationService.getCurrentAddress()
                                                    val coords = locationService.getCurrentCoordinates()
                                                    callbackUpdateAddress(
                                                        addressFound ?: "Localização não encontrada",
                                                        coords?.first,
                                                        coords?.second
                                                    )
                                                 }
                                            } else {
                                                permissionManager.askForPermission()
                                                callbackUpdateAddress("", null, null)
                                            }
                                        },
                                        onGetAddressFromMap = { lat, long ->
                                            locationService.getAddressFromCoordinates(lat, long)
                                        },
                                        isMuted = isMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Save Payment Method Sheet
        if (uiState.showSavePaymentSheet) {
            SavePaymentMethodSheet(
                onDismiss = { viewModel.dismissSavePaymentSheet() },
                onConfirm = { savePaymentMethod ->
                    viewModel.proceedToCheckout(savePaymentMethod)
                }
            )
        }

        // Confirm Payment with Saved Card Sheet
        if (uiState.showPaymentConfirmSheet && uiState.userProfile.savedPaymentMethods.isNotEmpty()) {
            PaymentConfirmBottomSheet(
                savedCards = uiState.userProfile.savedPaymentMethods,
                onDismiss = { viewModel.dismissPaymentConfirmSheet() },
                onUseSavedCard = { _ ->
                    viewModel.onPaymentConfirmResult(useSavedCard = true)
                },
                onUseOtherMethod = {
                    viewModel.onPaymentConfirmResult(useSavedCard = false)
                }
            )
        }

        // Delivery type is now selected inline in the order summary (ServiceFeeBottomSheet)
    }
}