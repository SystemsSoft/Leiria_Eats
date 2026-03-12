package org.leria.eats.project.presentation

import org.leria.eats.project.data.Address
import org.leria.eats.project.data.Order
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.data.UserProfile

enum class MainTab {
    HOME,
    CART,
    ORDERS,
    PROFILE,
    FAVORITES
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val textInput: String = "",
    val aiReply: String = "Olá! O que deseja comer hoje?",
    val restaurantResults: List<Restaurant> = emptyList(),
    val productResults: List<Product> = emptyList(),
    val error: String? = null,
    val selectedRestaurant: Restaurant? = null,
    var restaurant: Restaurant? = null,
    val selectedCategory: String? = null,
    val selectedOrder: Order? = null,
    val cartRestaurantId: Int? = null,
    val cartItems: List<Product> = emptyList(),
    val currentTab: MainTab = MainTab.HOME,
    val orderHistory: List<Order> = emptyList(),
    val userProfile: UserProfile = UserProfile(),
    val isFilterEnabled: Boolean = false,
    val checkoutUrl: String? = null,
    val isAddressSheetVisible: Boolean = false,
    val cartError: String? = null,
    val cartMessage: String? = null,
    val cartAiMessage: String? = null,
    val cartAiMessageSpoken: Boolean = false, // Track if AI message has been spoken
    val lastSearchQuery: String = "",
    val isSuggestionMode: Boolean = false,
    val showSearchTypeSheet: Boolean = false,
    val pendingRestaurantResults: List<Restaurant> = emptyList(),
    val pendingProductResults: List<Product> = emptyList(),
    val showSavePaymentSheet: Boolean = false,
    val showPaymentConfirmSheet: Boolean = false,
    val selectedAddressForCheckout: Address? = null,
    val isProcessingAutoPayment: Boolean = false,
    val autoPaymentOrderId: Int? = null,
    val autoPaymentIntentId: String? = null,
    val pendingSavePaymentMethod: Boolean = false,
    val pendingProfileNavigation: Boolean = false, // Flag to navigate to profile after TTS finishes
    val orderJustPlaced: Boolean = false, // Flag to trigger voice feedback when order is placed
    val favoriteOrderNicknames: Map<String, String> = emptyMap(),
    val orderSearchQueries: Map<String, String> = emptyMap()
) {
    val cartCount: Int get() = cartItems.size
    val favoriteOrders: List<Order> get() = orderHistory
        .filter { it.isFavorite }
        .map { order ->
            val nick = favoriteOrderNicknames[order.id]
            val query = orderSearchQueries[order.id]
            order.copy(
                nickname = if (!nick.isNullOrBlank()) nick else order.nickname,
                searchQuery = if (!query.isNullOrBlank()) query else order.searchQuery
            )
        }
}