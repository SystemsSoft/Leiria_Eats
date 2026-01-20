package org.leria.eats.project

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.presentation.CartScreen
import org.leria.eats.project.presentation.HomeScreen
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.OrdersScreen // <--- Nova tela
import org.leria.eats.project.presentation.ProfileScreen
import org.leria.eats.project.presentation.RestaurantDetailScreen
import org.leria.eats.project.presentation.SearchViewModel
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


    if (uiState.selectedRestaurant != null) {
        RestaurantDetailScreen(
            restaurant = uiState.selectedRestaurant!!,
            cartItems = uiState.cartItems,
            onBack = { viewModel.clearSelection() },
            onAdd = { product -> viewModel.addToCart(product) },
            onRemove = { product -> viewModel.removeFromCart(product) },
            onViewCart = {
                viewModel.onTabSelected(MainTab.CART)
            }
        )
    }
    else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF16213E),
                    contentColor = Color.White
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                        label = { Text("Início") },
                        selected = uiState.currentTab == MainTab.HOME,
                        onClick = { viewModel.onTabSelected(MainTab.HOME) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = Color(0xFFE94560),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )

                    NavigationBarItem(
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (uiState.cartCount > 0) {
                                        Badge { Text(uiState.cartCount.toString()) }
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
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = Color(0xFFE94560),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Pedidos") },
                        label = { Text("Pedidos") },
                        selected = uiState.currentTab == MainTab.ORDERS,
                        onClick = { viewModel.onTabSelected(MainTab.ORDERS) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = Color(0xFFE94560),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                        label = { Text("Perfil") },
                        selected = uiState.currentTab == MainTab.PROFILE,
                        onClick = { viewModel.onTabSelected(MainTab.PROFILE) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = Color(0xFFE94560),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
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
                            onRestaurantClick = { restaurant -> viewModel.selectRestaurant(restaurant) }
                        )
                    }
                    MainTab.CART -> {
                        CartScreen(
                            cartItems = uiState.cartItems,
                            onRemoveItem = { product -> viewModel.removeFromCart(product) },
                            // Aqui conectamos a ação de finalizar o pedido
                            onCheckout = { viewModel.checkout() }
                        )
                    }

                    MainTab.ORDERS -> {
                        OrdersScreen(
                            orders = uiState.orderHistory,
                            onRefresh = { viewModel.refreshOrders() }
                        )
                    }

                    MainTab.PROFILE -> {
                        ProfileScreen(
                            userProfile = uiState.userProfile,
                            onSave = { name, phone, address ->
                                viewModel.updateUserProfile(name, phone, address)
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
                            }
                        )
                    }
                }
            }
        }
    }
}