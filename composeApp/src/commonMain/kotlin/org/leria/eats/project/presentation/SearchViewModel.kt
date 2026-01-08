package org.leria.eats.project.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.leria.eats.project.data.LeriaApiClient
import org.leria.eats.project.data.Order
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant

class SearchViewModel(private val apiClient: LeriaApiClient) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChange(text: String) {
        _uiState.update { it.copy(textInput = text) }
    }

    fun updateInputFromVoice(text: String) {
        if (text.isNotBlank()) {
            _uiState.update { it.copy(textInput = text) }
        }
    }

    fun selectRestaurant(restaurant: Restaurant) {
        _uiState.update { it.copy(selectedRestaurant = restaurant) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedRestaurant = null) }
    }

    fun sendSearch() {
        val currentQuery = _uiState.value.textInput
        if (currentQuery.isBlank()) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val response = apiClient.sendChat(currentQuery)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        aiReply = response.reply,
                        restaurants = response.results,
                        textInput = "" // Limpa o campo após enviar (opcional)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erro ao conectar: ${e.message}"
                    )
                }
            }
        }
    }

    fun addToCart(product: Product) {
        _uiState.update { currentState ->
            currentState.copy(
                cartItems = currentState.cartItems + product
            )
        }
    }

    fun removeFromCart(product: Product) {
        _uiState.update { currentState ->
            val currentList = currentState.cartItems.toMutableList()
            currentList.remove(product)
            currentState.copy(cartItems = currentList)
        }
    }


    fun clearCart() {
        _uiState.update { it.copy(cartItems = emptyList()) }
    }

    fun onTabSelected(tab: MainTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }


    fun checkout() {
        _uiState.update { currentState ->
            if (currentState.cartItems.isEmpty()) return@update currentState

            val newOrder = Order(
                id = "#${(1000..9999).random()}", // Gera um ID simples
                items = currentState.cartItems,
                total = currentState.cartTotal,
                status = "Em preparo"
            )

            currentState.copy(
                orderHistory = currentState.orderHistory + newOrder,
                cartItems = emptyList(), // Esvazia a sacola
                currentTab = MainTab.ORDERS // Leva para a tela de pedidos
            )
        }
    }

}