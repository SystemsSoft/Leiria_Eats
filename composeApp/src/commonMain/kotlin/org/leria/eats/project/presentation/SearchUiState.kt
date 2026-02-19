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
    val cartError: String? = null
) {
    val cartTotal: Double get() = cartItems.sumOf { it.price }
    val cartCount: Int get() = cartItems.size
    val favoriteOrders: List<Order> get() = orderHistory.filter { it.isFavorite }


    // Filtra os produtos do restaurante selecionado com base na categoria
    val filteredProducts: List<Product>
        get() = selectedRestaurant?.products?.filter {
            selectedCategory == null || it.category == selectedCategory
        } ?: emptyList()

    // Obtém as categorias únicas do restaurante selecionado
    val categories: List<String>
        get() = selectedRestaurant?.products?.map { it.category }?.distinct()?.sorted() ?: emptyList()
}