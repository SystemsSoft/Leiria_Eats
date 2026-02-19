package org.leria.eats.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.presentation.*
import org.leria.eats.project.presentation.components.WebView
import org.leria.eats.project.service.LocationService
import org.leria.eats.project.voice.VoiceRecognizer

@Composable
fun MainScreenWithAI(
    permissionManager: PermissionManager,
    viewModel: SearchViewModel = koinViewModel(),
    locationService: LocationService = koinInject()
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val voiceRecognizer = koinInject<VoiceRecognizer>()
    val voiceText by voiceRecognizer.results.collectAsState()
    val isListening by voiceRecognizer.isListening.collectAsState()
    val permissionStatus by permissionManager.status.collectAsState()

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
        // Note: system back handling for WebView should be implemented in Android-specific code if desired.
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
        // Overlay progress indicator while WebView is loading
        if (webViewLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        val selectedColor = MaterialTheme.colorScheme.primary
                        val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                            label = { Text("Início") },
                            selected = uiState.currentTab == MainTab.HOME,
                            onClick = { viewModel.onTabSelected(MainTab.HOME) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = selectedColor,
                                indicatorColor = selectedColor,
                                unselectedIconColor = unselectedColor,
                                unselectedTextColor = unselectedColor
                            )
                        )

                        NavigationBarItem(
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (uiState.cartCount > 0) {
                                            Badge(containerColor = selectedColor, contentColor = MaterialTheme.colorScheme.onPrimary) {
                                                Text(uiState.cartCount.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = "Sacola")
                                }
                            },
                            label = { Text("Sacola") },
                            selected = uiState.currentTab == MainTab.CART,
                            onClick = { viewModel.onTabSelected(MainTab.CART) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = selectedColor,
                                indicatorColor = selectedColor,
                                unselectedIconColor = unselectedColor,
                                unselectedTextColor = unselectedColor
                            )
                        )

                        NavigationBarItem(
                            icon = { Icon(Icons.Default.List, contentDescription = "Pedidos") },
                            label = { Text("Pedidos") },
                            selected = uiState.currentTab == MainTab.ORDERS,
                            onClick = { viewModel.onTabSelected(MainTab.ORDERS) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = selectedColor,
                                indicatorColor = selectedColor,
                                unselectedIconColor = unselectedColor,
                                unselectedTextColor = unselectedColor
                            )
                        )

                         NavigationBarItem(
                            icon = { Icon(Icons.Default.Star, contentDescription = "Favoritos") },
                            label = { Text("Favoritos") },
                            selected = uiState.currentTab == MainTab.FAVORITES,
                            onClick = { viewModel.onTabSelected(MainTab.FAVORITES) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = selectedColor,
                                indicatorColor = selectedColor,
                                unselectedIconColor = unselectedColor,
                                unselectedTextColor = unselectedColor
                            )
                        )

                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                            label = { Text("Perfil") },
                            selected = uiState.currentTab == MainTab.PROFILE,
                            onClick = { viewModel.onTabSelected(MainTab.PROFILE) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = selectedColor,
                                indicatorColor = selectedColor,
                                unselectedIconColor = unselectedColor,
                                unselectedTextColor = unselectedColor
                            )
                        )
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
                                onRestaurantClick = { restaurant -> viewModel.selectRestaurant(restaurant) },
                                onCategorySelect = { category -> viewModel.selectCategory(category) },
                                onClearSelection = { viewModel.clearSelection() },
                                onAddToCart = { product -> viewModel.addToCart(product) },
                                onRemoveFromCart = { product -> viewModel.removeFromCart(product) },
                                onViewCart = { viewModel.onTabSelected(MainTab.CART) },
                                onClearSearch = { viewModel.clearSearch() }
                            )
                        }
                        MainTab.CART -> {
                            CartScreen(
                                cartItems = uiState.cartItems,
                                onRemoveItem = { product -> viewModel.removeFromCart(product) },
                                onCheckout = { viewModel.checkout() }
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
    }
}