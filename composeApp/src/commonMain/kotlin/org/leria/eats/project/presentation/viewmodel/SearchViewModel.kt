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
import org.leria.eats.project.data.RatingItemRequest
import org.leria.eats.project.data.RatingRequest
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.SearchUiState

class SearchViewModel(
    private val apiClient: LeriaApiClient,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    /** Gera um código de pedido numérico único com 9 dígitos */
    private fun generateTrackingCode(): String {
        return (100000000..999999999).random().toString()
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private fun buildGreeting(name: String): String {
        val firstName = name.trim().split(" ").firstOrNull { it.isNotBlank() }
        return if (!firstName.isNullOrBlank())
            "Olá ${firstName.replaceFirstChar { it.uppercaseChar() }}! O que deseja comer hoje?"
        else
            "Olá! O que deseja comer hoje?"
    }

    private fun buildWelcomeMessage(): String {
        return "Olá, seja bem-vindo! Sou o seu assistente pessoal que decide os melhores pratos para si. Vou levá-lo ao seu perfil para o conhecer melhor e começarmos."
    }

    private val favoriteOrderIdsFlow = profileRepository.favoriteOrderIdsFlow

    private var initialRestaurantsLoaded = false

    init {
        viewModelScope.launch {
            profileRepository.userProfileFlow.collect { profile ->
                // Check if user is registered (has name and at least one address)
                val isUserRegistered = profile.name.isNotBlank() && profile.addresses.isNotEmpty()

                if (isUserRegistered) {
                    // User is registered, show normal greeting
                    val greeting = buildGreeting(profile.name)
                    _uiState.update { it.copy(userProfile = profile, aiReply = greeting) }
                    // Load all restaurants automatically on first open
                    if (!initialRestaurantsLoaded) {
                        initialRestaurantsLoaded = true
                        fetchSearch("ver todos")
                    }
                } else {
                    // User is NOT registered, show welcome message
                    // Don't navigate yet - wait for TTS to finish
                    val welcomeMessage = buildWelcomeMessage()
                    _uiState.update {
                        it.copy(
                            userProfile = profile,
                            aiReply = welcomeMessage,
                            pendingProfileNavigation = true // Set flag to navigate after TTS
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            profileRepository.userProfileFlow.first { it.id.isNotBlank() }
            refreshOrdersInternal()
        }
        startStatusPolling()
        observeFavoriteOrders()
        observeFavoriteOrderNicknames()
        observeOrderSearchQueries()
        observeOrderItemRatings()
        observeOrderProductIds()
        observeOrderRestaurantIds()
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

    private fun observeFavoriteOrderNicknames() {
        viewModelScope.launch {
            profileRepository.favoriteOrderNicknamesFlow.collect { nicknames ->
                _uiState.update { it.copy(favoriteOrderNicknames = nicknames) }
            }
        }
    }

    private fun observeOrderSearchQueries() {
        viewModelScope.launch {
            profileRepository.orderSearchQueriesFlow.collect { queries ->
                _uiState.update { it.copy(orderSearchQueries = queries) }
            }
        }
    }

    fun updateFavoriteOrderNickname(orderId: String, nickname: String) {
        viewModelScope.launch {
            profileRepository.saveFavoriteOrderNickname(orderId, nickname)
        }
    }

    private fun observeOrderItemRatings() {
        viewModelScope.launch {
            profileRepository.orderItemRatingsFlow.collect { ratings ->
                _uiState.update { it.copy(orderItemRatings = ratings) }
            }
        }
    }

    private fun observeOrderProductIds() {
        viewModelScope.launch {
            profileRepository.orderProductIdsFlow.collect { productIds ->
                _uiState.update { it.copy(orderProductIds = productIds) }
            }
        }
    }

    private fun observeOrderRestaurantIds() {
        viewModelScope.launch {
            profileRepository.orderRestaurantIdsFlow.collect { restaurantIds ->
                _uiState.update { it.copy(orderRestaurantIds = restaurantIds) }
            }
        }
    }

    fun rateOrderItem(orderId: String, productId: Int, restaurantId: Int, productName: String, rating: Int) {
        viewModelScope.launch {
            // Guarda localmente para atualizar a UI imediatamente
            profileRepository.saveOrderItemRating(orderId, productName, rating)

            // Resolve productId — usa o local se a API devolveu 0
            val resolvedProductId = if (productId != 0) productId
                else _uiState.value.orderProductIds["$orderId::$productName"] ?: 0

            // Resolve restaurantId — usa o local se a API devolveu 0
            val resolvedRestaurantId = if (restaurantId != 0) restaurantId
                else _uiState.value.orderRestaurantIds[orderId] ?: 0

            if (resolvedProductId == 0) {
                println("⚠️ productId não encontrado para: orderId=$orderId, productName=$productName")
                return@launch
            }

            if (resolvedRestaurantId == 0) {
                println("⚠️ restaurantId não encontrado para: orderId=$orderId")
                return@launch
            }

            val request = RatingRequest(
                orderId = orderId,
                restaurantId = resolvedRestaurantId,
                ratings = listOf(RatingItemRequest(productId = resolvedProductId, rating = rating))
            )
            val response = apiClient.submitRatings(request)
            if (response == null || !response.success) {
                println("⚠️ Falha ao enviar avaliação: orderId=$orderId, productId=$resolvedProductId, restaurantId=$resolvedRestaurantId")
            } else {
                println("✅ Avaliação enviada: orderId=$orderId, productId=$resolvedProductId, restaurantId=$resolvedRestaurantId, rating=$rating")
            }
        }
    }


    private fun startStatusPolling() {
        viewModelScope.launch {
            while (true) {
                val userId = _uiState.value.userProfile.id
                val tab = _uiState.value.currentTab
                if (userId.isNotBlank() && (tab == MainTab.ORDERS || tab == MainTab.FAVORITES)) {
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

    fun updateUserProfile(name: String, email: String, phone: String, addresses: List<Address>) {
        viewModelScope.launch {
            val currentId = _uiState.value.userProfile.id
            val newId = if (currentId.isBlank()) "U-${(10000..99999).random()}" else currentId
            profileRepository.saveProfile(newId, name, email, phone, addresses)
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
                    _uiState.update { it.copy(isLoading = false, cartAiMessage = aiMsg, cartAiMessageSpoken = false) }
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

        // Prevent duplicate calls while a search is already in progress
        if (_uiState.value.isLoading) return

        // Clear textInput immediately to prevent duplicate calls (e.g. voice race condition)
        _uiState.update { it.copy(textInput = "") }

        var resolvedQuery = currentQuery.trim()

        val favoriteAlias = resolvedQuery.startsWith("pedir", ignoreCase = true)
        if (favoriteAlias) {
            val favoriteName = resolvedQuery
                .replaceFirst(Regex("^pedir\\s*", RegexOption.IGNORE_CASE), "")
                .trim()

            if (favoriteName.isBlank()) {
                return
            }

            val nicknames = _uiState.value.favoriteOrderNicknames
            val matchedOrderId = nicknames.entries
                .firstOrNull { it.value.contains(favoriteName, ignoreCase = true) }
                ?.key

            if (matchedOrderId == null) {
                return
            }

            val favoriteOrder = _uiState.value.favoriteOrders
                .firstOrNull { it.id == matchedOrderId }

            if (favoriteOrder == null) {
                return
            }

            resolvedQuery = favoriteOrder.searchQuery
            // Determine the display name: nickname if set, otherwise restaurant name
            val displayName = nicknames[matchedOrderId]?.takeIf { it.isNotBlank() }
                ?: favoriteOrder.restaurantName
            fetchSearch(resolvedQuery, favoriteName = displayName)
            return
        }

        fetchSearch(resolvedQuery)
    }

    private fun fetchSearch(resolvedQuery: String, favoriteName: String? = null) {
        _uiState.update { it.copy(isLoading = true, error = null, isSuggestionMode = false) }
        viewModelScope.launch {
            try {
                val response = apiClient.searchRestaurants(resolvedQuery.trim())

                val hasBoth = response.restaurantResults.isNotEmpty() && response.productResults.isNotEmpty()
                val isProductOnly = response.restaurantResults.isEmpty() && response.productResults.isNotEmpty()

                if (hasBoth) {
                    // Guarda resultados pendentes e pede ao utilizador para escolher
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            aiReply = response.reply,
                            textInput = "",
                            lastSearchQuery = resolvedQuery,
                            pendingRestaurantResults = response.restaurantResults,
                            pendingProductResults = response.productResults,
                            showSearchTypeSheet = true
                        )
                    }
                } else {
                    val resolvedReply = when {
                        resolvedQuery.equals("ver todos", ignoreCase = true) ->
                            _uiState.value.aiReply.ifBlank { "Todos os restaurantes disponíveis" }
                        else -> response.reply
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            aiReply = resolvedReply,
                            restaurantResults = response.restaurantResults,
                            productResults = response.productResults,
                            textInput = "",
                            lastSearchQuery = resolvedQuery
                        )
                    }
                    if (isProductOnly) {
                        addProductsToCart(response.productResults)
                        val productNames = response.productResults
                            .take(3)
                            .joinToString(", ") { it.name }
                        val aiMsg = buildString {
                            if (favoriteName != null) {
                                append("⭐ Pedido favorito \"$favoriteName\" adicionado à sacola: $productNames")
                            } else {
                                append("✅ Adicionei à sua sacola: $productNames")
                            }
                            if (response.productResults.size > 3)
                                append(" e mais ${response.productResults.size - 3} itens")
                            append(".\n\n💡 Quer que sugira outro restaurante com pratos semelhantes, ou gostaria de adicionar mais alguma coisa deste restaurante?")
                        }
                        _uiState.update { it.copy(cartAiMessage = aiMsg, cartAiMessageSpoken = false) }
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
            _uiState.update { it.copy(cartAiMessage = aiMsg, cartAiMessageSpoken = false) }
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
                    cartRestaurantId = product.restaurant_id
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

    fun markCartAiMessageAsSpoken() {
        _uiState.update { it.copy(cartAiMessageSpoken = true) }
    }

    fun completePendingProfileNavigation() {
        _uiState.update {
            it.copy(
                currentTab = MainTab.PROFILE,
                pendingProfileNavigation = false
            )
        }
    }

    fun resetOrderJustPlacedFlag() {
        _uiState.update { it.copy(orderJustPlaced = false) }
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
        if (tab == MainTab.ORDERS || tab == MainTab.FAVORITES) {
            val userId = _uiState.value.userProfile.id
            if (userId.isNotBlank()) {
                viewModelScope.launch { refreshOrdersInternal() }
            }
        }
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
        // Always show address selection sheet — no default concept
        _uiState.update { it.copy(isAddressSheetVisible = true) }
    }

    fun dismissPaymentConfirmSheet() {
        _uiState.update { it.copy(showPaymentConfirmSheet = false) }
    }

    /** Called when user confirms payment with saved card (useSavedCard=true) or chooses another method (false). */
    fun onPaymentConfirmResult(useSavedCard: Boolean) {
        if (_uiState.value.selectedAddressForCheckout == null) return
        _uiState.update {
            it.copy(
                showPaymentConfirmSheet = false,
                showDeliveryTypeSheet = true,
                pendingCheckoutSavePaymentMethod = useSavedCard
            )
        }
    }

    fun dismissSavePaymentSheet() {
        _uiState.update { it.copy(showSavePaymentSheet = false) }
    }

    fun showPaymentConfirmForAddress(address: Address) {
        _uiState.update {
            it.copy(
                isAddressSheetVisible = false,
                showPaymentConfirmSheet = true,
                selectedAddressForCheckout = address
            )
        }
    }

    fun showSavePaymentSheetForAddress(address: Address) {
        _uiState.update {
            it.copy(
                isAddressSheetVisible = false,
                showSavePaymentSheet = true,
                selectedAddressForCheckout = address
            )
        }
    }

    fun proceedToCheckout(savePaymentMethod: Boolean) {
        _uiState.update {
            it.copy(
                showSavePaymentSheet = false,
                showDeliveryTypeSheet = true,
                pendingCheckoutSavePaymentMethod = savePaymentMethod
            )
        }
    }

    fun dismissDeliveryTypeSheet() {
        _uiState.update { it.copy(showDeliveryTypeSheet = false) }
    }

    fun proceedWithDeliveryType(deliveryType: String) {
        _uiState.update { it.copy(showDeliveryTypeSheet = false) }
        val currentState = _uiState.value
        val address = currentState.selectedAddressForCheckout ?: return
        val savePaymentMethod = currentState.pendingCheckoutSavePaymentMethod
        _uiState.update {
            it.copy(
                selectedAddressForCheckout = null,
                pendingCheckoutSavePaymentMethod = false
            )
        }
        confirmCheckout(address, savePaymentMethod, deliveryType)
    }

    fun dismissAddressSheet() {
        _uiState.update { it.copy(isAddressSheetVisible = false) }
    }

    fun confirmCheckout(selectedAddress: Address, savePaymentMethod: Boolean = false, deliveryType: String = "") {
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

            val trackingCode = generateTrackingCode()

            // Calculates the base time: highest preparation time among the cart products
            val baseTime = currentState.cartItems.maxOfOrNull { product ->
                product.preparationTime
                    .filter { it.isDigit() || it == '.' }
                    .toDoubleOrNull()?.toInt() ?: 0
            } ?: 0

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
                save_payment_method = savePaymentMethod,
                search_query = currentState.lastSearchQuery,
                tracking_code = trackingCode,
                deliveryType = deliveryType,
                baseTime = baseTime
            )

            val sessionResponse = apiClient.initiateCheckout(request)

            if (sessionResponse == null) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao iniciar pagamento.") }
                return@launch
            }

            // Handle auto_paid scenario (payment method already saved)
            if (sessionResponse.auto_paid) {
                // Save search query locally before clearing state
                val orderId = sessionResponse.order_id
                if (orderId != null && currentState.lastSearchQuery.isNotBlank()) {
                    profileRepository.saveOrderSearchQuery(orderId.toString(), currentState.lastSearchQuery)
                }
                // Guardar mapeamento productName -> productId para avaliações futuras
                if (orderId != null) {
                    val productIdMap = currentState.cartItems.map { it.name to it.id }
                    profileRepository.saveOrderProductIds(orderId.toString(), productIdMap)
                    profileRepository.saveOrderRestaurantId(orderId.toString(), restaurant.id)
                }
                // Go DIRECTLY to Orders screen - don't wait for confirmation
                // Status will update in real-time on the Orders screen
                val greeting = buildGreeting(currentState.userProfile.name)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isProcessingAutoPayment = false, // No overlay
                        cartItems = emptyList(), // Clear cart immediately
                        cartRestaurantId = null,
                        selectedRestaurant = null, // Clear selected restaurant
                        cartAiMessage = null, // Clear AI chat bubble
                        restaurantResults = emptyList(), // Clear restaurant search results
                        productResults = emptyList(), // Clear product search results
                        textInput = "", // Clear search input
                        lastSearchQuery = "", // Clear last search query
                        aiReply = greeting, // Reset to greeting
                        isSuggestionMode = false, // Reset suggestion mode
                        selectedCategory = null, // Clear selected category
                        showSearchTypeSheet = false, // Clear search type sheet
                        pendingRestaurantResults = emptyList(), // Clear pending restaurants
                        pendingProductResults = emptyList(), // Clear pending products
                        currentTab = MainTab.ORDERS, // Go to Orders NOW!
                        error = null,
                        pendingSavePaymentMethod = false,
                        orderJustPlaced = true // Trigger voice feedback
                    )
                }

                // Refresh orders immediately to show the new order (status: Pendente)
                refreshOrders()

                // Start background polling to update order status in real-time
                // User will see status change from "Pendente" to "Em Preparo" automatically
                startBackgroundPolling(currentState.userProfile.id)
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
            val greeting = buildGreeting(_uiState.value.userProfile.name)
            val pendingSearchQuery = _uiState.value.lastSearchQuery
            val cartItems = _uiState.value.cartItems

            // Save search query locally before clearing state
            if (!orderId.isNullOrBlank() && pendingSearchQuery.isNotBlank()) {
                viewModelScope.launch {
                    profileRepository.saveOrderSearchQuery(orderId, pendingSearchQuery)
                }
            }

            // Guardar mapeamento productName -> productId para avaliações futuras
            if (!orderId.isNullOrBlank() && cartItems.isNotEmpty()) {
                viewModelScope.launch {
                    val productIdMap = cartItems.map { it.name to it.id }
                    profileRepository.saveOrderProductIds(orderId, productIdMap)
                    val restaurantId = _uiState.value.cartRestaurantId
                    if (restaurantId != null) {
                        profileRepository.saveOrderRestaurantId(orderId, restaurantId)
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    checkoutUrl = null,
                    cartItems = emptyList(),
                    cartRestaurantId = null,
                    selectedRestaurant = null, // Clear selected restaurant
                    cartAiMessage = null, // Clear AI chat bubble
                    restaurantResults = emptyList(), // Clear restaurant search results
                    productResults = emptyList(), // Clear product search results
                    textInput = "", // Clear search input
                    lastSearchQuery = "", // Clear last search query
                    aiReply = greeting, // Reset to greeting
                    isSuggestionMode = false, // Reset suggestion mode
                    selectedCategory = null, // Clear selected category
                    showSearchTypeSheet = false, // Clear search type sheet
                    pendingRestaurantResults = emptyList(), // Clear pending restaurants
                    pendingProductResults = emptyList(), // Clear pending products
                    currentTab = MainTab.ORDERS,
                    error = null,
                    pendingSavePaymentMethod = false,
                    orderJustPlaced = true // Trigger voice feedback
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

    fun markOrderAsDelivered(orderId: String) {
        viewModelScope.launch {
            val success = apiClient.updateOrderStatus(orderId, "Entregue")
            if (success) {
                refreshOrdersInternal()
            } else {
                _uiState.update { it.copy(error = "Erro ao marcar pedido como entregue.") }
            }
        }
    }

    private fun startBackgroundPolling(userId: String) {
        viewModelScope.launch {
            // Poll every 2 seconds for up to 30 seconds to catch status updates
            repeat(15) { // 15 attempts * 2 seconds = 30 seconds
                delay(2000) // Wait 2 seconds between updates

                try {
                    // Silently refresh orders in the background
                    refreshOrdersInternal()
                } catch (e: Exception) {
                    println("⚠️ Background polling error: ${e.message}")
                    // Don't show error to user, just continue
                }
            }
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

    private fun addProductsToCart(products: List<Product>) {
        _uiState.update { currentState ->
            val cartMap = currentState.cartItems.associateBy { it.id }.toMutableMap()
            for (product in products) {
                val existing = cartMap[product.id]
                if (existing != null) {
                    cartMap[product.id] = existing.copy(quantity = existing.quantity + product.quantity)
                } else {
                    cartMap[product.id] = product.copy(quantity = product.quantity)
                }
            }
            currentState.copy(
                cartItems = cartMap.values.toList(),
                cartRestaurantId = products.firstOrNull()?.restaurant_id ?: currentState.cartRestaurantId
            )
        }
    }
}