package org.leria.eats.project.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.leria.eats.project.data.*
import org.leria.eats.project.service.LocationService

class SearchViewModel(
    private val apiClient: LeriaApiClient,
    private val profileRepository: ProfileRepository,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val favoriteOrderIdsFlow = profileRepository.favoriteOrderIdsFlow


    init {
        viewModelScope.launch {
            profileRepository.userProfileFlow.collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
        viewModelScope.launch {
            profileRepository.userProfileFlow.first { it.id.isNotBlank() }
            refreshOrdersInternal()
        }
        loadInitialRestaurants()
        startStatusPolling()
        observeFavoriteOrders()
    }
    
    fun onLocationClicked() {
        viewModelScope.launch {
            val address = locationService.getCurrentAddress()
            if (!address.isNullOrBlank()) {
                val currentUserProfile = _uiState.value.userProfile
                val updatedProfile = currentUserProfile.copy(address = address)
                _uiState.update { it.copy(userProfile = updatedProfile) }
            }
        }
    }

     private fun observeFavoriteOrders() {
        viewModelScope.launch {
            favoriteOrderIdsFlow.collect { favoriteIds ->
                _uiState.update { currentState ->
                    val updatedOrders = currentState.orderHistory.map { order ->
                        order.copy(isFavorite = favoriteIds.contains(order.id))
                    }
                    currentState.copy(orderHistory = updatedOrders)
                }
            }
        }
    }

    fun toggleFavoriteOrder(order: Order) {
        viewModelScope.launch {
            val currentFavorites = favoriteOrderIdsFlow.first()
            val newFavorites = if (order.isFavorite) {
                currentFavorites - order.id
            } else {
                currentFavorites + order.id
            }
            profileRepository.saveFavoriteOrderIds(newFavorites)
        }
    }


    private fun startStatusPolling() {
        viewModelScope.launch {
            while (true) {
                val userId = _uiState.value.userProfile.id
                if (userId.isNotBlank() && _uiState.value.currentTab == MainTab.ORDERS) {
                    refreshOrdersInternal()
                }
                delay(10000)
            }
        }
    }

    private suspend fun refreshOrdersInternal() {
        val userId = _uiState.value.userProfile.id
        if (userId.isBlank()) return
        
        try {
            val updatedOrders = apiClient.getCustomerOrders(userId)
            val favoriteIds = favoriteOrderIdsFlow.first()
            val ordersWithFavorites = updatedOrders.map { order ->
                order.copy(isFavorite = favoriteIds.contains(order.id))
            }
            _uiState.update { it.copy(orderHistory = ordersWithFavorites) }
            
            val selectedId = _uiState.value.selectedOrder?.id
            if (selectedId != null) {
                val updatedSelected = ordersWithFavorites.find { it.id == selectedId }
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
            val currentId = _uiState.value.userProfile.id
            val newId = if (currentId.isBlank()) "U-${(10000..99999).random()}" else currentId
            profileRepository.saveProfile(newId, name, phone, address)
            _uiState.update { it.copy(userProfile = UserProfile(newId, name, phone, address)) }
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
            it.copy(restaurants = emptyList(), aiReply = "O que lhe apetece hoje?", textInput = "", error = null)
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
                    it.copy(isLoading = false, aiReply = response.reply, restaurants = response.results, textInput = "")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao conectar: ${e.message}") }
                e.printStackTrace()
            }
        }
    }

    fun addToCart(product: Product) {
        _uiState.update { currentState ->
            val restaurantId = currentState.cartRestaurantId ?: currentState.selectedRestaurant?.id
            currentState.copy(cartItems = currentState.cartItems + product, cartRestaurantId = restaurantId)
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
        _uiState.update { it.copy(cartItems = emptyList(), cartRestaurantId = null) }
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

    fun toggleFilter() {
        _uiState.update { it.copy(isFilterEnabled = !it.isFilterEnabled) }
    }

    fun checkout() {
        val currentState = _uiState.value
        val restaurantId = currentState.cartRestaurantId
    
        if (restaurantId == null) {
            _uiState.update { it.copy(error = "Erro: ID do restaurante não encontrado.") }
            return
        }
        val restaurant = uiState.value.restaurants.find { it.id == restaurantId }
        if (restaurant == null) {
            _uiState.update { it.copy(error = "Erro: Restaurante não encontrado.") }
            return
        }
        if (currentState.userProfile.name.isBlank() || currentState.userProfile.address.isBlank()) {
            _uiState.update { it.copy(error = "Por favor, preencha seu Nome e Endereço no Perfil.") }
            onTabSelected(MainTab.PROFILE)
            return
        }
        if (currentState.cartItems.isEmpty()) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val orderItems = currentState.cartItems.groupBy { it.id }.map { (id, products) ->
                OrderItemRequest(product_id = id, quantity = products.size, observation = null)
            }

            val request = OrderRequest(
                user_id = currentState.userProfile.id,
                user_name = currentState.userProfile.name,
                user_address = currentState.userProfile.address,
                user_phone = currentState.userProfile.phone,
                restaurant_id = restaurantId,
                restaurant_name = restaurant.name,
                items = orderItems
            )
            
            val sessionResponse = apiClient.initiateCheckout(request)

            if (sessionResponse == null) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao iniciar pagamento.") }
                return@launch
            }
            
            _uiState.update { 
                it.copy(isLoading = false, checkoutUrl = sessionResponse.url)
            }
        }
    }

    fun onPaymentResult(isSuccess: Boolean, orderId: String?) {
        if (isSuccess) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    checkoutUrl = null,
                    cartItems = emptyList(),
                    cartRestaurantId = null,
                    currentTab = MainTab.ORDERS,
                    error = null
                )
            }
            refreshOrders()
        } else {
            _uiState.update { it.copy(checkoutUrl = null, error = if (orderId == null) "Pagamento cancelado." else "Erro: ID do pedido não encontrado.") }
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
