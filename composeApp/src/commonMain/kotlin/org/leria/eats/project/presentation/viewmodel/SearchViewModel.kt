package org.leria.eats.project.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.leria.eats.project.domain.model.Address
import org.leria.eats.project.domain.model.ChatMessage
import org.leria.eats.project.domain.model.ChatMessageType
import org.leria.eats.project.domain.model.ChatReply
import org.leria.eats.project.domain.model.CheckoutSession
import org.leria.eats.project.domain.model.DeliveryFeeQuote
import org.leria.eats.project.domain.model.LiveEvent
import org.leria.eats.project.domain.model.Order
import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.domain.model.Restaurant
import org.leria.eats.project.domain.repository.AiChatRepository
import org.leria.eats.project.domain.repository.ChatHistoryRepository
import org.leria.eats.project.domain.repository.LiveConversationRepository
import org.leria.eats.project.domain.repository.OrderRepository
import org.leria.eats.project.domain.repository.ProfileRepository
import org.leria.eats.project.domain.repository.RestaurantRepository
import org.leria.eats.project.domain.repository.SpeechRepository
import org.leria.eats.project.domain.usecase.CartRules
import org.leria.eats.project.domain.usecase.PlaceOrderResult
import org.leria.eats.project.domain.usecase.PlaceOrderUseCase
import org.leria.eats.project.domain.usecase.RateOrderItemUseCase
import org.leria.eats.project.domain.usecase.SaveUserProfileUseCase
import org.leria.eats.project.domain.usecase.SyncSavedPaymentMethodsUseCase
import org.leria.eats.project.domain.usecase.buildAddToCartPrompt
import org.leria.eats.project.domain.usecase.buildPersonalizedSuggestionsPrompt
import org.leria.eats.project.domain.usecase.mesclarSugestoesDaLigacao
import org.leria.eats.project.domain.usecase.prepareTextForSpeech
import org.leria.eats.project.domain.voice.VoiceAudioInput
import org.leria.eats.project.domain.voice.VoiceAudioOutput
import org.leria.eats.project.payment.StripePaymentManager
import org.leria.eats.project.payment.StripePaymentResult
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.SearchUiState

/**
 * Estado e ações das telas principais (chat da IA, explorar, sacola, pedidos, perfil e ligação de
 * voz). Depende só de portas do domínio (repositórios, use cases, áudio) — as implementações
 * (Ktor, DataStore, WebSocket, AudioTrack/AVAudioEngine) são injetadas pelo Koin (di/).
 */
class SearchViewModel(
    private val restaurantRepository: RestaurantRepository,
    private val aiChatRepository: AiChatRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val orderRepository: OrderRepository,
    private val profileRepository: ProfileRepository,
    private val speechRepository: SpeechRepository,
    private val liveConversationRepository: LiveConversationRepository,
    private val liveAudioRecorder: VoiceAudioInput,
    private val liveAudioPlayer: VoiceAudioOutput,
    private val stripePaymentManager: StripePaymentManager,
    private val placeOrder: PlaceOrderUseCase,
    private val rateOrderItemUseCase: RateOrderItemUseCase,
    private val saveUserProfile: SaveUserProfileUseCase,
    private val syncSavedPaymentMethods: SyncSavedPaymentMethodsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private fun buildGreeting(name: String): String {
        // Saudação removida - retorna string vazia
        return ""
    }

    private fun buildWelcomeMessage(): String {
        return "Crie o seu perfil para começar a pedir. Leva menos de 1 minuto! 🍽️"
    }

    private var initialRestaurantsLoaded = false

    /**
     * Carrega todos os restaurantes e guarda em [SearchUiState.allRestaurants] para o Home.
     * Só marca [initialRestaurantsLoaded] em caso de sucesso — se a chamada falhar (rede
     * instável, servidor reiniciando, etc.), a próxima tentativa (ex.: reabrir a aba
     * Explorar via [onTabSelected]) tenta de novo, em vez de deixar a lista vazia pro
     * resto da sessão do app.
     */
    private fun loadAllRestaurantsForHome() {
        viewModelScope.launch {
            try {
                val restaurants = restaurantRepository.getAllRestaurants()
                if (restaurants.isNotEmpty()) {
                    _uiState.update { it.copy(allRestaurants = restaurants) }
                    initialRestaurantsLoaded = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        viewModelScope.launch {
            profileRepository.userProfile.collect { profile ->
                // Check if user is registered (has name and at least one address)
                val isUserRegistered = profile.name.isNotBlank() && profile.addresses.isNotEmpty()

                if (isUserRegistered) {
                    // User is registered, show normal greeting
                    val greeting = buildGreeting(profile.name)
                    _uiState.update { it.copy(userProfile = profile, aiReply = greeting) }
                    // Load all restaurants automatically on first open
                    if (!initialRestaurantsLoaded) {
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
            profileRepository.userProfile.first { it.id.isNotBlank() }
            refreshOrdersInternal()
        }
        startStatusPolling()
        observeOrderSearchQueries()
        observeOrderItemRatings()
        observeOrderProductGids()
        observeOrderRestaurantGids()
        observeChatMessages()
    }


    private fun observeChatMessages() {
        viewModelScope.launch {
            chatHistoryRepository.messages.collect { messages ->
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


    private fun observeOrderSearchQueries() {
        viewModelScope.launch {
            orderRepository.orderSearchQueries.collect { queries ->
                _uiState.update { it.copy(orderSearchQueries = queries) }
            }
        }
    }

    private fun observeOrderItemRatings() {
        viewModelScope.launch {
            orderRepository.orderItemRatings.collect { ratings ->
                _uiState.update { it.copy(orderItemRatings = ratings) }
            }
        }
    }

    private fun observeOrderProductGids() {
        viewModelScope.launch {
            orderRepository.orderProductGids.collect { productGids ->
                _uiState.update { it.copy(orderProductGids = productGids) }
            }
        }
    }

    private fun observeOrderRestaurantGids() {
        viewModelScope.launch {
            orderRepository.orderRestaurantGids.collect { restaurantGids ->
                _uiState.update { it.copy(orderRestaurantGids = restaurantGids) }
            }
        }
    }

    fun rateOrderItem(orderId: String, productGid: String, restaurantGid: String, productName: String, rating: Int) {
        viewModelScope.launch {
            rateOrderItemUseCase(orderId, productGid, restaurantGid, productName, rating)
        }
    }


    private fun startStatusPolling() {
        viewModelScope.launch {
            while (true) {
                val userId = _uiState.value.userProfile.id
                val tab = _uiState.value.currentTab
                if (userId.isNotBlank() && tab == MainTab.ORDERS) {
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
            val updatedOrders = orderRepository.getCustomerOrders(userId)
            _uiState.update { it.copy(orderHistory = updatedOrders) }

            val selectedGid = _uiState.value.selectedOrder?.gid
            if (selectedGid != null) {
                val updatedSelected = updatedOrders.find { it.gid == selectedGid }
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
            saveUserProfile(_uiState.value.userProfile.id, name, email, phone, addresses, photoUrl, allergies, lifestyles)
        }
    }

    fun onQueryChange(text: String) {
        // Digitação manual: a resposta da IA não deve ser falada.
        _uiState.update { it.copy(textInput = text, lastInputWasVoice = false) }
    }

    fun updateInputFromVoice(text: String) {
        if (text.isNotBlank()) {
            _uiState.update { it.copy(textInput = text, lastInputWasVoice = true) }
        }
    }

    fun selectRestaurant(restaurant: Restaurant) {
        if (restaurant.products.isEmpty()) {
            // Se o restaurante não tem produtos carregados, buscar pela API
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val resolvedRestaurant = restaurantRepository.getRestaurant(restaurant.gid) ?: restaurant
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
                val resolvedRestaurant = restaurantRepository.getRestaurant(restaurant.gid) ?: restaurant

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
        val sessaoLigacao = if (pedidoMontadoPorLigacao) aiChatRepository.currentSessionId() else null
        pedidoMontadoPorLigacao = false
        val currentState = _uiState.value
        viewModelScope.launch {
            // Notifica o servidor para reiniciar a sessão se houver uma ativa
            // Na ligação de voz currentAiSessionId é nulo: usa a sessão da ligação (a mesma do chat).
            (currentState.currentAiSessionId ?: sessaoLigacao)?.let { sessionId ->
                println("🧹 Reiniciando sessão da IA no servidor: $sessionId")
                aiChatRepository.restartSession(sessionId)
            }
            chatHistoryRepository.clear()
        }
        val greeting = buildGreeting(_uiState.value.userProfile.name)
        _uiState.update {
            it.copy(
                restaurantResults = emptyList(),
                productResults = emptyList(),
                aiReply = greeting,
                textInput = "",
                error = null,
                currentAiSessionId = null, // Reseta o ID da sessão localmente
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
        val cleanedText = text.replace(Regex("\\n{3,}"), "\n\n").trim()
        val message = ChatMessage(
            id = "ai_${kotlin.random.Random.nextLong()}",
            type = ChatMessageType.AI,
            text = cleanedText,
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
            chatHistoryRepository.save(_uiState.value.chatMessages)
        }
    }


    fun sendSearch() {
        val currentQuery = _uiState.value.textInput

        if (currentQuery.isBlank()) {
            return
        }

        // Prevent duplicate calls while a search is already in progress
        if (_uiState.value.isLoading) return

        // Clear textInput immediately to prevent duplicate calls (e.g. voice race condition)
        _uiState.update { it.copy(textInput = "") }

        val resolvedQuery = currentQuery.trim()
        fetchSearch(resolvedQuery)
    }

    /**
     * Envia ao chat da IA a escolha de um produto sugerido, como se o usuário tivesse digitado.
     * A quantidade vem explícita na mensagem para a IA adicionar direto ao carrinho (usuário
     * escolhe no modal de quantidade antes de chamar isto), sem perguntar de novo — ver regra
     * "Exceção" em gemini_sales_service.py.
     */
    fun chooseProductInChat(product: Product, quantity: Int = 1) {
        val pedido = buildAddToCartPrompt(product, quantity)
        // Durante a ligação de voz o pedido vai pela própria ligação: por baixo dos panos é pedir o
        // produto à IA, com a quantidade escolhida no modal — ela adiciona à sacola e responde falando.
        if (_uiState.value.isLiveConversationActive) {
            pedirNaLigacao(pedido)
            return
        }
        if (_uiState.value.isLoading) return
        fetchSearch(pedido)
    }

    /** Manda um pedido escrito para a IA DENTRO da ligação de voz (mesma sessão Live, então a resposta
     * volta em áudio e o carrinho é o mesmo). Corta a fala em andamento: o servidor interrompe a IA. */
    private fun pedirNaLigacao(pedido: String) {
        liveAudioPlayer.stop()
        _uiState.update { it.copy(isLiveAiSpeaking = false, isLiveAiThinking = true) }
        viewModelScope.launch { runCatching { liveConversationRepository.sendText(pedido) } }
    }

    /** Envia um prompt pré-definido ao chat da IA (ex.: atalhos rápidos como "Caixa Surpresa"). */
    fun sendQuickPrompt(prompt: String) {
        if (_uiState.value.isLoading) return
        fetchSearch(prompt)
    }

    /** Pede sugestões à IA considerando a personalização alimentar salva localmente no perfil. */
    fun requestPersonalizedSuggestions() {
        if (_uiState.value.isLoading) return
        fetchSearch(buildPersonalizedSuggestionsPrompt(_uiState.value.userProfile))
    }

    fun clearPendingVoiceAudio() {
        _uiState.update { it.copy(pendingVoiceAudio = null) }
    }

    private fun fetchSearch(resolvedQuery: String) {
        // Turno por voz (microfone): não revelamos o texto aos poucos como no streaming normal —
        // esperamos a resposta completa, sintetizamos a fala e só então mostramos a mensagem,
        // pra ela aparecer no exato momento em que a IA começa a falar (ver bloco pós-stream).
        val isVoiceTurn = _uiState.value.lastInputWasVoice

        // Adiciona a mensagem do usuário ao chat
        addUserMessage(resolvedQuery)

        _uiState.update { it.copy(isLoading = true, isStreaming = false, error = null, isSuggestionMode = false) }

        viewModelScope.launch {
            try {
                val currentAiMessageId = "ai_${kotlin.random.Random.nextLong()}"
                var fullResponseText = ""
                var lastChunk: ChatReply? = null

                // Consome o stream da IA
                aiChatRepository.streamMessage(resolvedQuery.trim()).collect { chunk ->
                    lastChunk = chunk
                    val fragment = chunk.fragment
                    
                    // Limpa tags internas e reduz espaços em branco excessivos
                    val cleanedFragment = fragment.replace("[[CONFIRM_ORDER]]", "")
                    fullResponseText += cleanedFragment
                    
                    // Tratamento final para exibição: remove triplas quebras de linha e trim no início/fim
                    val displayResponseText = fullResponseText
                        .replace(Regex("\\n{3,}"), "\n\n")
                        .trimStart()

                    if (isVoiceTurn) {
                        // Só acompanha a sessão/streaming; a mensagem em si só é revelada
                        // depois, junto com o áudio já sintetizado (ver pós-stream abaixo).
                        _uiState.update { state ->
                            state.copy(
                                isStreaming = true,
                                currentAiSessionId = chunk.sessionId ?: state.currentAiSessionId
                            )
                        }
                    } else {
                        _uiState.update { state ->
                            val messages = state.chatMessages.toMutableList()
                            val existingIndex = messages.indexOfFirst { it.id == currentAiMessageId }

                            val updatedMessage = if (existingIndex != -1) {
                                messages[existingIndex].copy(
                                    text = displayResponseText,
                                    restaurants = chunk.restaurants.ifEmpty { messages[existingIndex].restaurants },
                                    products = chunk.products.ifEmpty { messages[existingIndex].products }
                                )
                            } else {
                                ChatMessage(
                                    id = currentAiMessageId,
                                    type = ChatMessageType.AI,
                                    text = displayResponseText,
                                    restaurants = chunk.restaurants,
                                    products = chunk.products
                                )
                            }

                            if (existingIndex != -1) {
                                messages[existingIndex] = updatedMessage
                            } else {
                                messages.add(updatedMessage)
                            }

                            state.copy(
                                chatMessages = messages.takeLast(20),
                                aiReply = displayResponseText,
                                isStreaming = true,
                                isLoading = false, // Oculta o indicador de "pensando" assim que o texto começa a chegar
                                currentAiSessionId = chunk.sessionId ?: state.currentAiSessionId
                            )
                        }
                    }

                    // Se detectar sinal de mostrar sacola imediata no chunk
                    if (chunk.showCart) {
                        // Podemos processar logo ou aguardar o fim do stream
                    }
                }

                // Processamento pós-stream
                val finalResponse = lastChunk ?: return@launch

                _uiState.update { it.copy(isStreaming = false) }

                if (isVoiceTurn) {
                    // Sintetiza a fala ANTES de revelar a mensagem: assim o texto só aparece
                    // no chat no exato instante em que o áudio já está pronto pra tocar, sem
                    // ficar visível "mudo" à espera da síntese (era essa a causa do atraso
                    // perceptível entre mostrar a mensagem e a IA começar a falar).
                    val textoLimpo = fullResponseText.replace(Regex("\\n{3,}"), "\n\n").trim()
                    val textoParaFala = prepareTextForSpeech(textoLimpo)
                    val audioSintetizado = if (textoParaFala.isNotBlank()) {
                        runCatching { speechRepository.synthesize(textoParaFala) }.getOrNull()
                    } else null

                    _uiState.update { state ->
                        val messages = state.chatMessages.toMutableList()
                        val idx = messages.indexOfFirst { it.id == currentAiMessageId }
                        val msg = ChatMessage(id = currentAiMessageId, type = ChatMessageType.AI, text = textoLimpo)
                        if (idx != -1) messages[idx] = msg else messages.add(msg)
                        state.copy(
                            chatMessages = messages.takeLast(20),
                            aiReply = textoLimpo,
                            pendingVoiceAudio = audioSintetizado
                        )
                    }
                }

                // Verifica se deve mostrar a sacola IA integrada (Master Final Object ou Tag)
                if (finalResponse.showCart || fullResponseText.contains("[[CONFIRM_ORDER]]")) {
                    // Garante que o texto final não tenha a tag e remove brancos excessivos
                    val finalCleanedText = fullResponseText
                        .replace("[[CONFIRM_ORDER]]", "")
                        .replace(Regex("\\n{3,}"), "\n\n")
                        .trim()
                    
                    _uiState.update { state ->
                        val messages = state.chatMessages.toMutableList()
                        val idx = messages.indexOfFirst { it.id == currentAiMessageId }
                        if (idx != -1) {
                            messages[idx] = messages[idx].copy(text = finalCleanedText)
                        }
                        state.copy(
                            chatMessages = messages, 
                            aiReply = finalCleanedText,
                            currentAiSessionId = finalResponse.sessionId ?: state.currentAiSessionId
                        )
                    }

                    handleShowCart(finalResponse)
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                // Lógica de "ver todos" ou resultados específicos
                var aiRestaurantResults: List<Restaurant> = finalResponse.restaurants
                if (resolvedQuery.equals("ver todos", ignoreCase = true)
                    || resolvedQuery.equals("restaurantes", ignoreCase = true)
                    || resolvedQuery.contains("todos os restaurantes", ignoreCase = true)
                ) {
                    try {
                        val all = restaurantRepository.getAllRestaurants()
                        if (all.isNotEmpty()) aiRestaurantResults = all
                    } catch (e: Exception) {
                        println("⚠️ Falha ao buscar /restaurants: ${e.message}")
                    }
                }

                val aiProductResults = finalResponse.products

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
                        allRestaurants = updatedAllRestaurants
                    )
                }
                
                saveChatMessages()

            } catch (e: Exception) {
                // Mensagem amigável em PT-PT para o utilizador — o detalhe técnico
                // (e.message, muitas vezes em inglês/stacktrace) só vai para o log.
                e.printStackTrace()
                val errorMessage = "Ups! Não foi possível ligar aos nossos servidores neste momento. Verifique a sua ligação à internet e tente novamente daqui a pouco."
                addAiMessage(text = errorMessage)
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    /**
     * Processa a exibição da sacola integrada pela IA.
     * Sincroniza os produtos da IA com o carrinho e permite continuar editando.
     */
    private fun handleShowCart(chatResponse: ChatReply) {
        val aiCartProducts = chatResponse.cartProducts
        
        // Se a IA enviou uma lista de produtos na sacola, ela representa o estado ATUAL.
        updateAiCart(aiCartProducts)

        _uiState.update {
            it.copy(
                isLoading = false,
                currentTab = MainTab.AI, // Mantém no Chat para exibir a Sacola IA integrada
                isAiCartFlow = true
            )
        }
    }

    fun addToCart(product: Product) {
        // Exclusividade da Caixa Surpresa (ver CartRules) — só UX; o gate real é o checkout no servidor.
        if (CartRules.violatesSurpriseBoxExclusivity(_uiState.value.cartItems, product)) {
            _uiState.update {
                it.copy(
                    cartError = "Um pedido com Caixa Surpresa é exclusivo — não é possível " +
                        "adicionar outro produto. Finalize este pedido ou remova o item de " +
                        "Caixa Surpresa para continuar a comprar."
                )
            }
            return
        }

        _uiState.update { currentState ->
            val updatedCart = CartRules.addProduct(currentState.cartItems, product)
            currentState.copy(
                cartItems = updatedCart,
                cartRestaurantGid = product.restaurant_gid,
                // Mantém ou ativa o fluxo de Sacola IA se estiver na aba da IA
                isAiCartFlow = if (currentState.currentTab == MainTab.AI) true else currentState.isAiCartFlow
            )
        }
        product.restaurant_gid?.let { fetchCompanyByGid(it) }
    }

    private fun fetchCompanyByGid(gid: String) {
        viewModelScope.launch {
            val restaurantMetadata = restaurantRepository.getRestaurant(gid) ?: return@launch

            _uiState.update { state ->
                val updatedCartRestaurants = if (state.cartRestaurants.none { it.gid == gid }) {
                    state.cartRestaurants + restaurantMetadata
                } else {
                    state.cartRestaurants.map { if (it.gid == gid) restaurantMetadata else it }
                }

                val shouldUpdateRestaurant =
                    state.selectedRestaurant == null ||
                    state.selectedRestaurant?.gid == restaurantMetadata.gid ||
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
                val response = aiChatRepository.sendMessage(suggestionQuery)
                val label = if (lastQuery.isNotBlank()) lastQuery else "restaurantes"
                // Show results on HomeScreen – never auto-add to cart
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        aiReply = "Outras opções que também pode gostar, além de $label",
                        restaurantResults = response.restaurants,
                        productResults = response.products,
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
            val updatedCart = CartRules.removeOneUnit(currentState.cartItems, product)
            
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
        if (tab == MainTab.ORDERS) {
            val userId = _uiState.value.userProfile.id
            if (userId.isNotBlank()) {
                viewModelScope.launch { refreshOrdersInternal() }
            }
        }
        if (tab == MainTab.HOME && !initialRestaurantsLoaded) {
            loadAllRestaurantsForHome()
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

    fun toggleBottomNav() {
        _uiState.update { it.copy(isBottomNavVisible = !it.isBottomNavVisible) }
    }

    suspend fun getDeliveryFee(
        customerLat: Double,
        customerLon: Double,
        restaurantLat: Double,
        restaurantLon: Double,
        restaurantGid: String
    ): DeliveryFeeQuote = orderRepository.getDeliveryFee(customerLat, customerLon, restaurantLat, restaurantLon, restaurantGid)

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
        // Pedido montado por voz: "confirmar e pagar" limpa a conversa (só a conversa — o carrinho
        // continua, o checkout precisa dele).
        if (pedidoMontadoPorLigacao) clearSearch()
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

        if (currentState.cartItems.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, error = "Sua sacola está vazia.") }
            return
        }

        if (currentState.userProfile.name.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Por favor, preencha seu Nome no Perfil.") }
            onTabSelected(MainTab.PROFILE)
            return
        }

        // Sessão da IA no servidor: a do chat por texto (currentAiSessionId) ou, se o pedido foi
        // montado por ligação de voz, a sessão da ligação — currentAiSessionId só é preenchido pelo
        // chat por texto, então na voz ficava nulo e o carrinho da IA nunca era limpo no servidor.
        // Lido AQUI (antes do launch) porque clearSearch() zera pedidoMontadoPorLigacao logo em seguida.
        val sessaoIa = currentState.currentAiSessionId
            ?: if (pedidoMontadoPorLigacao) aiChatRepository.currentSessionId() else null
        viewModelScope.launch {
            try {
                val result = placeOrder(
                    cartItems = currentState.cartItems,
                    cartRestaurants = currentState.cartRestaurants,
                    profile = currentState.userProfile,
                    address = selectedAddress,
                    savePaymentMethod = savePaymentMethod,
                    deliveryType = deliveryType,
                    deliveryFee = deliveryFee,
                    serviceFee = serviceFee,
                    deliveryFeesMap = deliveryFeesMap,
                    searchQuery = currentState.lastSearchQuery,
                    aiSessionId = sessaoIa
                )
                when (result) {
                    is PlaceOrderResult.Started -> continueCheckout(result.session)
                    PlaceOrderResult.NoValidItems ->
                        _uiState.update { it.copy(isLoading = false, error = "Erro ao processar itens da sacola.") }
                    PlaceOrderResult.CheckoutUnavailable ->
                        _uiState.update { it.copy(isLoading = false, error = "Erro ao iniciar sessão de pagamento.") }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, error = "Erro crítico: ${e.message}") }
            }
        }
    }

    /** O servidor aceitou o pedido: segue para pago na hora, PaymentSheet da Stripe ou WebView. */
    private fun continueCheckout(session: CheckoutSession) {
        if (session.autoPaid) {
            finalizeOrderState(isSuccess = true, checkoutUrl = null, autoPaid = true)
        } else if (session.clientSecret != null) {
            // Inicializa o SDK com a chave vinda do servidor
            val key = session.publishableKey ?: ""

            println("💳 Iniciando Stripe com chave: ${key.takeLast(10)}")
            stripePaymentManager.init(key)

            // Inicia o SDK nativo da Stripe
            stripePaymentManager.presentPaymentSheet(
                paymentIntentClientSecret = session.clientSecret,
                customerId = session.customerId,
                ephemeralKeySecret = session.ephemeralKey,
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
        } else if (session.url != null) {
            // Fallback para WebView se necessário
            finalizeOrderState(isSuccess = true, checkoutUrl = session.url, autoPaid = false)
        } else {
            _uiState.update { it.copy(isLoading = false, error = "Erro ao iniciar sessão de pagamento.") }
        }
    }

    /** Pagamento concluído e o app indo para "Meus pedidos": limpa o chat da IA com a MESMA função do
     * botão de limpar conversa (clearSearch + clearCart), para voltar na aba da IA numa conversa nova. */
    private fun limparChatAposPagamento() {
        clearSearch()
        clearCart()
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
                currentTab = if (checkoutUrl != null) currentState.currentTab else MainTab.ORDERS,
                checkoutUrl = checkoutUrl,
                error = if (!isSuccess) "Falha ao processar o pedido." else null,
                pendingSavePaymentMethod = false,
                orderJustPlaced = autoPaid,
                isAiCartFlow = false
            )
        }

        // checkoutUrl == null: o app foi direto para "Pedidos" (com WebView o fim do pagamento chega em
        // onPaymentResult, que também limpa)
        if (isSuccess && checkoutUrl == null) limparChatAposPagamento()

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
                    orderRepository.saveOrderSearchQuery(orderId, pendingSearchQuery)
                }
            }

            // Guardar mapeamento productName -> productGid para avaliações futuras
            if (!orderId.isNullOrBlank() && cartItems.isNotEmpty()) {
                viewModelScope.launch {
                    val productGidMap = cartItems.map { it.name to it.gid }
                    orderRepository.saveOrderProductGids(orderId, productGidMap)
                    val restaurantGid = _uiState.value.cartRestaurantGid
                    if (restaurantGid != null) {
                        orderRepository.saveOrderRestaurantGid(orderId, restaurantGid)
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
                    currentTab = MainTab.ORDERS,
                    error = null,
                    pendingSavePaymentMethod = false,
                    orderJustPlaced = true // Trigger voice feedback
                )
            }
            limparChatAposPagamento()

            // Fetch saved payment methods if user chose to save
            if (shouldFetchMethods && userId.isNotBlank()) {
                viewModelScope.launch {
                    syncSavedPaymentMethods(userId)
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
            val success = orderRepository.updateOrderStatus(orderId, "Entregue")
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

    /**
     * Atualiza a sacola da IA substituindo os itens atuais pelos enviados pelo servidor.
     * Usado para permitir edição (remoção/alteração de quantidade) via conversa.
     */
    private fun updateAiCart(products: List<Product>) {
        // Descarta itens zerados e avisa no log se a IA misturou Caixa Surpresa com outros produtos.
        val filteredProducts = CartRules.sanitizeAiCart(products)
        if (filteredProducts.isEmpty()) return

        _uiState.update { currentState ->
            currentState.copy(
                cartItems = filteredProducts,
                cartRestaurantGid = filteredProducts.firstOrNull()?.restaurant_gid ?: currentState.cartRestaurantGid,
                isAiCartFlow = true 
            )
        }

        // Busca metadados para todos os restaurantes únicos na lista de produtos
        val uniqueRestaurantGids = filteredProducts.mapNotNull { it.restaurant_gid }.distinct()
        uniqueRestaurantGids.forEach { gid ->
            fetchCompanyByGid(gid)
        }
    }

    // ── Conversa de voz em tempo real (Gemini Live API) ──────────────────────
    // Modo ADICIONAL ao microfone (STT) e ao campo de texto já existentes — não
    // substitui nenhum dos dois. Usa a MESMA sessão (AiChatRepository.currentSessionId)
    // do chat por texto, então o carrinho/histórico continuam unificados entre os
    // três jeitos de montar um pedido. Ver services/gemini_live_bridge.py no
    // servidor para o protocolo de eventos (transcript/cart_updated/show_cart/
    // audio/turn_complete/error) e o motivo das escolhas de modelo/temperatura.

    // true depois de uma ligação de voz: ao confirmar o pagamento, a conversa da ligação é limpa
    // (mesma função do "limpar conversa" do chat). Zerado em clearSearch().
    private var pedidoMontadoPorLigacao = false

    private var liveConversationJob: Job? = null
    private var liveAudioSendJob: Job? = null

    fun startLiveConversation() {
        if (_uiState.value.isLiveConversationActive) return
        _uiState.update {
            it.copy(
                isLiveConversationActive = true,
                isLiveAiSpeaking = false,
                isLiveAiThinking = true, // até a saudação chegar
                liveSuggestedProducts = emptyList(),
                isMicMuted = true // a ligação sempre começa com o microfone mudo
            )
        }

        // Prepara a saída de áudio para a chamada (caminho de comunicação, alto-falante, volume
        // alinhado ao de mídia) ANTES de o microfone abrir — o cancelamento de eco precisa do modo
        // de áudio já definido quando a captura começa. Desfeito em stopLiveConversation().
        liveAudioPlayer.startCall()
        pedidoMontadoPorLigacao = true

        val sessionId = aiChatRepository.currentSessionId()
        val nomeUsuario = _uiState.value.userProfile.name

        liveConversationJob = viewModelScope.launch {
            launch {
                liveConversationRepository.events.collect { evento -> handleLiveEvent(evento) }
            }
            liveConversationRepository.connect(sessionId, nomeUsuario)
        }

        // Começa a mandar áudio do microfone assim que a ligação for aberta —
        // sendAudioChunk() não faz nada sozinho enquanto a sessão não conectar
        // (outgoing fica nulo até o LiveEvent.Connected, mas os frames anteriores
        // a isso são só descartados, sem erro).
        liveAudioSendJob = viewModelScope.launch {
            liveAudioRecorder.audioChunks().collect { chunk ->
                // Continua gravando (mantém o AudioRecord vivo, necessário pro
                // cancelamento de eco). Mudo NÃO descarta o chunk: manda silêncio
                // (zeros) no lugar — nenhum som real do microfone sai do aparelho, mas
                // o fluxo de frames continua. Descartar de vez deixava o servidor sem
                // receber NADA do app, e o watchdog dele (gemini_live_bridge.py) encerra
                // a ligação após 45s sem nenhum frame — como a ligação começa muda, ela
                // caía sozinha se o usuário demorasse a ativar o microfone. O silêncio
                // também deixa o detector de fala da Gemini fechar uma frase que o
                // usuário estava dizendo quando silenciou.
                liveConversationRepository.sendAudioChunk(
                    if (_uiState.value.isMicMuted) ByteArray(chunk.size) else chunk
                )
            }
        }
    }

    fun toggleMicMuted() {
        if (!_uiState.value.isLiveConversationActive) return
        _uiState.update { it.copy(isMicMuted = !it.isMicMuted) }
    }

    fun stopLiveConversation() {
        liveAudioSendJob?.cancel()
        liveAudioSendJob = null
        liveConversationJob?.cancel()
        liveConversationJob = null
        liveAudioPlayer.stop()
        // Devolve ao sistema o modo de áudio/roteamento/volume da chamada — as respostas faladas do
        // chat voltam a tocar como mídia, no volume de mídia do aparelho.
        liveAudioPlayer.endCall()
        _uiState.update {
            it.copy(
                isLiveConversationActive = false,
                isLiveAiSpeaking = false,
                isLiveAiThinking = false,
                liveSuggestedProducts = emptyList(),
                isMicMuted = false
            )
        }
        viewModelScope.launch { liveConversationRepository.disconnect() }
    }

    /** Ferramenta da IA rodou (sacola/sugestão): se ela ainda não começou a falar, está pensando. */
    private fun marcarIaPensando() {
        if (!_uiState.value.isLiveAiSpeaking) _uiState.update { it.copy(isLiveAiThinking = true) }
    }

    private var liveAiTranscriptBuffer = ""
    private var liveAiMessageId: String? = null

    private fun handleLiveEvent(evento: LiveEvent) {
        when (evento) {
            is LiveEvent.Transcript -> {
                if (evento.role == "user") {
                    addUserMessage(evento.text)
                    // Ouviu o usuário e ainda não está falando: está processando a resposta.
                    if (!_uiState.value.isLiveAiSpeaking) _uiState.update { it.copy(isLiveAiThinking = true) }
                } else {
                    // Acumula o texto da IA no MESMO padrão de streaming do chat por
                    // texto (isStreaming = true, atualiza a última mensagem por id),
                    // pra UI (ChatMessagesView) funcionar igual não importa a origem.
                    liveAiTranscriptBuffer += evento.text
                    val id = liveAiMessageId ?: "ai_live_${kotlin.random.Random.nextLong()}".also { liveAiMessageId = it }
                    _uiState.update { state ->
                        val messages = state.chatMessages.toMutableList()
                        val idx = messages.indexOfFirst { it.id == id }
                        val msg = ChatMessage(id = id, type = ChatMessageType.AI, text = liveAiTranscriptBuffer)
                        if (idx != -1) messages[idx] = msg else messages.add(msg)
                        state.copy(chatMessages = messages.takeLast(20), isStreaming = true, isLoading = false)
                    }
                }
            }
            is LiveEvent.CartUpdated -> {
                updateAiCart(evento.cart)
                marcarIaPensando() // a ferramenta rodou; a fala de resposta vem logo depois
            }
            is LiveEvent.ShowCart -> {
                _uiState.update { it.copy(isAiCartFlow = true) }
                marcarIaPensando()
            }
            is LiveEvent.ProductsSuggested -> {
                _uiState.update { it.copy(liveSuggestedProducts = mesclarSugestoesDaLigacao(it.liveSuggestedProducts, evento.products)) }
                marcarIaPensando()
            }
            is LiveEvent.Audio -> {
                liveAudioPlayer.play(evento.pcm)
                // Primeiro chunk de áudio de um turno = a IA começou a falar. Só volta a
                // "ouvindo" no turn_complete abaixo — é uma aproximação (o chunk já
                // recebido ainda leva um instante pra tocar de verdade), mas suficiente
                // pra UI acompanhar o ritmo da conversa.
                if (!_uiState.value.isLiveAiSpeaking) {
                    _uiState.update { it.copy(isLiveAiSpeaking = true, isLiveAiThinking = false) }
                }
            }
            is LiveEvent.TurnComplete -> {
                if (liveAiMessageId != null) saveChatMessages()
                liveAiTranscriptBuffer = ""
                liveAiMessageId = null
                _uiState.update { it.copy(isStreaming = false, isLiveAiSpeaking = false, isLiveAiThinking = false) }
            }
            is LiveEvent.Interrupted -> {
                // O usuário falou por cima da IA: a Gemini já parou de gerar, mas o áudio
                // que ela mandou mais rápido que o tempo real ainda está na fila do
                // player — sem limpar, a IA continuaria falando por cima dele até a fila
                // esvaziar. A fala cortada vira a mensagem final (mesmo fechamento do
                // turn_complete).
                liveAudioPlayer.stop()
                if (liveAiMessageId != null) saveChatMessages()
                liveAiTranscriptBuffer = ""
                liveAiMessageId = null
                _uiState.update { it.copy(isStreaming = false, isLiveAiSpeaking = false, isLiveAiThinking = false) }
            }
            is LiveEvent.Error -> {
                addAiMessage(text = evento.message)
                stopLiveConversation()
            }
            is LiveEvent.Connected -> { /* nada a fazer — já em isLiveConversationActive */ }
            is LiveEvent.Disconnected -> {
                // A ligação acabou pelo lado do servidor (ou caiu): faz a MESMA limpeza de um
                // "desligar". Antes só a tela era atualizada — o microfone continuava capturando e,
                // agora que a chamada muda o modo de áudio do sistema, o aparelho ficaria em modo de
                // comunicação (botões de volume presos no volume de chamada) até fechar o app.
                if (_uiState.value.isLiveConversationActive) {
                    stopLiveConversation()
                }
            }
        }
    }
}