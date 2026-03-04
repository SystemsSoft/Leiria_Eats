package org.leria.eats.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.presentation.*
import org.leria.eats.project.presentation.components.WebView
import org.leria.eats.project.presentation.viewmodel.SearchViewModel
import org.leria.eats.project.service.LocationService
import org.leria.eats.project.voice.VoiceRecognizer
import org.leria.eats.project.voice.TextToSpeechService

// ─── Paleta KOMAAI ────────────────────────────────────────────────────────────
private val KomaDeepBg   = Color(0xFF061510)
private val KomaGold     = Color(0xFFFFC107)
private val KomaGreen    = Color(0xFF4ADE80)
private val KomaMuted    = Color(0xFF6EE7A0)

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
    val permissionStatus by permissionManager.status.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isMuted by remember { mutableStateOf(false) }

    // Falar a saudação quando o nome for carregado
    LaunchedEffect(uiState.aiReply) {
        if (!isMuted && (uiState.aiReply.startsWith("Olá") || uiState.aiReply.startsWith("Outras opções"))) {
            tts.speak(uiState.aiReply)
        }
    }

    // Stop TTS immediately when muted
    LaunchedEffect(isMuted) {
        if (isMuted) tts.stop()
    }

    LaunchedEffect(uiState.cartError) {
        uiState.cartError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearCartError()
        }
    }

    LaunchedEffect(uiState.cartMessage) {
        uiState.cartMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearCartMessage()
        }
    }

    LaunchedEffect(voiceText) {
        if (isListening && voiceText.isNotEmpty()) {
            viewModel.updateInputFromVoice(voiceText)
        }
    }

    LaunchedEffect(permissionStatus) {
        if (permissionStatus != PermissionStatus.GRANTED) voiceRecognizer.stopListening()
    }

    // Use animated visibility to smoothly transition between main UI and WebView
    Box(modifier = Modifier.fillMaxSize()) {
        var webViewLoading by remember { mutableStateOf(false) }

        // Smooth transition for WebView (already present)
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

        // Overlay progress indicator while WebView internal resources are loading
        if (webViewLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // New: show a full-screen animated loading overlay after user confirms (e.g., confirmCheckout)
        // This covers the period between starting the checkout request and receiving the checkoutUrl
        AnimatedVisibility(
            // only show loading overlay when we're waiting for checkoutUrl and the address bottom sheet is not visible
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
            visible = uiState.checkoutUrl == null,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
        ) {
            // Address sheet and main scaffold when there's no checkout URL
            if (uiState.isAddressSheetVisible) {
                AddressSelectionBottomSheet(
                    addresses = uiState.userProfile.addresses,
                    onAddressSelected = { address ->
                        viewModel.confirmCheckout(address)
                    },
                    onDismiss = {
                        viewModel.dismissAddressSheet()
                    }
                )
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KomaDeepBg)
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        KomaGold.copy(alpha = 0.0f),
                                        KomaGold.copy(alpha = 0.35f),
                                        KomaGreen.copy(alpha = 0.25f),
                                        KomaGold.copy(alpha = 0.0f)
                                    )
                                ),
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            contentColor = KomaMuted,
                            tonalElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val selectedColor = KomaGold
                            val unselectedColor = KomaMuted.copy(alpha = 0.55f)

                            // ── Início ────────────────────────────────────────
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                                label = {
                                    Text(
                                        "Início",
                                        fontSize = 10.sp,
                                        fontWeight = if (uiState.currentTab == MainTab.HOME) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = uiState.currentTab == MainTab.HOME,
                                onClick = { viewModel.onTabSelected(MainTab.HOME) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = KomaDeepBg,
                                    selectedTextColor = selectedColor,
                                    indicatorColor = KomaGold,
                                    unselectedIconColor = unselectedColor,
                                    unselectedTextColor = unselectedColor
                                )
                            )

                            // ── Sacola ────────────────────────────────────────
                            NavigationBarItem(
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (uiState.cartCount > 0) {
                                                Badge(
                                                    containerColor = KomaGreen,
                                                    contentColor = KomaDeepBg
                                                ) {
                                                    Text(
                                                        uiState.cartCount.toString(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = "Sacola")
                                    }
                                },
                                label = {
                                    Text(
                                        "Sacola",
                                        fontSize = 10.sp,
                                        fontWeight = if (uiState.currentTab == MainTab.CART) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = uiState.currentTab == MainTab.CART,
                                onClick = { viewModel.onTabSelected(MainTab.CART) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = KomaDeepBg,
                                    selectedTextColor = selectedColor,
                                    indicatorColor = KomaGold,
                                    unselectedIconColor = unselectedColor,
                                    unselectedTextColor = unselectedColor
                                )
                            )

                            // ── Pedidos ───────────────────────────────────────
                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Pedidos") },
                                label = {
                                    Text(
                                        "Pedidos",
                                        fontSize = 10.sp,
                                        fontWeight = if (uiState.currentTab == MainTab.ORDERS) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = uiState.currentTab == MainTab.ORDERS,
                                onClick = { viewModel.onTabSelected(MainTab.ORDERS) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = KomaDeepBg,
                                    selectedTextColor = selectedColor,
                                    indicatorColor = KomaGold,
                                    unselectedIconColor = unselectedColor,
                                    unselectedTextColor = unselectedColor
                                )
                            )

                            // ── Favoritos ─────────────────────────────────────
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Star, contentDescription = "Favoritos") },
                                label = {
                                    Text(
                                        "Favoritos",
                                        fontSize = 10.sp,
                                        fontWeight = if (uiState.currentTab == MainTab.FAVORITES) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = uiState.currentTab == MainTab.FAVORITES,
                                onClick = { viewModel.onTabSelected(MainTab.FAVORITES) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = KomaDeepBg,
                                    selectedTextColor = selectedColor,
                                    indicatorColor = KomaGold,
                                    unselectedIconColor = unselectedColor,
                                    unselectedTextColor = unselectedColor
                                )
                            )

                            // ── Som ───────────────────────────────────────────
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = if (isMuted) "Ativar som" else "Desativar som"
                                    )
                                },
                                label = {
                                    Text(
                                        if (isMuted) "Som off" else "Som",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                },
                                selected = false,
                                onClick = { isMuted = !isMuted },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = KomaDeepBg,
                                    selectedTextColor = selectedColor,
                                    indicatorColor = KomaGold,
                                    unselectedIconColor = if (isMuted) KomaMuted.copy(alpha = 0.4f) else KomaMuted.copy(alpha = 0.55f),
                                    unselectedTextColor = if (isMuted) KomaMuted.copy(alpha = 0.4f) else KomaMuted.copy(alpha = 0.55f)
                                )
                            )


                            // ── Perfil ────────────────────────────────────────
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                                label = {
                                    Text(
                                        "Perfil",
                                        fontSize = 10.sp,
                                        fontWeight = if (uiState.currentTab == MainTab.PROFILE) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = uiState.currentTab == MainTab.PROFILE,
                                onClick = { viewModel.onTabSelected(MainTab.PROFILE) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = KomaDeepBg,
                                    selectedTextColor = selectedColor,
                                    indicatorColor = KomaGold,
                                    unselectedIconColor = unselectedColor,
                                    unselectedTextColor = unselectedColor
                                )
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (uiState.currentTab) {
                        MainTab.HOME -> {
                            HomeScreen(
                                uiState = uiState,
                                isListening = isListening,
                                permissionStatus = permissionStatus,
                                onMicClick = {
                                    when (permissionStatus) {
                                        PermissionStatus.IDLE -> permissionManager.askForPermission()
                                        PermissionStatus.DENIED -> permissionManager.openSettings()
                                        PermissionStatus.GRANTED -> {
                                            if (isListening) voiceRecognizer.stopListening()
                                            else {
                                                viewModel.onQueryChange("")
                                                voiceRecognizer.startListening()
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
                                onClearSearch = { viewModel.clearSearch() },
                                onSearchTypeSelected = { showRestaurants -> viewModel.onSearchTypeSelected(showRestaurants) },
                                onDismissSearchTypeSheet = { viewModel.dismissSearchTypeSheet() }
                            )
                        }
                        MainTab.CART -> {
                            CartScreen(
                                cartItems = uiState.cartItems,
                                restaurantSelected = uiState.selectedRestaurant,
                                onRemoveItem = { product -> viewModel.removeFromCart(product) },
                                onCheckout = { viewModel.checkout() },
                                onGoToRestaurant = { restaurant ->
                                    viewModel.selectRestaurant(restaurant)
                                    viewModel.onTabSelected(MainTab.HOME)
                                },
                                cartAiMessage = uiState.cartAiMessage,
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
                                isMuted = isMuted
                            )
                        }

                        MainTab.ORDERS -> {
                            OrdersScreen(
                                orders = uiState.orderHistory,
                                isLoading = uiState.isLoading,
                                selectedOrder = uiState.selectedOrder,
                                onRefresh = { viewModel.refreshOrders() },
                                onOrderClick = { order -> viewModel.selectOrder(order) },
                                onBackToList = { viewModel.clearOrderSelection() },
                                onToggleFavorite = { order -> viewModel.toggleFavoriteOrder(order) },
                                isFiltered = uiState.isFilterEnabled,
                                onFilterToggle = { viewModel.toggleFilter() }
                            )
                        }

                         MainTab.FAVORITES -> {
                            FavoritesScreen(
                                orders = uiState.favoriteOrders,
                                selectedOrder = uiState.selectedOrder,
                                onOrderClick = { order -> viewModel.selectOrder(order) },
                                onToggleFavorite = { order -> viewModel.toggleFavoriteOrder(order) },
                                onBackToList = { viewModel.clearOrderSelection() }
                            )
                        }

                        MainTab.PROFILE -> {
                            ProfileScreen(
                                userProfile = uiState.userProfile,
                                onSave = { name, phone, addresses ->
                                    viewModel.updateUserProfile(name, phone, addresses)
                                    viewModel.onTabSelected(MainTab.HOME)
                                },
                                onGetLocation = { callbackUpdateAddress ->
                                    if (permissionStatus == PermissionStatus.GRANTED) {
                                        scope.launch {
                                            val addressFound = locationService.getCurrentAddress()
                                            callbackUpdateAddress(addressFound ?: "Localização não encontrada")
                                         }
                                    } else {
                                        permissionManager.askForPermission()
                                        callbackUpdateAddress("")
                                    }
                                },
                                onGetAddressFromMap = { lat, long ->
                                    locationService.getAddressFromCoordinates(lat, long)
                                }
                            )
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
    }
}