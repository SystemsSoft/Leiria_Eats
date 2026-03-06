package org.leria.eats.project.presentation

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
    val aiReply: String = "Olá! O que vamos comer hoje?",
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
    val isProcessingAutoPayment: Boolean = false,
    val autoPaymentOrderId: Int? = null,
    val autoPaymentIntentId: String? = null,
    val pendingSavePaymentMethod: Boolean = false
) {
    val cartCount: Int get() = cartItems.size
    val favoriteOrders: List<Order> get() = orderHistory.filter { it.isFavorite }

}