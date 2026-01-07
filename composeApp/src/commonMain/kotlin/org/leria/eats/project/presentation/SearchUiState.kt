package org.leria.eats.project.presentation
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.data.Product

data class SearchUiState(
    val isLoading: Boolean = false,
    val textInput: String = "",
    val aiReply: String = "Olá! O que vamos comer hoje?",
    val restaurants: List<Restaurant> = emptyList(),
    val error: String? = null,
    val selectedRestaurant: Restaurant? = null,

    // --- NOVO: O CARRINHO ---
    // Guardamos a lista simples. Se tiver 2 pizzas, ela aparece 2 vezes na lista.
    val cartItems: List<Product> = emptyList()
) {
    // Helper para calcular o total rapidinho na UI
    val cartTotal: Double
        get() = cartItems.sumOf { it.price }

    val cartCount: Int
        get() = cartItems.size
}