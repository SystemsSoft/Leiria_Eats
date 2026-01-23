package org.leria.eats.project.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.leria.eats.project.data.LeriaApiClient
import org.leria.eats.project.data.Order
import org.leria.eats.project.data.OrderItem
import org.leria.eats.project.data.OrderItemRequest
import org.leria.eats.project.data.OrderRequest
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.ProfileRepository
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.data.UserProfile

class SearchViewModel(
    private val apiClient: LeriaApiClient,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()


    init {
        // Carrega o perfil do usuário
        viewModelScope.launch {
            profileRepository.userProfileFlow.collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
        
        // Carrega todos os restaurantes na inicialização
        loadInitialRestaurants()

        // Inicia a observação automática de status dos pedidos
        startStatusPolling()
    }

    private fun startStatusPolling() {
        viewModelScope.launch {
            while (true) {
                val userId = _uiState.value.userProfile.id
                if (userId.isNotBlank() && _uiState.value.currentTab == MainTab.ORDERS) {
                    refreshOrdersInternal()
                }
                delay(10000) // Verifica a cada 10 segundos
            }
        }
    }

    private suspend fun refreshOrdersInternal() {
        val userId = _uiState.value.userProfile.id
        if (userId.isBlank()) return
        
        try {
            val updatedOrders = apiClient.getCustomerOrders(userId)
            _uiState.update {
                it.copy(orderHistory = updatedOrders)
            }
            
            // Se houver um pedido selecionado, atualiza ele também
            val selectedId = _uiState.value.selectedOrder?.id
            if (selectedId != null) {
                val updatedSelected = updatedOrders.find { it.id == selectedId }
                if (updatedSelected != null) {
                    _uiState.update { it.copy(selectedOrder = updatedSelected) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadInitialRestaurants() {
        _uiState.update { it.copy(textInput = "ver todos") }
        sendSearch()
    }

    fun updateUserProfile(name: String, phone: String, address: String) {
        viewModelScope.launch {
            // Gera um ID único se o usuário ainda não tiver um (ex: U-12345)
            val currentId = _uiState.value.userProfile.id
            val newId = if (currentId.isBlank()) {
                "U-${(10000..99999).random()}"
            } else {
                currentId
            }

            profileRepository.saveProfile(newId, name, phone, address)
            _uiState.update {
                it.copy(userProfile = UserProfile(newId, name, phone, address))
            }
        }
    }

    fun onQueryChange(text: String) {
        _uiState.update { it.copy(textInput = text) }
    }

    fun updateInputFromVoice(text: String) {
        if (text.isNotBlank()) {
            _uiState.update { it.copy(textInput = text) }
        }
    }

    fun selectRestaurant(restaurant: Restaurant) {
        _uiState.update { it.copy(selectedRestaurant = restaurant, selectedCategory = null) }
    }

    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedRestaurant = null, selectedCategory = null) }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                restaurants = emptyList(),
                aiReply = "O que lhe apetece hoje?",
                textInput = "",
                error = null
            )
        }
    }


    fun sendSearch() {
        val currentQuery = _uiState.value.textInput
        if (currentQuery.isBlank()) {
            clearSearch()
            return
        }

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
                e.printStackTrace()
            }
        }
    }

    fun addToCart(product: Product) {
        _uiState.update { currentState ->
            val restaurantId = currentState.cartRestaurantId ?: currentState.selectedRestaurant?.id

            currentState.copy(
                cartItems = currentState.cartItems + product,
                cartRestaurantId = restaurantId
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

    fun selectOrder(order: Order) {
        _uiState.update { it.copy(selectedOrder = order) }
    }

    fun clearOrderSelection() {
        _uiState.update { it.copy(selectedOrder = null) }
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

        if (currentUser.name.isBlank() || currentUser.address.isBlank()) {
            _uiState.update {
                it.copy(error = "Por favor, preencha seu Nome e Endereço na aba Perfil antes de finalizar.")
            }
            onTabSelected(MainTab.PROFILE)
            return
        }

        if (currentCart.isEmpty()) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val orderItems = currentCart.groupBy { it.id }.map { (id, products) ->
                    OrderItemRequest(
                        product_id = id,
                        quantity = products.size,
                        observation = null
                    )
                }

                val request = OrderRequest(
                    user_id = currentUser.id,
                    user_name = currentUser.name,
                    user_address = currentUser.address,
                    user_phone = currentUser.phone,
                    restaurant_id = restaurantId,
                    items = orderItems
                )

                val success = apiClient.sendOrder(request)

                if (success) {
                    // Mapeia o carrinho atual para a nova estrutura de OrderItem para o histórico
                    val orderItemsForHistory = currentCart.groupBy { it.name }.map { (name, products) ->
                        OrderItem(
                            product_name = name,
                            quantity = products.size,
                            observation = null
                        )
                    }


                    val newOrder = Order(
                        id = "#OK-${(100..999).random()}",
                        items = orderItemsForHistory,
                        total = _uiState.value.cartTotal,
                        status = "Enviado para o Restaurante",
                        date = "Hoje"
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            cartItems = emptyList(),
                            orderHistory = it.orderHistory + newOrder,
                            currentTab = MainTab.ORDERS,
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

    fun refreshOrders() {
        val userId = _uiState.value.userProfile.id
        if (userId.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            refreshOrdersInternal()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
