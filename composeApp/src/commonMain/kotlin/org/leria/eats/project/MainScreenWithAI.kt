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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.domain.model.Address
import org.leria.eats.project.presentation.*
import org.leria.eats.project.presentation.components.WebView
import org.leria.eats.project.presentation.viewmodel.CartViewModel
import org.leria.eats.project.presentation.viewmodel.ChatViewModel
import org.leria.eats.project.presentation.viewmodel.CheckoutViewModel
import org.leria.eats.project.presentation.viewmodel.ExploreViewModel
import org.leria.eats.project.presentation.viewmodel.LiveCallViewModel
import org.leria.eats.project.presentation.viewmodel.MainViewModel
import org.leria.eats.project.presentation.viewmodel.OrdersViewModel
import org.leria.eats.project.presentation.viewmodel.ProfileViewModel
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

// Tempo de folga em segundo plano antes de considerar o app "realmente fechado" e limpar
// a conversa (ver DisposableEffect em MainScreenWithAI) — evita limpar por uma troca rápida
// de app, notificação ou tela de bloqueio.
private const val APP_FECHADO_GRACE_PERIOD_MS = 2 * 60 * 1000L

// ─── Aliases locais → paleta central ─────────────────────────────────────────
private val KomaDeepBg = KomaBg
private val KomaNavGold  = KomaGold
private val KomaNavGreen = KomaBrandGreen
private val KomaMuted    = KomaTextSec
private val NavBarGradient = Brush.verticalGradient(
    colors = listOf(KomaTopBarGreenStart, KomaTopBarGreenEnd)
)

@Composable
fun MainScreenWithAI(
    permissionManager: PermissionManager,
    mainViewModel: MainViewModel = koinViewModel(),
    chatViewModel: ChatViewModel = koinViewModel(),
    liveCallViewModel: LiveCallViewModel = koinViewModel(),
    exploreViewModel: ExploreViewModel = koinViewModel(),
    cartViewModel: CartViewModel = koinViewModel(),
    checkoutViewModel: CheckoutViewModel = koinViewModel(),
    ordersViewModel: OrdersViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel(),
    locationService: LocationService = koinInject()
) {
    val scope = rememberCoroutineScope()
    val navigation by mainViewModel.state.collectAsState()
    val conversation by chatViewModel.conversation.collectAsState()
    val chat by chatViewModel.state.collectAsState()
    val live by liveCallViewModel.state.collectAsState()
    val catalog by exploreViewModel.catalog.collectAsState()
    val explore by exploreViewModel.state.collectAsState()
    val cart by cartViewModel.state.collectAsState()
    val checkout by checkoutViewModel.state.collectAsState()
    val orders by ordersViewModel.state.collectAsState()
    val userProfile by profileViewModel.userProfile.collectAsState()
    // Indicador de "a carregar" das telas: qualquer operação em curso (catálogo, IA, checkout ou
    // pedidos), como no tempo em que todas partilhavam um único estado.
    val isLoading = explore.isLoading || chat.isLoading || checkout.isLoading || orders.isLoading
    val voiceRecognizer = koinInject<VoiceRecognizer>()
    val tts = koinInject<TextToSpeechService>()
    val voiceText by voiceRecognizer.results.collectAsState()
    val isListening by voiceRecognizer.isListening.collectAsState()
    val voiceContext by voiceRecognizer.currentContext.collectAsState()
    val permissionStatus by permissionManager.status.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isMuted by remember { mutableStateOf(false) }

    // Nem Android nem iOS garantem um callback confiável de "processo realmente encerrado"
    // de forma multiplataforma — ON_STOP dispara todo vez que o app vai pra segundo plano
    // (trocar de app, notificação, tela de bloqueio), não só quando é de fato fechado. Por
    // isso não limpamos na hora: damos uma folga (GRACE_PERIOD) e só chamamos a mesma função
    // do botão "limpar conversa" se o app continuar em segundo plano depois desse prazo — se
    // voltar (ON_START) antes disso, foi só uma saída rápida e cancelamos a limpeza.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var pendingClearJob: Job? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    pendingClearJob?.cancel()
                    pendingClearJob = scope.launch {
                        delay(APP_FECHADO_GRACE_PERIOD_MS)
                        chatViewModel.clearSearch()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    pendingClearJob?.cancel()
                    pendingClearJob = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            pendingClearJob?.cancel()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Voz da IA (TTS): só deve falar quando o usuário usou o microfone (voz) ou
    // está em ligação de voz ativa — nunca em resposta a mensagens digitadas.
    val shouldUseAiVoice = chat.lastInputWasVoice || live.isActive

    // Toca a fala da IA já pré-sintetizada para o turno por voz (ver ChatViewModel.
    // fetchSearch: o áudio é sintetizado ANTES da mensagem ser revelada, então aqui é só
    // reprodução local — sem espera de rede — no mesmo instante em que o texto aparece.
    // Depois do pagamento o app vai para "Meus pedidos": encerra a ligação de voz (microfone,
    // áudio e tela de ligação) para que, ao voltar na aba da IA, apareça a tela inicial do chat
    // (AiSearchScreen) e não a ligação antiga.
    LaunchedEffect(navigation.currentTab) {
        if (navigation.currentTab == MainTab.ORDERS && live.isActive) {
            liveCallViewModel.stopLiveConversation()
        }
    }

    LaunchedEffect(chat.pendingVoiceAudio) {
        val audio = chat.pendingVoiceAudio
        if (audio != null) {
            if (!isMuted && shouldUseAiVoice) {
                tts.playAudio(audio)
            }
            chatViewModel.clearPendingVoiceAudio()
        }
    }


    LaunchedEffect(checkout.orderJustPlaced) {
        if (checkout.orderJustPlaced && !isMuted) {
            delay(500)

            if (shouldUseAiVoice) {
                val successMessage = "Pedido realizado com sucesso! Acompanhe agora o seu estado em Os Meus Pedidos."
                tts.speak(successMessage)
            }

            delay(1000)
            checkoutViewModel.resetOrderJustPlacedFlag()
        }
    }

    LaunchedEffect(isMuted) {
        if (isMuted) tts.stop()
    }

    LaunchedEffect(cart.error) {
        cart.error?.let {
            snackbarHostState.showSnackbar(it)
            cartViewModel.clearCartError()
        }
    }

    // Só processa voz se o contexto for AI ou Home Search
    LaunchedEffect(voiceText, voiceContext) {
        // Só atualiza o input se o contexto for de IA ou Home Search
        if (voiceText.isNotEmpty() && (voiceContext == VoiceContext.AI_SEARCH || voiceContext == VoiceContext.HOME_SEARCH)) {
            chatViewModel.updateInputFromVoice(voiceText)
        }
    }

    // Auto-envia assim que o microfone fecha (seja por pausa detectada automaticamente
    // -estilo Gemini- ou por clique manual no botão) - apenas para AI e Home Search.
    // Única fonte de envio automático: evita disparos duplicados de sendSearch().
    LaunchedEffect(isListening, voiceContext) {
        if (!isListening && (voiceContext == VoiceContext.AI_SEARCH || voiceContext == VoiceContext.HOME_SEARCH)) {
            val capturedText = voiceText.trim()
            if (capturedText.isNotEmpty()) {
                chatViewModel.updateInputFromVoice(capturedText)
                delay(300)
                chatViewModel.sendSearch()
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
    LaunchedEffect(navigation.currentTab) {
        voiceRecognizer.clearResults()
        voiceRecognizer.stopListening()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        var webViewLoading by remember { mutableStateOf(false) }


        AnimatedVisibility(
            visible = checkout.checkoutUrl != null,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
        ) {
            checkout.checkoutUrl?.let { url ->
                WebView(
                    modifier = Modifier.fillMaxSize(),
                    url = url,
                    onSuccess = { orderId -> checkoutViewModel.onPaymentResult(isSuccess = true, orderId = orderId) },
                    onCancel = { checkoutViewModel.onPaymentResult(isSuccess = false, orderId = null) },
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
            visible = isLoading && checkout.checkoutUrl == null && !checkout.isAddressSheetVisible,
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
            visible = checkout.isProcessingAutoPayment,
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
                        onClick = { checkoutViewModel.cancelAutoPayment() }
                    ) {
                        Text("Voltar", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = checkout.checkoutUrl == null,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
        ) {
            // Address sheet and main scaffold when there's no checkout URL
            // Validamos isAddressSheetVisible apenas se não for o fluxo de Sacola IA,
            // já que a AiCartScreen lida com seu próprio seletor de endereço.
            if (checkout.isAddressSheetVisible && !cart.isAiCartFlow) {
                AddressSelectionBottomSheet(
                    addresses = userProfile.addresses,
                    onAddressSelected = { address ->
                        val hasSavedPaymentMethods = userProfile.savedPaymentMethods.isNotEmpty()
                        if (hasSavedPaymentMethods) {
                            checkoutViewModel.showPaymentConfirmForAddress(address)
                        } else {
                            checkoutViewModel.showSavePaymentSheetForAddress(address)
                        }
                    },
                    onDismiss = {
                        checkoutViewModel.dismissAddressSheet()
                    }
                )
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    val isOnboarding = navigation.currentTab == MainTab.PROFILE && userProfile.name.isBlank()
                    val showBottomBar = !isOnboarding && if (navigation.currentTab == MainTab.AI) {
                        navigation.isBottomNavVisible
                    } else {
                        !(navigation.currentTab == MainTab.HOME && catalog.selectedRestaurant != null)
                                        && navigation.currentTab != MainTab.CART
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
                                color = Color.Transparent,
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
                                        .background(NavBarGradient)
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
                                        val isSelected = navigation.currentTab == tab
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) { mainViewModel.onTabSelected(tab) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                val contentColor by animateColorAsState(
                                                    targetValue = if (isSelected) KomaNavGold else Color.White.copy(alpha = 0.55f),
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
                    // Animated content transition between tabs — desliza para o lado seguindo a
                    // ordem das abas na barra inferior (IA, Explorar, Pedidos, Perfil), como um
                    // arrastar de tela: avançar desliza da direita, voltar desliza da esquerda.
                    val tabOrder = remember { listOf(MainTab.AI, MainTab.HOME, MainTab.ORDERS, MainTab.PROFILE) }
                    AnimatedContent(
                        targetState = navigation.currentTab,
                        transitionSpec = {
                            val indoAdiante = tabOrder.indexOf(targetState) >= tabOrder.indexOf(initialState)
                            val direcao = if (indoAdiante) 1 else -1
                            (
                                slideInHorizontally(
                                    animationSpec = tween(350, easing = EaseInOut),
                                    initialOffsetX = { fullWidth -> direcao * fullWidth }
                                ) + fadeIn(animationSpec = tween(350))
                            ) togetherWith (
                                slideOutHorizontally(
                                    animationSpec = tween(350, easing = EaseInOut),
                                    targetOffsetX = { fullWidth -> -direcao * fullWidth / 3 }
                                ) + fadeOut(animationSpec = tween(250))
                            )
                        },
                        label = "tab_transition"
                    ) { currentTab ->
                        when (currentTab) {
                            MainTab.HOME -> {
                                HomeScreen(
                                    catalog = catalog,
                                    cartItems = cart.items,
                                    isLoading = isLoading,
                                    isListening = isListening && voiceContext == VoiceContext.HOME_SEARCH,
                                    permissionStatus = permissionStatus,
                                    onMicClick = {
                                        when (permissionStatus) {
                                            PermissionStatus.IDLE -> permissionManager.askForPermission()
                                            PermissionStatus.DENIED -> permissionManager.openSettings()
                                            PermissionStatus.GRANTED -> {
                                                if (isListening) voiceRecognizer.stopListening()
                                                else {
                                                    chatViewModel.onQueryChange("")
                                                    voiceRecognizer.startListening(VoiceContext.HOME_SEARCH)
                                                }
                                            }
                                        }
                                    },
                                    onSendClick = {
                                        if (isListening) voiceRecognizer.stopListening()
                                        chatViewModel.sendSearch()
                                    },
                                    onTextChange = { chatViewModel.onQueryChange(it) },
                                    onRestaurantClick = { restaurant -> exploreViewModel.selectRestaurantOrAddToCart(restaurant) },
                                    onCategorySelect = { category -> exploreViewModel.selectCategory(category) },
                                    onClearSelection = { exploreViewModel.clearSelection() },
                                    onClearSelectionAndCart = { exploreViewModel.clearSelectionAndCart() },
                                    onAddToCart = { product -> cartViewModel.addToCart(product) },
                                    onRemoveFromCart = { product -> cartViewModel.removeFromCart(product) },
                                    onViewCart = { mainViewModel.onTabSelected(MainTab.CART) },
                                    onClearSearch = { chatViewModel.clearSearch() }
                                )
                            }
                            MainTab.AI -> {
                                AiSearchScreen(
                                    conversation = conversation,
                                    live = live,
                                    cart = cart,
                                    allRestaurants = catalog.allRestaurants,
                                    userAddresses = userProfile.addresses,
                                    isLoading = isLoading,
                                    isStreaming = chat.isStreaming || live.isStreaming,
                                    isBottomNavVisible = navigation.isBottomNavVisible,
                                    isListening = isListening && voiceContext == VoiceContext.AI_SEARCH,
                                    permissionStatus = permissionStatus,
                                    onMicClick = {
                                        when (permissionStatus) {
                                            PermissionStatus.IDLE -> permissionManager.askForPermission()
                                            PermissionStatus.DENIED -> permissionManager.openSettings()
                                            PermissionStatus.GRANTED -> {
                                                if (isListening) voiceRecognizer.stopListening()
                                                else {
                                                    // Mesma exclusividade de microfone do outro sentido
                                                    // (ver onToggleLiveConversation): não dá pra ter STT e
                                                    // conversa ao vivo usando o hardware ao mesmo tempo.
                                                    if (live.isActive) liveCallViewModel.stopLiveConversation()
                                                    chatViewModel.onQueryChange("")
                                                    voiceRecognizer.startListening(VoiceContext.AI_SEARCH)
                                                }
                                            }
                                        }
                                    },
                                    onSendClick = {
                                        if (isListening) voiceRecognizer.stopListening()
                                        if (live.isActive) liveCallViewModel.stopLiveConversation()
                                        chatViewModel.sendSearch()
                                    },
                                    onTextChange = { chatViewModel.onQueryChange(it) },
                                    onAddToCart = { product -> cartViewModel.addToCart(product) },
                                    onRemoveFromCart = { product -> cartViewModel.removeFromCart(product) },
                                    onChooseProductInChat = { product, quantity ->
                                        // Durante a ligação de voz o pedido vai pela própria ligação.
                                        if (live.isActive) liveCallViewModel.orderInCall(product, quantity)
                                        else chatViewModel.chooseProductInChat(product, quantity)
                                    },
                                    onQuickPrompt = { prompt -> chatViewModel.sendQuickPrompt(prompt) },
                                    onRequestSuggestions = { chatViewModel.requestPersonalizedSuggestions() },
                                    onToggleLiveConversation = {
                                        if (live.isActive) {
                                            liveCallViewModel.stopLiveConversation()
                                        } else {
                                            when (permissionStatus) {
                                                PermissionStatus.IDLE -> permissionManager.askForPermission()
                                                PermissionStatus.DENIED -> permissionManager.openSettings()
                                                PermissionStatus.GRANTED -> {
                                                    // O microfone (STT) e a conversa ao vivo são mutuamente
                                                    // exclusivos — ambos usam o hardware do microfone.
                                                    if (isListening) voiceRecognizer.stopListening()
                                                    liveCallViewModel.startLiveConversation()
                                                }
                                            }
                                        }
                                    },
                                    onToggleMicMuted = { liveCallViewModel.toggleMicMuted() },
                                    onCheckout = { address, deliveryFee, serviceFee, deliveryType, feesMap ->
                                        checkoutViewModel.checkoutWithAddress(address, deliveryFee, serviceFee, deliveryType, feesMap)
                                    },
                                    onViewCart = { mainViewModel.onTabSelected(MainTab.CART) },
                                    onClearSearch = {
                                        chatViewModel.clearSearch()
                                        cartViewModel.clearCart()
                                    },
                                    onToggleNav = { mainViewModel.toggleBottomNav() },
                                    onGetDeliveryFee = { custLat, custLon, restLat, restLon, restGid ->
                                        checkoutViewModel.getDeliveryFee(custLat, custLon, restLat, restLon, restGid)
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
                                        
                                        chatViewModel.addAiMessage(introText)
                                    }
                                )
                            }
                            MainTab.CART -> {
                                val uniqueRestaurantsInCart = cart.items.map { it.restaurant_gid }.distinct().size
                                if (cart.isAiCartFlow || uniqueRestaurantsInCart > 1 || cart.restaurants.size > 1) {
                                    AiCartScreen(
                                        cartItems = cart.items,
                                        cartRestaurants = cart.restaurants,
                                        onAddItem = { product -> cartViewModel.addToCart(product) },
                                        onRemoveItem = { product -> cartViewModel.removeFromCart(product) },
                                        onCheckout = { address, deliveryFee, serviceFee, deliveryType, feesMap ->
                                            checkoutViewModel.checkoutWithAddress(address, deliveryFee, serviceFee, deliveryType, feesMap)
                                        },
                                        userAddresses = userProfile.addresses,
                                        onGetAddressFromMap = { lat, long ->
                                            locationService.getAddressFromCoordinates(lat, long)
                                        },
                                        onGetDeliveryFee = { custLat, custLon, restLat, restLon, restGid ->
                                            checkoutViewModel.getDeliveryFee(custLat, custLon, restLat, restLon, restGid)
                                        },
                                        onGoToRestaurant = { restaurant ->
                                            exploreViewModel.selectRestaurant(restaurant)
                                            mainViewModel.onTabSelected(MainTab.HOME)
                                        },
                                        cartAiMessage = cart.aiMessage,
                                        cartAiMessageSpoken = cart.aiMessageSpoken,
                                        onDismissAiMessage = { cartViewModel.clearCartAiMessage() },
                                        onSuggestAnotherRestaurant = {
                                            exploreViewModel.suggestAnotherRestaurant()
                                        },
                                        onMarkAiMessageAsSpoken = { cartViewModel.markCartAiMessageAsSpoken() },
                                        onClearCart = {
                                            exploreViewModel.clearSelectionAndCart()
                                            mainViewModel.onTabSelected(MainTab.AI)
                                        },
                                        isMuted = isMuted,
                                        onGoToHome = { mainViewModel.onTabSelected(MainTab.HOME) }
                                    )
                                } else {
                                    CartScreen(
                                        cartItems = cart.items,
                                        restaurantSelected = catalog.selectedRestaurant,
                                        onAddItem = { product -> cartViewModel.addToCart(product) },
                                        onRemoveItem = { product -> cartViewModel.removeFromCart(product) },
                                        onCheckout = { address, deliveryFee, serviceFee, deliveryType, feesMap ->
                                            checkoutViewModel.checkoutWithAddress(address, deliveryFee, serviceFee, deliveryType, feesMap)
                                        },
                                        userAddresses = userProfile.addresses,
                                        onGetAddressFromMap = { lat, long ->
                                            locationService.getAddressFromCoordinates(lat, long)
                                        },
                                        onGetDeliveryFee = { custLat, custLon, restLat, restLon, restGid ->
                                            checkoutViewModel.getDeliveryFee(custLat, custLon, restLat, restLon, restGid)
                                        },
                                        onGoToRestaurant = { restaurant ->
                                            exploreViewModel.selectRestaurant(restaurant)
                                            mainViewModel.onTabSelected(MainTab.HOME)
                                        },
                                        cartAiMessage = cart.aiMessage,
                                        cartAiMessageSpoken = cart.aiMessageSpoken,
                                        onDismissAiMessage = { cartViewModel.clearCartAiMessage() },
                                        onSuggestAnotherRestaurant = {
                                            exploreViewModel.suggestAnotherRestaurant()
                                        },
                                        onAddMoreFromSame = {
                                            cartViewModel.clearCartAiMessage()
                                            catalog.selectedRestaurant?.let { r ->
                                                exploreViewModel.selectRestaurant(r)
                                            }
                                            mainViewModel.onTabSelected(MainTab.HOME)
                                        },
                                        onMarkAiMessageAsSpoken = { cartViewModel.markCartAiMessageAsSpoken() },
                                        onClearCart = {
                                            exploreViewModel.clearSelectionAndCart()
                                            mainViewModel.onTabSelected(MainTab.AI)
                                        },
                                        isMuted = isMuted,
                                        onGoToHome = { mainViewModel.onTabSelected(MainTab.HOME) }
                                    )
                                }
                            }

                            MainTab.ORDERS -> {
                                OrdersScreen(
                                    orders = orders.orders,
                                    isLoading = isLoading,
                                    selectedOrder = orders.selectedOrder,
                                    onRefresh = { ordersViewModel.refreshOrders() },
                                    onOrderClick = { order -> ordersViewModel.selectOrder(order) },
                                    onBackToList = { ordersViewModel.clearOrderSelection() },
                                    isFiltered = orders.isFilterEnabled,
                                    onFilterToggle = { ordersViewModel.toggleFilter() },
                                    orderItemRatings = orders.orderItemRatings,
                                    onRateItem = { orderId, productGid, restaurantGid, productName, rating ->
                                        ordersViewModel.rateOrderItem(orderId, productGid, restaurantGid, productName, rating)
                                    },
                                    onMarkDelivered = { orderId -> ordersViewModel.markOrderAsDelivered(orderId) }
                                )
                            }

                            MainTab.PROFILE -> {
                                 // Verificar se o usuário tem perfil
                                if (userProfile.name.isEmpty()) {
                                    // Onboarding: usuário novo sem perfil
                                    OnboardingChatScreen(
                                        onComplete = { name: String, email: String, phone: String, address: Address? ->
                                            // Salvar perfil do usuário e redirecionar para a tela de IA
                                            val addresses: List<Address> = if (address != null) listOf(address) else emptyList()
                                            profileViewModel.updateUserProfile(name, email, phone, addresses)
                                            mainViewModel.onTabSelected(MainTab.AI)
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
                                        userProfile = userProfile,
                                        onSave = { name, email, phone, addresses, photoUrl, allergies, lifestyles ->
                                            profileViewModel.updateUserProfile(name, email, phone, addresses, photoUrl, allergies, lifestyles)
                                            mainViewModel.onTabSelected(MainTab.HOME)
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
        if (checkout.showSavePaymentSheet) {
            SavePaymentMethodSheet(
                onDismiss = { checkoutViewModel.dismissSavePaymentSheet() },
                onConfirm = { savePaymentMethod ->
                    checkoutViewModel.proceedToCheckout(savePaymentMethod)
                }
            )
        }

        // Confirm Payment with Saved Card Sheet
        if (checkout.showPaymentConfirmSheet && userProfile.savedPaymentMethods.isNotEmpty()) {
            PaymentConfirmBottomSheet(
                savedCards = userProfile.savedPaymentMethods,
                onDismiss = { checkoutViewModel.dismissPaymentConfirmSheet() },
                onUseSavedCard = { _ ->
                    checkoutViewModel.onPaymentConfirmResult(useSavedCard = true)
                },
                onUseOtherMethod = {
                    checkoutViewModel.onPaymentConfirmResult(useSavedCard = false)
                }
            )
        }

        // Delivery type is now selected inline in the order summary (ServiceFeeBottomSheet)
    }
}