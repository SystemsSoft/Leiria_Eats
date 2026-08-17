package org.leria.eats.project.presentation

import kotlinx.serialization.Serializable
import org.leria.eats.project.data.Address
import org.leria.eats.project.data.Order
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.data.UserProfile

enum class MainTab {
    HOME,
    AI,
    CART,
    ORDERS,
    PROFILE,
    FAVORITES
}

@Serializable
enum class ChatMessageType {
    USER,
    AI
}

@Serializable
data class ChatMessage(
    val id: String,
    val type: ChatMessageType,
    val text: String,
    val restaurants: List<Restaurant> = emptyList(),
    val products: List<Product> = emptyList()
)

data class SearchUiState(
    val isLoading: Boolean = false,
    val textInput: String = "",
    val aiReply: String = "",
    val restaurantResults: List<Restaurant> = emptyList(),
    val productResults: List<Product> = emptyList(),
    // Lista exclusiva do Home — carregada uma vez e nunca sobrescrita por pesquisas da IA
    val allRestaurants: List<Restaurant> = emptyList(),
    val error: String? = null,
    val selectedRestaurant: Restaurant? = null,
    var restaurant: Restaurant? = null,
    val selectedCategory: String? = null,
    val selectedOrder: Order? = null,
    val cartRestaurantId: Int? = null,
    val cartItems: List<Product> = emptyList(),
    val cartRestaurants: List<Restaurant> = emptyList(), // Metadados dos restaurantes no carrinho
    val currentTab: MainTab = MainTab.AI,
    val orderHistory: List<Order> = emptyList(),
    val userProfile: UserProfile = UserProfile(),
    val isFilterEnabled: Boolean = false,
    val checkoutUrl: String? = null,
    val isAddressSheetVisible: Boolean = false,
    val cartError: String? = null,
    val cartMessage: String? = null,
    val cartAiMessage: String? = null,
    val cartAiMessageSpoken: Boolean = false, // Track if AI message has been spoken
    val isAiCartFlow: Boolean = false, // Indica se o carrinho atual veio de um fluxo da IA
    val lastSearchQuery: String = "",
    val isSuggestionMode: Boolean = false,
    val showSearchTypeSheet: Boolean = false,
    val pendingRestaurantResults: List<Restaurant> = emptyList(),
    val pendingProductResults: List<Product> = emptyList(),
    val showSavePaymentSheet: Boolean = false,
    val showPaymentConfirmSheet: Boolean = false,
    val showDeliveryTypeSheet: Boolean = false,
    val pendingDeliveryType: String = "delivery",
    val pendingCheckoutSavePaymentMethod: Boolean = false,
    val selectedAddressForCheckout: Address? = null,
    val pendingDeliveryFee: Double = 0.0,
    val pendingServiceFee: Double = 0.0,
    val isProcessingAutoPayment: Boolean = false,
    val autoPaymentOrderId: Int? = null,
    val autoPaymentIntentId: String? = null,
    val pendingSavePaymentMethod: Boolean = false,
    val pendingProfileNavigation: Boolean = false, // Flag to navigate to profile after TTS finishes
    val orderJustPlaced: Boolean = false, // Flag to trigger voice feedback when order is placed
    val favoriteOrderNicknames: Map<String, String> = emptyMap(),
    val orderSearchQueries: Map<String, String> = emptyMap(),
    // key = "orderId::productName", value = 1..5
    val orderItemRatings: Map<String, Int> = emptyMap(),
    // key = "orderId::productName", value = productId
    val orderProductIds: Map<String, Int> = emptyMap(),
    // key = orderId, value = restaurantId
    val orderRestaurantIds: Map<String, Int> = emptyMap(),
    // Lista de mensagens de chat da IA
    val chatMessages: List<ChatMessage> = emptyList()
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