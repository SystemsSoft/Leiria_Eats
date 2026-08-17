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
import org.leria.eats.project.data.ChatRepository
import org.leria.eats.project.data.ChatResponse
import org.leria.eats.project.data.DeliveryFeeRequest
import org.leria.eats.project.data.DeliveryFeeResponse
import org.leria.eats.project.data.LeriaApiClient
import org.leria.eats.project.data.Order
import org.leria.eats.project.data.OrderItemRequest
import org.leria.eats.project.data.OrderRequest
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.ProfileRepository
import org.leria.eats.project.data.RatingItemRequest
import org.leria.eats.project.data.RatingRequest
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.data.SearchResponse
import org.leria.eats.project.presentation.ChatMessage
import org.leria.eats.project.presentation.ChatMessageType
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.SearchUiState

class SearchViewModel(
    private val apiClient: LeriaApiClient,
    private val profileRepository: ProfileRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    /** Gera um código de pedido numérico único com 9 dígitos */
    private fun generateTrackingCode(): String {
        return (100000000..999999999).random().toString()
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private fun buildGreeting(name: String): String {
        // Saudação removida - retorna string vazia
        return ""
    }

    private fun buildWelcomeMessage(): String {
        return "Crie o seu perfil para começar a pedir. Leva menos de 1 minuto! 🍽️"
    }

    private val favoriteOrderIdsFlow = profileRepository.favoriteOrderIdsFlow

    private var initialRestaurantsLoaded = false

    /** Carrega todos os restaurantes e guarda em [SearchUiState.allRestaurants] para o Home. */
    private fun loadAllRestaurantsForHome() {
        viewModelScope.launch {
            try {
                val restaurants = apiClient.getAllRestaurants()
                if (restaurants.isNotEmpty()) {
                    _uiState.update { it.copy(allRestaurants = restaurants) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
                        loadAllRestaurantsForHome()
                    }
                } else {
                    // Utilizador sem perfil → abrir diretamente no Profile, sem TTS
                    _uiState.update {
                        it.copy(
                            userProfile = profile,
                            aiReply = buildWelcomeMessage(),
                            currentTab = MainTab.PROFILE,
                            pendingProfileNavigation = false
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
        observeChatMessages()
    }


    private fun observeChatMessages() {
        viewModelScope.launch {
            chatRepository.chatMessagesFlow.collect { messages ->
                if (messages.isNotEmpty()) {
                    _uiState.update { it.copy(chatMessages = messages) }
                } else {
                    // Se não houver mensagens salvas, mostra a saudação inicial
                    val greeting = buildGreeting(_uiState.value.userProfile.name)
                    if (greeting.isNotBlank()) {
                        val initialMessage = ChatMessage(
                            id = "initial",
                            type = ChatMessageType.AI,
                            text = greeting
                        )
                        _uiState.update { it.copy(chatMessages = listOf(initialMessage)) }
                    }
                }
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

    fun updateUserProfile(name: String, email: String, phone: String, addresses: List<Address>, photoUrl: String? = null, allergies: String = "", lifestyles: String = "") {
        viewModelScope.launch {
            val currentId = _uiState.value.userProfile.id
            val newId = if (currentId.isBlank()) "U-${(10000..99999).random()}" else currentId
            profileRepository.saveProfile(newId, name, email, phone, addresses, photoUrl, allergies, lifestyles)
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
        if (restaurant.products.isEmpty()) {
            // Se o restaurante não tem produtos carregados, buscar pela API
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val company = apiClient.getCompanyById(restaurant.id)
                val resolvedRestaurant = if (company != null) Restaurant(
                    id = company.id,
                    name = company.name,
                    category = company.category,
                    image_url = company.imageUrl,
                    products = company.products
                ) else restaurant
                _uiState.update { it.copy(selectedRestaurant = resolvedRestaurant, selectedCategory = null, isLoading = false) }
            }
        } else {
            // Restaurante já tem produtos carregados
            _uiState.update { it.copy(selectedRestaurant = restaurant, selectedCategory = null) }
        }
    }

    fun selectRestaurantOrAddToCart(restaurant: Restaurant) {
        if (_uiState.value.isSuggestionMode) {
            // Modo sugestão: abrir cardápio do restaurante selecionado
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

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedRestaurant = resolvedRestaurant,
                        selectedCategory = null,
                        currentTab = MainTab.HOME
                    )
                }
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
                cartRestaurantId = null,
                cartRestaurants = emptyList(),
                isAiCartFlow = false
            )
        }
    }

    fun clearSearch() {
        viewModelScope.launch {
            chatRepository.clearChat()
        }
        val greeting = buildGreeting(_uiState.value.userProfile.name)
        _uiState.update {
            it.copy(
                restaurantResults = emptyList(),
                productResults = emptyList(),
                aiReply = greeting,
                textInput = "",
                error = null,
                chatMessages = if (greeting.isNotBlank()) {
                    listOf(
                        ChatMessage(
                            id = "initial",
                            type = ChatMessageType.AI,
                            text = greeting
                        )
                    )
                } else emptyList()
            )
        }
    }

    private fun addUserMessage(text: String) {
        val message = ChatMessage(
            id = "user_${kotlin.random.Random.nextLong()}",
            type = ChatMessageType.USER,
            text = text
        )
        _uiState.update {
            val updatedMessages = (it.chatMessages + message).takeLast(20)
            it.copy(chatMessages = updatedMessages)
        }
        saveChatMessages()
    }

    fun addAiMessage(
        text: String,
        restaurants: List<Restaurant> = emptyList(),
        products: List<Product> = emptyList()
    ) {
        val message = ChatMessage(
            id = "ai_${kotlin.random.Random.nextLong()}",
            type = ChatMessageType.AI,
            text = text,
            restaurants = restaurants,
            products = products
        )
        _uiState.update {
            val updatedMessages = (it.chatMessages + message).takeLast(20)
            it.copy(chatMessages = updatedMessages)
        }
        saveChatMessages()
    }

    private fun saveChatMessages() {
        viewModelScope.launch {
            chatRepository.saveChatMessages(_uiState.value.chatMessages)
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
        // Adiciona a mensagem do usuário ao chat
        addUserMessage(resolvedQuery)

        _uiState.update { it.copy(isLoading = true, error = null, isSuggestionMode = false) }
        viewModelScope.launch {
            try {
                // Usa o novo endpoint com IA Generativa
                val chatResponse = apiClient.sendChatMessage(resolvedQuery.trim())

                // Verifica se o pedido foi confirmado pela IA
                if (chatResponse.orderConfirmed) {
                    handleOrderConfirmation(chatResponse)
                    return@launch
                }

                // Se o usuário pediu para ver todos os restaurantes explicitamente,
                // consome o endpoint /restaurants separado da IA para garantir que a
                // lista completa seja obtida e separada do resultado gerado pela IA.
                var aiRestaurantResults: List<Restaurant> = chatResponse.restaurantResults
                if (resolvedQuery.equals("ver todos", ignoreCase = true)
                    || resolvedQuery.equals("restaurantes", ignoreCase = true)
                    || resolvedQuery.contains("todos os restaurantes", ignoreCase = true)
                ) {
                    try {
                        val all = apiClient.getAllRestaurants()
                        if (all.isNotEmpty()) aiRestaurantResults = all
                    } catch (e: Exception) {
                        // Mantém os resultados da IA caso o endpoint falhe
                        println("⚠️ Falha ao buscar /restaurants: ${e.message}")
                    }
                }

                // Converte para SearchResponse para compatibilidade com código existente
                val response = SearchResponse(
                    reply = chatResponse.response,
                    intent = chatResponse.intent,
                    restaurantResults = aiRestaurantResults,
                    productResults = if (chatResponse.products.isNotEmpty())
                        chatResponse.products
                    else
                        chatResponse.productResults
                )

                val hasBoth = response.restaurantResults.isNotEmpty() && response.productResults.isNotEmpty()
                val hasOnlyRestaurants = response.restaurantResults.isNotEmpty() && response.productResults.isEmpty()
                val hasOnlyProducts = response.restaurantResults.isEmpty() && response.productResults.isNotEmpty()

                if (hasBoth) {
                    // Guarda resultados pendentes e pede ao utilizador para escolher
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            aiReply = response.reply?: "",
                            textInput = "",
                            lastSearchQuery = resolvedQuery,
                            pendingRestaurantResults = response.restaurantResults,
                            pendingProductResults = response.productResults,
                            showSearchTypeSheet = true
                        )
                    }
                } else if (hasOnlyRestaurants) {
                    val resolvedReply = when {
                        resolvedQuery.equals("ver todos", ignoreCase = true) ->
                            _uiState.value.aiReply.ifBlank { "Todos os restaurantes disponíveis" }
                        else -> response.reply
                    }

                    // Adiciona mensagem da IA com restaurantes
                    addAiMessage(
                        text = resolvedReply ?: "Encontrei estes restaurantes para você:",
                        restaurants = response.restaurantResults
                    )

                    // Se a pesquisa trouxe todos os restaurantes, atualiza também a lista do Home
                    val updatedAllRestaurants = if (
                        resolvedQuery.equals("ver todos", ignoreCase = true) &&
                        response.restaurantResults.isNotEmpty()
                    ) response.restaurantResults else _uiState.value.allRestaurants

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            aiReply = resolvedReply?: "",
                            restaurantResults = response.restaurantResults,
                            productResults = emptyList(),
                            textInput = "",
                            lastSearchQuery = resolvedQuery,
                            allRestaurants = updatedAllRestaurants
                        )
                    }
                } else if (hasOnlyProducts) {
                    val aiReplyText = response.reply ?: "Encontrei estes produtos para você:"

                    // Adiciona mensagem da IA com produtos
                    addAiMessage(
                        text = aiReplyText,
                        products = response.productResults
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            aiReply = aiReplyText,
                            restaurantResults = emptyList(),
                            productResults = response.productResults,
                            textInput = "",
                            lastSearchQuery = resolvedQuery
                        )
                    }
                } else {
                    // Nenhum resultado encontrado
                    val noResultsMessage = response.reply ?: "Desculpe, não encontrei nada relacionado à sua pesquisa. Tente usar outras palavras!"
                    addAiMessage(text = noResultsMessage)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            aiReply = noResultsMessage,
                            restaurantResults = emptyList(),
                            productResults = emptyList(),
                            textInput = "",
                            lastSearchQuery = resolvedQuery
                        )
                    }
                }

            } catch (e: Exception) {
                val errorMessage = "Erro ao conectar: ${e.message}"
                addAiMessage(text = errorMessage)
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                e.printStackTrace()
            }
        }
    }

    /**
     * Processa a confirmação do pedido pela IA.
     * Sincroniza os produtos da IA com o carrinho e valida o restaurante.
     */
    private suspend fun handleOrderConfirmation(chatResponse: ChatResponse) {
        val aiProducts = chatResponse.products
        
        if (aiProducts.isEmpty()) {
            val errorMsg = "Não encontrei os produtos para finalizar o pedido. Pode repetir o que deseja?"
            addAiMessage(text = errorMsg)
            _uiState.update { it.copy(isLoading = false, error = errorMsg) }
            return
        }

        addProductsToCart(aiProducts)

        val confirmationMessage = chatResponse.response ?: "Perfeito! Tudo pronto com o seu pedido."
        addAiMessage(text = confirmationMessage)

        _uiState.update {
            it.copy(
                isLoading = false,
                currentTab = MainTab.CART,
                isAiCartFlow = true
            )
        }

        delay(500)

        checkout()
        
        clearSearch()
    }

    fun onSearchTypeSelected(showRestaurants: Boolean) {
        val state = _uiState.value
        if (showRestaurants) {
            addAiMessage(
                text = state.aiReply.ifBlank { "Aqui estão os restaurantes:" },
                restaurants = state.pendingRestaurantResults
            )

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
            addAiMessage(
                text = state.aiReply.ifBlank { "Aqui estão os produtos:" },
                products = products
            )

            _uiState.update {
                it.copy(
                    showSearchTypeSheet = false,
                    restaurantResults = emptyList(),
                    productResults = products,
                    pendingRestaurantResults = emptyList(),
                    pendingProductResults = emptyList()
                )
            }
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
                cartRestaurantId = product.restaurant_id, // Mantido para compatibilidade, mas agora o sistema suporta múltiplos
                isAiCartFlow = false // Força fluxo normal se adicionado manualmente
            )
        }
        fetchCompanyById(product.restaurant_id)
    }

    private fun fetchCompanyById(id: Int) {
        viewModelScope.launch {
            val company = apiClient.getCompanyById(id)
            if (company == null) return@launch
            
            val restaurantMetadata = Restaurant(
                id = company.id,
                name = company.name,
                category = company.category,
                image_url = company.imageUrl,
                products = company.products
            )

            _uiState.update { state ->
                val updatedCartRestaurants = if (state.cartRestaurants.none { it.id == id }) {
                    state.cartRestaurants + restaurantMetadata
                } else {
                    state.cartRestaurants.map { if (it.id == id) restaurantMetadata else it }
                }

                val shouldUpdateRestaurant =
                    state.selectedRestaurant == null ||
                    state.selectedRestaurant?.id == company.id ||
                    state.selectedRestaurant?.products?.isEmpty() == true

                state.copy(
                    cartRestaurants = updatedCartRestaurants,
                    selectedRestaurant = if (shouldUpdateRestaurant) restaurantMetadata else state.selectedRestaurant
                )
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
            
            // Atualiza a lista de metadados se um restaurante não tiver mais produtos
            val remainingRestaurantIds = updatedCart.map { it.restaurant_id }.toSet()
            val updatedCartRestaurants = currentState.cartRestaurants.filter { it.id in remainingRestaurantIds }
            
            val newRestaurantId = if (updatedCart.isEmpty()) null else {
                // Se o restaurante removido era o "principal", escolhe o próximo disponível
                if (product.restaurant_id == currentState.cartRestaurantId && product.restaurant_id !in remainingRestaurantIds) {
                    remainingRestaurantIds.firstOrNull()
                } else {
                    currentState.cartRestaurantId
                }
            }
            
            currentState.copy(
                cartItems = updatedCart, 
                cartRestaurantId = newRestaurantId,
                cartRestaurants = updatedCartRestaurants
            )
        }
    }

    fun clearCart() {
        _uiState.update { 
            it.copy(
                cartItems = emptyList(), 
                cartRestaurantId = null,
                cartRestaurants = emptyList(),
                isAiCartFlow = false
            ) 
        }
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

    suspend fun getDeliveryFee(
        customerLat: Double,
        customerLon: Double,
        restaurantLat: Double,
        restaurantLon: Double,
        restaurantId: Int
    ): DeliveryFeeResponse {
        return apiClient.getDeliveryFee(
            DeliveryFeeRequest(
                customer_latitude = customerLat,
                customer_longitude = customerLon,
                restaurant_latitude = restaurantLat,
                restaurant_longitude = restaurantLon,
                restaurant_id = restaurantId
            )
        )
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

    /** Called from the ServiceFeeBottomSheet — skips the address selection sheet */
    fun checkoutWithAddress(address: Address, deliveryFee: Double = 0.0, serviceFee: Double = 0.0, deliveryType: String = "delivery") {
        val currentState = _uiState.value
        if (currentState.userProfile.name.isBlank()) {
            _uiState.update { it.copy(error = "Por favor, preencha seu Nome no Perfil.") }
            onTabSelected(MainTab.PROFILE)
            return
        }
        if (currentState.cartItems.isEmpty()) return
        // Store the delivery type chosen in the summary
        _uiState.update { it.copy(pendingDeliveryType = deliveryType) }
        val hasSavedPaymentMethods = currentState.userProfile.savedPaymentMethods.isNotEmpty()
        if (hasSavedPaymentMethods) {
            showPaymentConfirmForAddress(address, deliveryFee, serviceFee)
        } else {
            showSavePaymentSheetForAddress(address, deliveryFee, serviceFee)
        }
    }

    fun dismissPaymentConfirmSheet() {
        _uiState.update { it.copy(showPaymentConfirmSheet = false) }
    }

    /** Called when user confirms payment with saved card (useSavedCard=true) or chooses another method (false). */
    fun onPaymentConfirmResult(useSavedCard: Boolean) {
        if (_uiState.value.selectedAddressForCheckout == null) return
        val deliveryType = _uiState.value.pendingDeliveryType
        _uiState.update {
            it.copy(
                showPaymentConfirmSheet = false,
                // Sempre passa false para não salvar cartão como padrão
                pendingCheckoutSavePaymentMethod = false
            )
        }
        // Proceed directly with the type already chosen in the summary
        proceedWithDeliveryType(deliveryType)
    }

    fun dismissSavePaymentSheet() {
        _uiState.update { it.copy(showSavePaymentSheet = false) }
    }

    fun showPaymentConfirmForAddress(address: Address, deliveryFee: Double = 0.0, serviceFee: Double = 0.0) {
        _uiState.update {
            it.copy(
                isAddressSheetVisible = false,
                showPaymentConfirmSheet = true,
                selectedAddressForCheckout = address,
                pendingDeliveryFee = deliveryFee,
                pendingServiceFee = serviceFee
            )
        }
    }

    fun showSavePaymentSheetForAddress(address: Address, deliveryFee: Double = 0.0, serviceFee: Double = 0.0) {
        _uiState.update {
            it.copy(
                isAddressSheetVisible = false,
                showSavePaymentSheet = true,
                selectedAddressForCheckout = address,
                pendingDeliveryFee = deliveryFee,
                pendingServiceFee = serviceFee
            )
        }
    }

    fun proceedToCheckout(savePaymentMethod: Boolean) {
        val deliveryType = _uiState.value.pendingDeliveryType
        _uiState.update {
            it.copy(
                showSavePaymentSheet = false,
                // Sempre passa false para não salvar cartão como padrão
                pendingCheckoutSavePaymentMethod = false
            )
        }
        // Use the delivery type already chosen in the summary
        proceedWithDeliveryType(deliveryType)
    }

    fun dismissDeliveryTypeSheet() {
        _uiState.update { it.copy(showDeliveryTypeSheet = false) }
    }

    fun proceedWithDeliveryType(deliveryType: String) {
        _uiState.update { it.copy(showDeliveryTypeSheet = false) }
        val currentState = _uiState.value
        val address = currentState.selectedAddressForCheckout ?: return
        val savePaymentMethod = currentState.pendingCheckoutSavePaymentMethod
        val deliveryFee = currentState.pendingDeliveryFee
        val serviceFee = currentState.pendingServiceFee
        _uiState.update {
            it.copy(
                selectedAddressForCheckout = null,
                pendingCheckoutSavePaymentMethod = false,
                pendingDeliveryFee = 0.0,
                pendingServiceFee = 0.0
            )
        }
        confirmCheckout(address, savePaymentMethod, deliveryType, deliveryFee, serviceFee)
    }

    fun dismissAddressSheet() {
        _uiState.update { it.copy(isAddressSheetVisible = false) }
    }

    fun confirmCheckout(selectedAddress: Address, savePaymentMethod: Boolean = false, deliveryType: String = "", deliveryFee: Double = 0.0, serviceFee: Double = 0.0) {
        _uiState.update {
            it.copy(
                isAddressSheetVisible = false,
                isLoading = true,
                error = null,
                pendingSavePaymentMethod = savePaymentMethod
            )
        }

        val currentState = _uiState.value
        val itemsByRestaurant = currentState.cartItems.groupBy { it.restaurant_id }

        if (itemsByRestaurant.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, error = "Sua sacola está vazia.") }
            return
        }

        if (currentState.userProfile.name.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Por favor, preencha seu Nome no Perfil.") }
            onTabSelected(MainTab.PROFILE)
            return
        }

        viewModelScope.launch {
            var firstStripeUrl: String? = null
            var successCount = 0
            var anyFailure = false
            val totalRestaurants = itemsByRestaurant.size
            
            // Taxas distribuídas (simplificação para múltiplos pedidos)
            val serviceFeePerOrder = serviceFee / totalRestaurants
            val deliveryFeePerOrder = deliveryFee / totalRestaurants

            for ((restaurantId, products) in itemsByRestaurant) {
                // 1. Obter metadados do restaurante
                var restaurant = currentState.cartRestaurants.find { it.id == restaurantId }
                if (restaurant == null) {
                    val company = apiClient.getCompanyById(restaurantId)
                    if (company != null) {
                        restaurant = Restaurant(
                            id = company.id,
                            name = company.name,
                            category = company.category,
                            image_url = company.imageUrl,
                            products = company.products
                        )
                    }
                }

                if (restaurant == null) {
                    anyFailure = true
                    continue
                }

                // 2. Preparar itens do pedido
                val orderItems = products.map { product ->
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
                val baseTime = products.maxOfOrNull { p ->
                    p.preparationTime.filter { it.isDigit() || it == '.' }.toDoubleOrNull()?.toInt() ?: 0
                } ?: 0

                // 3. Criar request
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
                    baseTime = baseTime,
                    deliveryLatitude = selectedAddress.latitude,
                    deliveryLongitude = selectedAddress.longitude,
                    deliveryFee = deliveryFeePerOrder,
                    serviceFee = serviceFeePerOrder
                )

                // 4. Iniciar checkout
                try {
                    val sessionResponse = apiClient.initiateCheckout(request)
                    if (sessionResponse != null) {
                        if (sessionResponse.auto_paid) {
                            successCount++
                            // Guardar mapeamentos
                            val orderId = sessionResponse.order_id
                            if (orderId != null) {
                                profileRepository.saveOrderSearchQuery(orderId.toString(), currentState.lastSearchQuery)
                                val productIdMap = products.map { it.name to it.id }
                                profileRepository.saveOrderProductIds(orderId.toString(), productIdMap)
                                profileRepository.saveOrderRestaurantId(orderId.toString(), restaurant.id)
                            }
                        } else if (firstStripeUrl == null) {
                            firstStripeUrl = sessionResponse.url
                        }
                    } else {
                        anyFailure = true
                    }
                } catch (e: Exception) {
                    anyFailure = true
                    e.printStackTrace()
                }
            }

            // 5. Finalizar estado
            if (successCount > 0 || firstStripeUrl != null) {
                val greeting = buildGreeting(currentState.userProfile.name)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isProcessingAutoPayment = false,
                        cartItems = emptyList(),
                        cartRestaurantId = null,
                        cartRestaurants = emptyList(),
                        selectedRestaurant = null,
                        cartAiMessage = null,
                        restaurantResults = emptyList(),
                        productResults = emptyList(),
                        textInput = "",
                        lastSearchQuery = "",
                        aiReply = greeting,
                        isSuggestionMode = false,
                        selectedCategory = null,
                        showSearchTypeSheet = false,
                        pendingRestaurantResults = emptyList(),
                        pendingProductResults = emptyList(),
                        currentTab = if (firstStripeUrl != null) currentState.currentTab else MainTab.ORDERS,
                        checkoutUrl = firstStripeUrl,
                        error = if (anyFailure) "Alguns pedidos falharam ao ser processados." else null,
                        pendingSavePaymentMethod = false,
                        orderJustPlaced = successCount > 0,
                        isAiCartFlow = false
                    )
                }
                
                if (successCount > 0) {
                    refreshOrders()
                    startBackgroundPolling(currentState.userProfile.id)
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao processar seus pedidos. Tente novamente.") }
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
                        val successStatuses = listOf("Em Preparo", "Confirmado", "Pago", "estafeta chegou ao seu endereco", "Entregue")
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
        if (products.isEmpty()) return

        _uiState.update { currentState ->
            val cartItems = currentState.cartItems.toMutableList()
            
            for (product in products) {
                // Tenta encontrar um item existente (mesmo ID ou mesmo Nome + Restaurante)
                val existingIndex = cartItems.indexOfFirst { 
                    (it.id != 0 && it.id == product.id) || 
                    (it.name == product.name && it.restaurant_id == product.restaurant_id)
                }

                if (existingIndex != -1) {
                    val existing = cartItems[existingIndex]
                    cartItems[existingIndex] = existing.copy(quantity = existing.quantity + product.quantity)
                } else {
                    cartItems.add(product)
                }
            }

            currentState.copy(
                cartItems = cartItems,
                cartRestaurantId = products.firstOrNull()?.restaurant_id ?: currentState.cartRestaurantId,
                isAiCartFlow = true 
            )
        }

        // Busca metadados para todos os restaurantes únicos na lista de produtos
        val uniqueRestaurantIds = products.map { it.restaurant_id }.distinct()
        uniqueRestaurantIds.forEach { id ->
            fetchCompanyById(id)
        }
    }
}