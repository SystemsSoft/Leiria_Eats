package org.leria.eats.project.presentation

import org.leria.eats.project.data.Order
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant

enum class MainTab {
    HOME,
    CART,
    ORDERS
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val textInput: String = "",
    val aiReply: String = "Olá! O que vamos comer hoje?",
    val restaurants: List<Restaurant> = emptyList(),
    val error: String? = null,
    val selectedRestaurant: Restaurant? = null,
    val cartItems: List<Product> = emptyList(),
    val currentTab: MainTab = MainTab.HOME,

    val orderHistory: List<Order> = emptyList()
) {
    val cartTotal: Double get() = cartItems.sumOf { it.price }
    val cartCount: Int get() = cartItems.size
}