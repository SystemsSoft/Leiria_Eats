package org.leria.eats.project.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.leria.eats.project.data.Address
import org.leria.eats.project.data.LeriaApiClient
import org.leria.eats.project.data.Order
import org.leria.eats.project.data.OrderItemRequest
import org.leria.eats.project.data.OrderRequest
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.ProfileRepository
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.SearchUiState

class SearchViewModel(
    private val apiClient: LeriaApiClient,
    private val profileRepository: ProfileRepository,
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

    fun updateUserProfile(name: String, phone: String, addresses: List<Address>) {
        viewModelScope.launch {
            val currentId = _uiState.value.userProfile.id
            val newId = if (currentId.isBlank()) "U-${(10000..99999).random()}" else currentId
            profileRepository.saveProfile(newId, name, phone, addresses)
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

    fun clearSelectionAndCart() {
        _uiState.update {
            it.copy(
                selectedRestaurant = null,
                selectedCategory = null,
                cartItems = emptyList(),
                cartRestaurantId = null
            )
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                restaurantResults = emptyList(),
                productResults = emptyList(),
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

                val hasBoth = response.restaurantResults.isNotEmpty() && response.productResults.isNotEmpty()
                val isProductOnly = response.restaurantResults.isEmpty() && response.productResults.isNotEmpty()

                if (hasBoth) {
                    // Guarda resultados pendentes e pede ao utilizador para escolher
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            aiReply = response.reply,
                            textInput = "",
                            pendingRestaurantResults = response.restaurantResults,
                            pendingProductResults = response.productResults,
                            showSearchTypeSheet = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            aiReply = response.reply,
                            restaurantResults = response.restaurantResults,
                            productResults = response.productResults,
                            textInput = ""
                        )
                    }
                    if (isProductOnly) {
                        response.productResults.forEach { product -> addToCart(product) }
                        onTabSelected(MainTab.CART)
                    }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao conectar: ${e.message}") }
                e.printStackTrace()
            }
        }
    }

    fun onSearchTypeSelected(showRestaurants: Boolean) {
        val state = _uiState.value
        if (showRestaurants) {
            _uiState.update {
                it.copy(
                    showSearchTypeSheet = false,
                    restaurantResults = state.pendingRestaurantResults,
                    productResults = emptyList(),
                    pendingRestaurantResults = emptyList(),
                    pendingProductResults = emptyList()
                )
            }
        } else {
            val products = state.pendingProductResults
            _uiState.update {
                it.copy(
                    showSearchTypeSheet = false,
                    restaurantResults = emptyList(),
                    productResults = products,
                    pendingRestaurantResults = emptyList(),
                    pendingProductResults = emptyList()
                )
            }
            products.forEach { product -> addToCart(product) }
            onTabSelected(MainTab.CART)
        }
    }

    fun dismissSearchTypeSheet() {
        _uiState.update {
            it.copy(
                showSearchTypeSheet = false,
                pendingRestaurantResults = emptyList(),
                pendingProductResults = emptyList()
            )
        }
    }

    fun addToCart(product: Product) {
        _uiState.update { currentState ->
            if (currentState.cartRestaurantId != null && currentState.cartRestaurantId !=  product.restaurant_id) {
                currentState.copy(cartError = "Não pode adicionar produtos de restaurantes diferentes.")
            } else {
                currentState.copy(
                    cartItems = currentState.cartItems + product,
                    cartRestaurantId =  product.restaurant_id,
                    cartMessage = "${product.name} adicionado à sacola."
                )
            }
        }
        fetchCompanyById(product.restaurant_id)
    }

    private fun fetchCompanyById(id: Int) {
        viewModelScope.launch {
            val company = apiClient.getCompanyById(id)
            _uiState.update {
                if (it.selectedRestaurant == null) {
                    it.copy(
                        selectedRestaurant = Restaurant(
                            id = company?.id ?: 0,
                            name = company?.name ?: "",
                            category = company?.category ?: "",
                            image_url = company?.imageUrl ?: "",
                            products = company?.products ?: listOf()
                        )
                    )
                } else {
                    it.copy()
                }
            }
        }
    }

    fun clearCartError() {
        _uiState.update { it.copy(cartError = null) }
    }

    fun clearCartMessage() {
        _uiState.update { it.copy(cartMessage = null) }
    }

    fun removeFromCart(product: Product) {
        _uiState.update { currentState ->
            val updatedCart = currentState.cartItems.toMutableList().apply { remove(product) }
            val newRestaurantId = if (updatedCart.isEmpty()) null else currentState.cartRestaurantId
            currentState.copy(cartItems = updatedCart, cartRestaurantId = newRestaurantId)
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
        if (currentState.userProfile.addresses.isEmpty()) {
            _uiState.update { it.copy(error = "Por favor, adicione um endereço no seu perfil antes de continuar.") }
            onTabSelected(MainTab.PROFILE)
            return
        }
        _uiState.update { it.copy(isAddressSheetVisible = true) }
    }

    fun dismissAddressSheet() {
        _uiState.update { it.copy(isAddressSheetVisible = false) }
    }

    fun confirmCheckout(selectedAddress: Address) {
        _uiState.update { it.copy(isAddressSheetVisible = false, isLoading = true, error = null) }

        val currentState = _uiState.value
        val restaurantId = currentState.cartRestaurantId

        if (restaurantId == null) {
            _uiState.update { it.copy(isLoading = false, error = "Erro: ID do restaurante não encontrado.") }
            return
        }
        val restaurant = uiState.value.restaurantResults.find { it.id == restaurantId }
        if (restaurant == null) {
            _uiState.update { it.copy(isLoading = false, error = "Erro: Restaurante não encontrado.") }
            return
        }
        if (currentState.userProfile.name.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Por favor, preencha seu Nome no Perfil.") }
            onTabSelected(MainTab.PROFILE)
            return
        }
        if (currentState.cartItems.isEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            val orderItems = currentState.cartItems.groupBy { it }.map { (product, products) ->
                OrderItemRequest(
                    product_id = product.id,
                    quantity = products.size,
                    observation = null,
                    product_name = product.name,
                    price = product.price,
                    image_url = product.image_url,
                    description = product.description
                )
            }

            val request = OrderRequest(
                user_id = currentState.userProfile.id,
                user_name = currentState.userProfile.name,
                user_address = selectedAddress.address,
                user_phone = currentState.userProfile.phone,
                restaurant_id = restaurant.id,
                restaurant_name = restaurant.name,
                restaurant_image_url = restaurant.image_url,
                restaurant_category = restaurant.category,
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