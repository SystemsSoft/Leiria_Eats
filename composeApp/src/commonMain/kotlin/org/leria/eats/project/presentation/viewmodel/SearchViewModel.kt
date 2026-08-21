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
import org.leria.eats.project.data.SubOrderRequest
import org.leria.eats.project.payment.StripePaymentManager
import org.leria.eats.project.payment.StripePaymentResult
import org.leria.eats.project.presentation.ChatMessage
import org.leria.eats.project.presentation.ChatMessageType
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.SearchUiState
import org.leria.eats.project.util.Ulid
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

class SearchViewModel(
    private val apiClient: LeriaApiClient,
    private val profileRepository: ProfileRepository,
    private val chatRepository: ChatRepository,
    private val stripePaymentManager: StripePaymentManager
) : ViewModel() {

    /** Gera um código de pedido numérico único com 9 dígitos */
    private fun generateTrackingCode(): String {
        return (100000000..999999999).random().toString()
    }

    /** Gera um identificador único de string (ULID) */
    private fun generateGid(prefix: String = "ord"): String {
        return "${prefix}_${Ulid.nextUlid()}"
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
        observeOrderProductGids()
        observeOrderRestaurantGids()
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
                        order.copy(isFavorite = favoriteIds.contains(order.gid))
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
                currentFavorites - order.gid
            } else {
                currentFavorites + order.gid
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

    private fun observeOrderProductGids() {
        viewModelScope.launch {
            profileRepository.orderProductGidsFlow.collect { productGids ->
                _uiState.update { it.copy(orderProductGids = productGids) }
            }
        }
    }

    private fun observeOrderRestaurantGids() {
        viewModelScope.launch {
            profileRepository.orderRestaurantGidsFlow.collect { restaurantGids ->
                _uiState.update { it.copy(orderRestaurantGids = restaurantGids) }
            }
        }
    }

    fun rateOrderItem(orderId: String, productGid: String, restaurantGid: String, productName: String, rating: Int) {
        viewModelScope.launch {
            // Guarda localmente para atualizar a UI imediatamente
            profileRepository.saveOrderItemRating(orderId, productName, rating)

            // Resolve productGid — usa o local se a API devolveu vazio
            val resolvedProductGid = if (productGid.isNotBlank()) productGid
                else _uiState.value.orderProductGids["$orderId::$productName"] ?: ""

            // Resolve restaurantGid — usa o local se a API devolveu vazio
            val resolvedRestaurantGid = if (restaurantGid.isNotBlank()) restaurantGid
                else _uiState.value.orderRestaurantGids[orderId] ?: ""

            if (resolvedProductGid.isBlank()) {
                println("⚠️ productGid não encontrado para: orderId=$orderId, productName=$productName")
                return@launch
            }

            if (resolvedRestaurantGid.isBlank()) {
                println("⚠️ restaurantGid não encontrado para: orderId=$orderId")
                return@launch
            }

            val request = RatingRequest(
                orderId = orderId,
                restaurantGid = resolvedRestaurantGid,
                ratings = listOf(RatingItemRequest(productGid = resolvedProductGid, rating = rating))
            )
            val response = apiClient.submitRatings(request)
            if (response == null || !response.success) {
                println("⚠️ Falha ao enviar avaliação: orderId=$orderId, productGid=$resolvedProductGid, restaurantGid=$resolvedRestaurantGid")
            } else {
                println("✅ Avaliação enviada: orderId=$orderId, productGid=$resolvedProductGid, restaurantGid=$resolvedRestaurantGid, rating=$rating")
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
                order.copy(isFavorite = favoriteIds.contains(order.gid))
            }
            _uiState.update { it.copy(orderHistory = ordersWithFavorites) }

            val selectedGid = _uiState.value.selectedOrder?.gid
            if (selectedGid != null) {
                val updatedSelected = ordersWithFavorites.find { it.gid == selectedGid }
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
                val company = apiClient.getCompanyByGid(restaurant.gid)
                val resolvedRestaurant = if (company != null) Restaurant(
                    gid = company.gid,
                    name = company.name,
                    category = company.category,
                    image_url = company.imageUrl,
                    products = company.products,
                    latitude = company.latitude,
                    longitude = company.longitude
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
                val company = apiClient.getCompanyByGid(restaurant.gid)
                val resolvedRestaurant = if (company != null) Restaurant(
                    gid = company.gid,
                    name = company.name,
                    category = company.category,
                    image_url = company.imageUrl,
                    products = company.products,
                    latitude = company.latitude,
                    longitude = company.longitude
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
                cartRestaurantGid = null,
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
                .firstOrNull { it.gid == matchedOrderId }

            if (favoriteOrder == null) {
                return
            }

            resolvedQuery = favoriteOrder.searchQuery
            // Determine the display name: nickname if set, otherwise restaurant name
            val displayName = nicknames[matchedOrderId]?.takeIf { it.isNotBlank() }
                ?: (favoriteOrder.subOrders.firstOrNull()?.restaurantName ?: "")
            fetchSearch(resolvedQuery, favoriteName = displayName)
            return
        }

        fetchSearch(resolvedQuery)
    }

    private fun fetchSearch(resolvedQuery: String, favoriteName: String? = null) {
        // Adiciona a mensagem do usuário ao chat
        addUserMessage(resolvedQuery)

        _uiState.update { it.copy(isLoading = true, isStreaming = false, error = null, isSuggestionMode = false) }
        
        viewModelScope.launch {
            try {
                val currentAiMessageId = "ai_${kotlin.random.Random.nextLong()}"
                var fullResponseText = ""
                var lastChunk: ChatResponse? = null

                // Consome o stream da IA
                apiClient.sendChatMessageStream(resolvedQuery.trim()).collect { chunk ->
                    lastChunk = chunk
                    val fragment = chunk.text ?: chunk.response ?: ""
                    
                    // Limpa tags internas da IA que não devem aparecer para o usuário
                    val cleanedFragment = fragment.replace("[[CONFIRM_ORDER]]", "")
                    fullResponseText += cleanedFragment

                    _uiState.update { state ->
                        val messages = state.chatMessages.toMutableList()
                        val existingIndex = messages.indexOfFirst { it.id == currentAiMessageId }
                        
                        val updatedMessage = if (existingIndex != -1) {
                            messages[existingIndex].copy(
                                text = fullResponseText,
                                restaurants = if (chunk.restaurantResults.isNotEmpty()) chunk.restaurantResults else messages[existingIndex].restaurants,
                                products = if (chunk.products.isNotEmpty()) chunk.products else (if (chunk.productResults.isNotEmpty()) chunk.productResults else messages[existingIndex].products)
                            )
                        } else {
                            ChatMessage(
                                id = currentAiMessageId,
                                type = ChatMessageType.AI,
                                text = fullResponseText,
                                restaurants = chunk.restaurantResults,
                                products = if (chunk.products.isNotEmpty()) chunk.products else chunk.productResults
                            )
                        }

                        if (existingIndex != -1) {
                            messages[existingIndex] = updatedMessage
                        } else {
                            messages.add(updatedMessage)
                        }
                        
                        state.copy(
                            chatMessages = messages.takeLast(20),
                            aiReply = fullResponseText,
                            isStreaming = true,
                            isLoading = false // Oculta o indicador de "pensando" assim que o texto começa a chegar
                        )
                    }

                    // Se detectar confirmação imediata no chunk, podemos processar logo
                    if (chunk.orderConfirmed) {
                        // O flow será cancelado pelo return@collect (implícito no loop se quisermos interromper)
                        // Mas aqui apenas marcamos para processar ao fim do stream
                    }
                }

                // Processamento pós-stream
                val finalResponse = lastChunk ?: return@launch
                
                _uiState.update { it.copy(isStreaming = false) }

                // Verifica se o pedido foi confirmado pela IA (Master Final Object)
                if (finalResponse.orderConfirmed || fullResponseText.contains("[[CONFIRM_ORDER]]")) {
                    // Garante que o texto final não tenha a tag se ela veio no acumulado
                    val finalCleanedText = fullResponseText.replace("[[CONFIRM_ORDER]]", "").trim()
                    
                    _uiState.update { state ->
                        val messages = state.chatMessages.toMutableList()
                        val idx = messages.indexOfFirst { it.id == currentAiMessageId }
                        if (idx != -1) {
                            messages[idx] = messages[idx].copy(text = finalCleanedText)
                        }
                        state.copy(chatMessages = messages, aiReply = finalCleanedText)
                    }

                    handleOrderConfirmation(finalResponse.copy(response = finalCleanedText))
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                // Lógica de "ver todos" ou resultados específicos
                var aiRestaurantResults: List<Restaurant> = finalResponse.restaurantResults
                if (resolvedQuery.equals("ver todos", ignoreCase = true)
                    || resolvedQuery.equals("restaurantes", ignoreCase = true)
                    || resolvedQuery.contains("todos os restaurantes", ignoreCase = true)
                ) {
                    try {
                        val all = apiClient.getAllRestaurants()
                        if (all.isNotEmpty()) aiRestaurantResults = all
                    } catch (e: Exception) {
                        println("⚠️ Falha ao buscar /restaurants: ${e.message}")
                    }
                }

                val aiProductResults = if (finalResponse.products.isNotEmpty()) finalResponse.products else finalResponse.productResults

                _uiState.update { state ->
                    // Atualiza o Home se necessário
                    val updatedAllRestaurants = if (
                        resolvedQuery.equals("ver todos", ignoreCase = true) &&
                        aiRestaurantResults.isNotEmpty()
                    ) aiRestaurantResults else state.allRestaurants

                    // Atualiza a última mensagem com os resultados finais consolidados
                    val messages = state.chatMessages.toMutableList()
                    val idx = messages.indexOfFirst { it.id == currentAiMessageId }
                    if (idx != -1) {
                        messages[idx] = messages[idx].copy(
                            restaurants = aiRestaurantResults,
                            products = aiProductResults
                        )
                    }

                    state.copy(
                        isLoading = false,
                        chatMessages = messages,
                        restaurantResults = aiRestaurantResults,
                        productResults = aiProductResults,
                        textInput = "",
                        lastSearchQuery = resolvedQuery,
                        allRestaurants = updatedAllRestaurants,
                        showSearchTypeSheet = aiRestaurantResults.isNotEmpty() && aiProductResults.isNotEmpty()
                    )
                }
                
                saveChatMessages()

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
                currentTab = MainTab.AI, // Mantém no Chat para exibir a Sacola IA integrada
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
            val existing = currentState.cartItems.find { it.gid == product.gid }
            val updatedCart = if (existing != null) {
                currentState.cartItems.map {
                    if (it.gid == product.gid) it.copy(quantity = it.quantity + product.quantity)
                    else it
                }
            } else {
                currentState.cartItems + product
            }
            currentState.copy(
                cartItems = updatedCart,
                cartRestaurantGid = product.restaurant_gid,
                isAiCartFlow = false
            )
        }
        product.restaurant_gid?.let { fetchCompanyByGid(it) }
    }

    private fun fetchCompanyByGid(gid: String) {
        viewModelScope.launch {
            val company = apiClient.getCompanyByGid(gid)
            if (company == null) return@launch
            
            val restaurantMetadata = Restaurant(
                gid = company.gid,
                name = company.name,
                category = company.category,
                image_url = company.imageUrl,
                products = company.products,
                latitude = company.latitude,
                longitude = company.longitude
            )

            _uiState.update { state ->
                val updatedCartRestaurants = if (state.cartRestaurants.none { it.gid == gid }) {
                    state.cartRestaurants + restaurantMetadata
                } else {
                    state.cartRestaurants.map { if (it.gid == gid) restaurantMetadata else it }
                }

                val shouldUpdateRestaurant =
                    state.selectedRestaurant == null ||
                    state.selectedRestaurant?.gid == company.gid ||
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
                cartRestaurantGid = null,
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
                .map { if (it.gid == product.gid) it.copy(quantity = it.quantity - 1) else it }
                .filter { it.quantity > 0 }
            
            // Atualiza a lista de metadados se um restaurante não tiver mais produtos
            val remainingRestaurantGids = updatedCart.mapNotNull { it.restaurant_gid }.toSet()
            val updatedCartRestaurants = currentState.cartRestaurants.filter { it.gid in remainingRestaurantGids }
            
            val newRestaurantGid = if (updatedCart.isEmpty()) null else {
                // Se o restaurante removido era o "principal", escolhe o próximo disponível
                if (product.restaurant_gid == currentState.cartRestaurantGid && product.restaurant_gid !in remainingRestaurantGids) {
                    remainingRestaurantGids.firstOrNull()
                } else {
                    currentState.cartRestaurantGid
                }
            }
            
            currentState.copy(
                cartItems = updatedCart, 
                cartRestaurantGid = newRestaurantGid,
                cartRestaurants = updatedCartRestaurants
            )
        }
    }

    fun clearCart() {
        _uiState.update { 
            it.copy(
                cartItems = emptyList(), 
                cartRestaurantGid = null,
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
        restaurantGid: String
    ): DeliveryFeeResponse {
        return apiClient.getDeliveryFee(
            DeliveryFeeRequest(
                customer_latitude = customerLat,
                customer_longitude = customerLon,
                restaurant_latitude = restaurantLat,
                restaurant_longitude = restaurantLon,
                restaurant_gid = restaurantGid
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

    fun checkoutWithAddress(
        address: Address, 
        deliveryFee: Double = 0.0, 
        serviceFee: Double = 0.0, 
        deliveryType: String = "delivery",
        deliveryFeesMap: Map<String, Double> = emptyMap()
    ) {
        val currentState = _uiState.value
        if (currentState.userProfile.name.isBlank()) {
            _uiState.update { it.copy(error = "Por favor, preencha seu Nome no Perfil.") }
            onTabSelected(MainTab.PROFILE)
            return
        }
        if (currentState.cartItems.isEmpty()) return
        // Store the delivery type chosen in the summary
        _uiState.update { 
            it.copy(
                pendingDeliveryType = deliveryType,
                pendingDeliveryFee = deliveryFee,
                pendingServiceFee = serviceFee,
                selectedAddressForCheckout = address
            ) 
        }
        val hasSavedPaymentMethods = currentState.userProfile.savedPaymentMethods.isNotEmpty()
        if (hasSavedPaymentMethods) {
            showPaymentConfirmForAddress(address, deliveryFee, serviceFee, deliveryFeesMap)
        } else {
            showSavePaymentSheetForAddress(address, deliveryFee, serviceFee, deliveryFeesMap)
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

    fun showPaymentConfirmForAddress(address: Address, deliveryFee: Double = 0.0, serviceFee: Double = 0.0, deliveryFeesMap: Map<String, Double> = emptyMap()) {
        _uiState.update {
            it.copy(
                isAddressSheetVisible = false,
                showPaymentConfirmSheet = true,
                selectedAddressForCheckout = address,
                pendingDeliveryFee = deliveryFee,
                pendingServiceFee = serviceFee,
                pendingDeliveryFeesMap = deliveryFeesMap
            )
        }
    }

    fun showSavePaymentSheetForAddress(address: Address, deliveryFee: Double = 0.0, serviceFee: Double = 0.0, deliveryFeesMap: Map<String, Double> = emptyMap()) {
        _uiState.update {
            it.copy(
                isAddressSheetVisible = false,
                showSavePaymentSheet = true,
                selectedAddressForCheckout = address,
                pendingDeliveryFee = deliveryFee,
                pendingServiceFee = serviceFee,
                pendingDeliveryFeesMap = deliveryFeesMap
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
        val deliveryFeesMap = currentState.pendingDeliveryFeesMap
        
        _uiState.update {
            it.copy(
                selectedAddressForCheckout = null,
                pendingCheckoutSavePaymentMethod = false,
                pendingDeliveryFee = 0.0,
                pendingServiceFee = 0.0,
                pendingDeliveryFeesMap = emptyMap()
            )
        }
        confirmCheckout(address, savePaymentMethod, deliveryType, deliveryFee, serviceFee, deliveryFeesMap)
    }

    fun dismissAddressSheet() {
        _uiState.update { it.copy(isAddressSheetVisible = false) }
    }

    fun confirmCheckout(
        selectedAddress: Address, 
        savePaymentMethod: Boolean = false, 
        deliveryType: String = "", 
        deliveryFee: Double = 0.0, 
        serviceFee: Double = 0.0,
        deliveryFeesMap: Map<String, Double> = emptyMap()
    ) {
        _uiState.update {
            it.copy(
                isAddressSheetVisible = false,
                isLoading = true,
                error = null,
                pendingSavePaymentMethod = savePaymentMethod
            )
        }

        val currentState = _uiState.value
        val itemsByRestaurantGid = currentState.cartItems.groupBy { it.restaurant_gid }

        if (itemsByRestaurantGid.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, error = "Sua sacola está vazia.") }
            return
        }

        if (currentState.userProfile.name.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Por favor, preencha seu Nome no Perfil.") }
            onTabSelected(MainTab.PROFILE)
            return
        }

        viewModelScope.launch {
            val masterGid = generateGid("mst")
            val trackingCode = generateTrackingCode()
            val subOrders = mutableListOf<SubOrderRequest>()
            
            for ((restaurantGid, products) in itemsByRestaurantGid) {
                if (restaurantGid == null) continue
                
                // 1. Obter metadados do restaurante
                var restaurant = currentState.cartRestaurants.find { it.gid == restaurantGid }
                if (restaurant == null) {
                    val company = apiClient.getCompanyByGid(restaurantGid)
                    if (company != null) {
                        restaurant = Restaurant(
                            gid = company.gid,
                            name = company.name,
                            category = company.category,
                            image_url = company.imageUrl,
                            products = company.products,
                            latitude = company.latitude,
                            longitude = company.longitude
                        )
                    }
                }

                if (restaurant == null) continue

                // 2. Preparar itens do sub-pedido
                val orderItems = products.map { product ->
                    OrderItemRequest(
                        product_gid = product.gid,
                        quantity = product.quantity,
                        observation = null,
                        product_name = product.name,
                        price = product.price,
                        image_url = product.image_url,
                        description = product.description
                    )
                }

                val subBaseTime = products.maxOfOrNull { p ->
                    p.preparationTime.filter { it.isDigit() || it == '.' }.toDoubleOrNull()?.toInt() ?: 0
                } ?: 0

                val subDeliveryFee = deliveryFeesMap[restaurantGid] ?: (deliveryFee / itemsByRestaurantGid.size)

                subOrders.add(
                    SubOrderRequest(
                        gid = generateGid("sub"),
                        orderGid = masterGid,
                        restaurantGid = restaurant.gid,
                        restaurantName = restaurant.name,
                        restaurantImageUrl = restaurant.image_url,
                        restaurantCategory = restaurant.category,
                        items = orderItems,
                        deliveryFee = subDeliveryFee,
                        baseTime = subBaseTime
                    )
                )
            }

            if (subOrders.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao processar itens da sacola.") }
                return@launch
            }

            // 3. Criar Master Request
            val masterRequest = OrderRequest(
                gid = masterGid,
                user_id = currentState.userProfile.id,
                user_name = currentState.userProfile.name,
                user_address = selectedAddress.address,
                user_phone = currentState.userProfile.phone,
                save_payment_method = savePaymentMethod,
                search_query = currentState.lastSearchQuery,
                tracking_code = trackingCode,
                deliveryType = deliveryType,
                deliveryLatitude = selectedAddress.latitude,
                deliveryLongitude = selectedAddress.longitude,
                totalDeliveryFee = deliveryFee,
                totalServiceFee = serviceFee,
                subOrders = subOrders
            )

            // 4. Iniciar checkout único
            try {
                val sessionResponse = apiClient.initiateCheckout(masterRequest)
                if (sessionResponse != null) {
                    val orderId = sessionResponse.order_id
                    if (orderId != null) {
                        // Guardar mapeamentos para o pedido principal
                        profileRepository.saveOrderSearchQuery(orderId.toString(), currentState.lastSearchQuery)
                        
                        // Para múltiplos restaurantes, o gid do restaurante no nível superior pode ser do primeiro ou nulo
                        // Aqui usamos o primeiro restaurante como referência para a UI se necessário
                        subOrders.firstOrNull()?.let { firstSub ->
                            profileRepository.saveOrderRestaurantGid(orderId.toString(), firstSub.restaurantGid)
                        }
                    }

                    if (sessionResponse.auto_paid) {
                        finalizeOrderState(isSuccess = true, checkoutUrl = null, autoPaid = true)
                    } else if (sessionResponse.clientSecret != null) {
                        // Inicializa o SDK com a chave vinda do servidor
                        val key = sessionResponse.publishableKey 
                            ?: "pk_test_51TyC5WEv3cCEwtfr67LPi8l5lrzC9XzOKx5C9haQkDTBSeWbEyjSbrcudEFeR6OAblPv2rHq1WQQmnGIJpPdAJJU00IOw8BsSu"
                        
                        println("💳 Iniciando Stripe com chave: ${key.takeLast(10)}")
                        stripePaymentManager.init(key)

                        // Inicia o SDK nativo da Stripe
                        stripePaymentManager.presentPaymentSheet(
                            paymentIntentClientSecret = sessionResponse.clientSecret,
                            customerId = sessionResponse.customerId,
                            ephemeralKeySecret = sessionResponse.ephemeralKey,
                            onResult = { result ->
                                when (result) {
                                    is StripePaymentResult.Completed -> {
                                        finalizeOrderState(isSuccess = true, checkoutUrl = null, autoPaid = false)
                                    }
                                    is StripePaymentResult.Canceled -> {
                                        _uiState.update { it.copy(isLoading = false, error = "Pagamento cancelado.") }
                                    }
                                    is StripePaymentResult.Failed -> {
                                        _uiState.update { it.copy(isLoading = false, error = "Erro no pagamento: ${result.error}") }
                                    }
                                }
                            }
                        )
                    } else if (sessionResponse.url != null) {
                        // Fallback para WebView se necessário
                        finalizeOrderState(isSuccess = true, checkoutUrl = sessionResponse.url, autoPaid = false)
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Erro ao iniciar sessão de pagamento.") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Erro ao iniciar sessão de pagamento.") }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, error = "Erro crítico: ${e.message}") }
            }
        }
    }

    private fun finalizeOrderState(isSuccess: Boolean, checkoutUrl: String?, autoPaid: Boolean) {
        val currentState = _uiState.value
        val greeting = buildGreeting(currentState.userProfile.name)
        
        _uiState.update {
            it.copy(
                isLoading = false,
                isProcessingAutoPayment = false,
                cartItems = if (isSuccess) emptyList() else it.cartItems,
                cartRestaurantGid = if (isSuccess) null else it.cartRestaurantGid,
                cartRestaurants = if (isSuccess) emptyList() else it.cartRestaurants,
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
                currentTab = if (checkoutUrl != null) currentState.currentTab else MainTab.ORDERS,
                checkoutUrl = checkoutUrl,
                error = if (!isSuccess) "Falha ao processar o pedido." else null,
                pendingSavePaymentMethod = false,
                orderJustPlaced = autoPaid,
                isAiCartFlow = false
            )
        }
        
        if (autoPaid) {
            refreshOrders()
            startBackgroundPolling(currentState.userProfile.id)
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

            // Guardar mapeamento productName -> productGid para avaliações futuras
            if (!orderId.isNullOrBlank() && cartItems.isNotEmpty()) {
                viewModelScope.launch {
                    val productGidMap = cartItems.map { it.name to it.gid }
                    profileRepository.saveOrderProductGids(orderId, productGidMap)
                    val restaurantGid = _uiState.value.cartRestaurantGid
                    if (restaurantGid != null) {
                        profileRepository.saveOrderRestaurantGid(orderId, restaurantGid)
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    checkoutUrl = null,
                    cartItems = emptyList(),
                    cartRestaurantGid = null,
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
                    val order = updatedOrders.find { it.id == orderId }

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
                                        cartRestaurantGid = null,
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
                // Tenta encontrar um item existente (mesmo GID ou mesmo Nome + Restaurante)
                val existingIndex = cartItems.indexOfFirst { 
                    (it.gid.isNotBlank() && it.gid == product.gid) || 
                    (it.name == product.name && it.restaurant_gid == product.restaurant_gid)
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
                cartRestaurantGid = products.firstOrNull()?.restaurant_gid ?: currentState.cartRestaurantGid,
                isAiCartFlow = true 
            )
        }

        // Busca metadados para todos os restaurantes únicos na lista de produtos
        val uniqueRestaurantGids = products.mapNotNull { it.restaurant_gid }.distinct()
        uniqueRestaurantGids.forEach { gid ->
            fetchCompanyByGid(gid)
        }
    }
}