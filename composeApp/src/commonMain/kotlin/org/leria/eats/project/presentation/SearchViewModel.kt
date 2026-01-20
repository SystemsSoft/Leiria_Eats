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
import org.leria.eats.project.data.OrderItemRequest
import org.leria.eats.project.data.OrderRequest
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.data.UserProfile

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
                val response = apiClient.searchRestaurants(currentQuery)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        aiReply = response.reply,
                        restaurants = response.results,
                        textInput = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erro ao conectar: ${e.message}"
                    )
                }
                e.printStackTrace() // Ajuda a ver o erro no Logcat
            }
        }
    }

    fun addToCart(product: Product) {
        _uiState.update { currentState ->
            val restaurantId = currentState.cartRestaurantId ?: currentState.selectedRestaurant?.id

            currentState.copy(
                cartItems = currentState.cartItems + product,
                cartRestaurantId = restaurantId // <--- SALVAMOS AQUI
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
        _uiState.update {
            it.copy(
                cartItems = emptyList(),
                cartRestaurantId = null
            )
        }
    }

    fun onTabSelected(tab: MainTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }


    fun checkout() {
        val currentUser = _uiState.value.userProfile
        val currentCart = _uiState.value.cartItems


        val restaurantId = _uiState.value.cartRestaurantId

        if (restaurantId == null) {
            _uiState.update {
                it.copy(error = "Erro: Não foi possível identificar o restaurante do pedido.")
            }
            return
        }

        // --- 2. VALIDAÇÃO DE PERFIL ---
        if (currentUser.name.isBlank() || currentUser.address.isBlank()) {
            _uiState.update {
                it.copy(error = "Por favor, preencha seu Nome e Endereço na aba Perfil antes de finalizar.")
            }
            // Redireciona para a aba de perfil para o usuário preencher
            onTabSelected(MainTab.PROFILE)
            return
        }

        // --- 3. VALIDAÇÃO DE CARRINHO VAZIO ---
        if (currentCart.isEmpty()) return

        // Inicia carregamento
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // --- 4. PREPARAR OS DADOS ---
                // Agrupa itens iguais (ex: 2x Pizza de Calabresa)
                val orderItems = currentCart.groupBy { it.id }.map { (id, products) ->
                    OrderItemRequest(
                        product_id = id,
                        quantity = products.size,
                        observation = null
                    )
                }

                val request = OrderRequest(
                    user_name = currentUser.name,
                    user_address = currentUser.address,
                    user_phone = currentUser.phone,
                    restaurant_id = restaurantId, // <--- CAMPO NOVO IMPORTANTE
                    items = orderItems
                )

                // --- 5. ENVIAR AO SERVIDOR ---
                val success = apiClient.sendOrder(request)

                if (success) {
                    // Sucesso: Cria um objeto de pedido local para mostrar no histórico
                    val newOrder = Order(
                        id = "#OK-${(100..999).random()}", // ID temporário visual
                        items = currentCart,
                        total = _uiState.value.cartTotal,
                        status = "Enviado para o Restaurante"
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            cartItems = emptyList(), // Limpa a sacola
                            orderHistory = it.orderHistory + newOrder, // Adiciona ao histórico
                            currentTab = MainTab.ORDERS, // Leva para a tela de pedidos
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Falha ao enviar pedido. Verifique sua internet.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(isLoading = false, error = "Erro interno ao processar pedido: ${e.message}")
                }
            }
        }
    }


    fun updateUserProfile(name: String, phone: String, address: String) {
        _uiState.update { currentState ->
            currentState.copy(
                userProfile = UserProfile(
                    name = name,
                    phone = phone,
                    address = address
                )
            )
        }
    }

    fun refreshOrders() {
        val userName = _uiState.value.userProfile.name
        if (userName.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val updatedOrders = apiClient.getCustomerOrders(userName)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    orderHistory = updatedOrders // Substitui a lista local pela do servidor
                )
            }
        }
    }

}