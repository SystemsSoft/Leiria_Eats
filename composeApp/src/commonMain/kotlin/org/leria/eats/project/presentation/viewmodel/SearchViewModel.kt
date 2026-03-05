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

    private fun buildGreeting(name: String): String {
        val firstName = name.trim().split(" ").firstOrNull { it.isNotBlank() }
        return if (!firstName.isNullOrBlank())
            "Olá ${firstName.replaceFirstChar { it.uppercaseChar() }}! O que vamos comer hoje?"
        else
            "Olá! O que vamos comer hoje?"
    }

    private val favoriteOrderIdsFlow = profileRepository.favoriteOrderIdsFlow


    init {
        viewModelScope.launch {
            profileRepository.userProfileFlow.collect { profile ->
                val greeting = buildGreeting(profile.name)
                _uiState.update { it.copy(userProfile = profile, aiReply = greeting) }
            }
        }
        viewModelScope.launch {
            profileRepository.userProfileFlow.first { it.id.isNotBlank() }
            refreshOrdersInternal()
        }
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
        // sem pesquisa automática no arranque
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

    fun selectRestaurantOrAddToCart(restaurant: Restaurant) {
        if (_uiState.value.isSuggestionMode) {
            // Modo sugestão: buscar produtos e adicionar à sacola
            _uiState.update { it.copy(isLoading = true, isSuggestionMode = false) }
            viewModelScope.launch {
                val company = apiClient.getCompanyById(restaurant.id)
                val resolvedRestaurant = if (company != null) Restaurant(
                    id = company.id,
                    name = company.name,
                    category = company.category,
                    image_url = company.imageUrl,
                    products = company.products
                ) else restaurant
                val products = resolvedRestaurant.products
                if (products.isNotEmpty()) {
                    // Adiciona à sacola directamente sem chamar fetchCompanyById de novo
                    _uiState.update { currentState ->
                        currentState.copy(
                            cartItems = currentState.cartItems + products,
                            cartRestaurantId = resolvedRestaurant.id,
                            selectedRestaurant = resolvedRestaurant
                        )
                    }
                    val productNames = products.take(3).joinToString(", ") { it.name }
                    val aiMsg = buildString {
                        append("✅ Adicionei à sua sacola: $productNames")
                        if (products.size > 3) append(" e mais ${products.size - 3} itens")
                        append(".\n\n💡 Quer que sugira outro restaurante com pratos semelhantes, ou gostaria de adicionar mais alguma coisa deste restaurante?")
                    }
                    _uiState.update { it.copy(isLoading = false, cartAiMessage = aiMsg) }
                } else {
                    _uiState.update { it.copy(isLoading = false, selectedRestaurant = resolvedRestaurant) }
                }
                onTabSelected(MainTab.CART)
            }
        } else {
            selectRestaurant(restaurant)
        }
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
        val greeting = buildGreeting(_uiState.value.userProfile.name)
        _uiState.update {
            it.copy(
                restaurantResults = emptyList(),
                productResults = emptyList(),
                aiReply = greeting,
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
        _uiState.update { it.copy(isLoading = true, error = null, isSuggestionMode = false) }
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
                            lastSearchQuery = currentQuery,
                            pendingRestaurantResults = response.restaurantResults,
                            pendingProductResults = response.productResults,
                            showSearchTypeSheet = true
                        )
                    }
                } else {
                    val resolvedReply = if (currentQuery.trim().equals("ver todos", ignoreCase = true))
                        "Todos os restaurantes disponíveis"
                    else
                        response.reply
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            aiReply = resolvedReply,
                            restaurantResults = response.restaurantResults,
                            productResults = response.productResults,
                            textInput = "",
                            lastSearchQuery = currentQuery
                        )
                    }
                    if (isProductOnly) {
                        response.productResults.forEach { product -> addToCart(product) }
                        val productNames = response.productResults
                            .take(3)
                            .joinToString(", ") { it.name }
                        val aiMsg = buildString {
                            append("✅ Adicionei à sua sacola: $productNames")
                            if (response.productResults.size > 3)
                                append(" e mais ${response.productResults.size - 3} itens")
                            append(".\n\n💡 Quer que sugira outro restaurante com pratos semelhantes, ou gostaria de adicionar mais alguma coisa deste restaurante?")
                        }
                        _uiState.update { it.copy(cartAiMessage = aiMsg) }
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
            val productNames = products.take(3).joinToString(", ") { it.name }
            val aiMsg = buildString {
                append("✅ Adicionei à sua sacola: $productNames")
                if (products.size > 3) append(" e mais ${products.size - 3} itens")
                append(".\n\n")
                append("💡 Quer que sugira outro restaurante com pratos semelhantes, ou gostaria de adicionar mais alguma coisa deste restaurante?")
            }
            _uiState.update { it.copy(cartAiMessage = aiMsg) }
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
            if (currentState.cartRestaurantId != null && currentState.cartRestaurantId != product.restaurant_id) {
                currentState.copy(cartError = "Não pode adicionar produtos de restaurantes diferentes.")
            } else {
                val existing = currentState.cartItems.find { it.id == product.id }
                val updatedCart = if (existing != null) {
                    currentState.cartItems.map {
                        if (it.id == product.id) it.copy(quantity = it.quantity + product.quantity)
                        else it
                    }
                } else {
                    currentState.cartItems + product
                }
                currentState.copy(
                    cartItems = updatedCart,
                    cartRestaurantId = product.restaurant_id,
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

    fun clearCartAiMessage() {
        _uiState.update { it.copy(cartAiMessage = null) }
    }

    fun suggestAnotherRestaurant() {
        val lastQuery = _uiState.value.lastSearchQuery.trim()
        val suggestionQuery = if (lastQuery.isNotBlank()) "sugestão $lastQuery" else "sugestão"
        // Clear restaurant detail, cart and AI message, go HOME immediately
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                cartAiMessage = null,
                cartItems = emptyList(),
                cartRestaurantId = null,
                selectedRestaurant = null,
                selectedCategory = null,
                isSuggestionMode = true,
                currentTab = MainTab.HOME
            )
        }
        viewModelScope.launch {
            try {
                val response = apiClient.searchRestaurants(suggestionQuery)
                val label = if (lastQuery.isNotBlank()) lastQuery else "restaurantes"
                // Show results on HomeScreen – never auto-add to cart
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        aiReply = "Outras opções que também pode gostar, além de $label",
                        restaurantResults = response.restaurantResults,
                        productResults = response.productResults,
                        textInput = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao conectar: ${e.message}") }
                e.printStackTrace()
            }
        }
    }

    fun removeFromCart(product: Product) {
        _uiState.update { currentState ->
            val updatedCart = currentState.cartItems
                .map { if (it.id == product.id) it.copy(quantity = it.quantity - 1) else it }
                .filter { it.quantity > 0 }
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

        // Check if there's a default address
        val defaultAddress = currentState.userProfile.addresses.find { it.isDefault }

        // Check if user has saved payment methods
        val hasSavedPaymentMethods = currentState.userProfile.savedPaymentMethods.isNotEmpty()

        if (defaultAddress != null) {
            if (hasSavedPaymentMethods) {
                // User has saved payment methods, proceed directly with auto-payment
                // Backend will handle charging the saved card
                confirmCheckout(defaultAddress, savePaymentMethod = true)
            } else {
                // User doesn't have saved payment methods, show sheet to ask
                _uiState.update { it.copy(showSavePaymentSheet = true) }
            }
        } else {
            // Show address selection sheet if no default
            _uiState.update { it.copy(isAddressSheetVisible = true) }
        }
    }

    fun dismissSavePaymentSheet() {
        _uiState.update { it.copy(showSavePaymentSheet = false) }
    }

    fun proceedToCheckout(savePaymentMethod: Boolean) {
        _uiState.update { it.copy(showSavePaymentSheet = false) }

        val currentState = _uiState.value
        val defaultAddress = currentState.userProfile.addresses.find { it.isDefault }

        if (defaultAddress != null) {
            confirmCheckout(defaultAddress, savePaymentMethod)
        }
    }

    fun dismissAddressSheet() {
        _uiState.update { it.copy(isAddressSheetVisible = false) }
    }

    fun confirmCheckout(selectedAddress: Address, savePaymentMethod: Boolean = false) {
        _uiState.update {
            it.copy(
                isAddressSheetVisible = false,
                isLoading = true,
                error = null,
                pendingSavePaymentMethod = savePaymentMethod
            )
        }

        val currentState = _uiState.value
        val restaurantId = currentState.cartRestaurantId

        if (restaurantId == null) {
            _uiState.update { it.copy(isLoading = false, error = "Erro: ID do restaurante não encontrado.") }
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
            // Procura o restaurante nos resultados ou no selectedRestaurant
            var restaurant = uiState.value.restaurantResults.find { it.id == restaurantId }
                ?: currentState.selectedRestaurant?.takeIf { it.id == restaurantId }

            if (restaurant == null) {
                // Último recurso: ir buscar à API
                val company = apiClient.getCompanyById(restaurantId)
                if (company != null) {
                    restaurant = Restaurant(
                        id = company.id,
                        name = company.name,
                        category = company.category,
                        image_url = company.imageUrl,
                        products = company.products
                    )
                    _uiState.update { it.copy(selectedRestaurant = restaurant) }
                }
            }

            if (restaurant == null) {
                _uiState.update { it.copy(isLoading = false, error = "Erro: Restaurante não encontrado.") }
                return@launch
            }

            val orderItems = currentState.cartItems.map { product ->
                OrderItemRequest(
                    product_id = product.id,
                    quantity = product.quantity,
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
                items = orderItems,
                save_payment_method = savePaymentMethod
            )

            val sessionResponse = apiClient.initiateCheckout(request)

            if (sessionResponse == null) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao iniciar pagamento.") }
                return@launch
            }

            // Handle auto_paid scenario (payment method already saved)
            if (sessionResponse.auto_paid) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isProcessingAutoPayment = true,
                        autoPaymentOrderId = sessionResponse.order_id,
                        autoPaymentIntentId = sessionResponse.payment_intent_id
                    )
                }
                // Start polling for payment confirmation
                startPaymentPolling(currentState.userProfile.id, sessionResponse.order_id)
            } else {
                // Traditional checkout flow - redirect to Stripe Checkout URL
                _uiState.update {
                    it.copy(isLoading = false, checkoutUrl = sessionResponse.url)
                }
            }
        }
    }

    fun onPaymentResult(isSuccess: Boolean, orderId: String?) {
        if (isSuccess) {
            val shouldFetchMethods = _uiState.value.pendingSavePaymentMethod
            val userId = _uiState.value.userProfile.id

            _uiState.update {
                it.copy(
                    isLoading = false,
                    checkoutUrl = null,
                    cartItems = emptyList(),
                    cartRestaurantId = null,
                    currentTab = MainTab.ORDERS,
                    error = null,
                    pendingSavePaymentMethod = false
                )
            }

            // Fetch saved payment methods if user chose to save
            if (shouldFetchMethods && userId.isNotBlank()) {
                viewModelScope.launch {
                    fetchSavedPaymentMethods(userId)
                }
            }

            refreshOrders()
        } else {
            _uiState.update {
                it.copy(
                    checkoutUrl = null,
                    pendingSavePaymentMethod = false,
                    error = if (orderId == null) "Pagamento cancelado." else "Erro: ID do pedido não encontrado."
                )
            }
        }
    }

    private fun startPaymentPolling(userId: String, orderId: Int?) {
        if (orderId == null) {
            _uiState.update {
                it.copy(
                    isProcessingAutoPayment = false,
                    error = "Erro: ID do pedido não encontrado."
                )
            }
            return
        }

        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 30 // 30 seconds max (30 * 1 second)
            var paymentConfirmed = false

            while (attempts < maxAttempts && !paymentConfirmed) {
                delay(1000) // Wait 1 second between checks
                attempts++

                try {
                    // Fetch latest orders to check if payment was confirmed
                    val updatedOrders = apiClient.getCustomerOrders(userId)
                    val order = updatedOrders.find { it.id == orderId.toString() }

                    if (order != null) {
                        val successStatuses = listOf("Em Preparo", "Confirmado", "Pago", "Saiu para Entrega", "Entregue")
                        val failureStatuses = listOf("Falhou", "Cancelado", "Recusado")

                        when {
                            order.status in successStatuses -> {
                                // Payment confirmed!
                                paymentConfirmed = true

                                // Fetch saved payment methods if user chose to save
                                val shouldFetchMethods = _uiState.value.pendingSavePaymentMethod
                                if (shouldFetchMethods) {
                                    fetchSavedPaymentMethods(userId)
                                }

                                _uiState.update {
                                    it.copy(
                                        isProcessingAutoPayment = false,
                                        autoPaymentOrderId = null,
                                        autoPaymentIntentId = null,
                                        cartItems = emptyList(),
                                        cartRestaurantId = null,
                                        currentTab = MainTab.ORDERS,
                                        error = null,
                                        pendingSavePaymentMethod = false
                                    )
                                }
                                refreshOrders()
                                return@launch
                            }
                            order.status in failureStatuses -> {
                                // Payment failed!
                                paymentConfirmed = true // Stop polling
                                _uiState.update {
                                    it.copy(
                                        isProcessingAutoPayment = false,
                                        autoPaymentOrderId = null,
                                        autoPaymentIntentId = null,
                                        error = "Pagamento falhou. Por favor, tente novamente ou use outro cartão."
                                    )
                                }
                                return@launch
                            }
                            // Otherwise status is still "Pendente", continue polling
                        }
                    }
                } catch (e: Exception) {
                    println("Erro ao verificar status do pagamento: ${e.message}")
                }
            }

            // Timeout reached
            if (!paymentConfirmed) {
                _uiState.update {
                    it.copy(
                        isProcessingAutoPayment = false,
                        autoPaymentOrderId = null,
                        autoPaymentIntentId = null,
                        error = "O pagamento está demorando mais do que o esperado. Verifique seus pedidos."
                    )
                }
            }
        }
    }

    fun cancelAutoPayment() {
        _uiState.update {
            it.copy(
                isProcessingAutoPayment = false,
                autoPaymentOrderId = null,
                autoPaymentIntentId = null
            )
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

    private suspend fun fetchSavedPaymentMethods(userId: String) {
        try {
            val response = apiClient.getSavedPaymentMethods(userId)
            if (response != null && response.hasSavedMethods) {
                // Save to local repository
                profileRepository.savePaymentMethods(response.methods)
                println("✅ Métodos de pagamento salvos: ${response.methods.size}")
            } else {
                println("ℹ️ Nenhum método de pagamento encontrado")
            }
        } catch (e: Exception) {
            println("⚠️ Erro ao buscar métodos de pagamento: ${e.message}")
            // Don't block the flow, just log the error
        }
    }
}