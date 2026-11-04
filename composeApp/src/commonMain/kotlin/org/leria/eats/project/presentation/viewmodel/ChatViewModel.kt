package org.leria.eats.project.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.leria.eats.project.domain.model.ChatMessage
import org.leria.eats.project.domain.model.ChatMessageType
import org.leria.eats.project.domain.model.ChatReply
import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.domain.model.Restaurant
import org.leria.eats.project.domain.model.UserProfile
import org.leria.eats.project.domain.repository.AiChatRepository
import org.leria.eats.project.domain.repository.ChatHistoryRepository
import org.leria.eats.project.domain.repository.ProfileRepository
import org.leria.eats.project.domain.repository.RestaurantRepository
import org.leria.eats.project.domain.repository.SpeechRepository
import org.leria.eats.project.domain.usecase.buildAddToCartPrompt
import org.leria.eats.project.domain.usecase.buildPersonalizedSuggestionsPrompt
import org.leria.eats.project.domain.usecase.prepareTextForSpeech
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.state.CartRestaurantLoader
import org.leria.eats.project.presentation.state.CartStore
import org.leria.eats.project.presentation.state.CatalogStore
import org.leria.eats.project.presentation.state.ConversationState
import org.leria.eats.project.presentation.state.ConversationStore
import org.leria.eats.project.presentation.state.NavigationStore

data class ChatState(
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false, // Indica se a IA está enviando texto em tempo real
    // true quando o texto atual/último enviado veio do microfone (STT), e não de digitação.
    // Usado para restringir a voz da IA (TTS) a apenas quando o usuário usou o microfone ou
    // está em ligação de voz — não deve falar respostas a mensagens digitadas.
    val lastInputWasVoice: Boolean = false,
    // Áudio (PCM16) já sintetizado para a última resposta de um turno por voz, pronto pra
    // tocar sem espera adicional — ver fetchSearch(): sintetizamos ANTES de revelar o texto,
    // pra mensagem e voz aparecerem juntas. Consumido (e zerado) assim que tocado.
    val pendingVoiceAudio: ByteArray? = null,
    val error: String? = null
)

/** Chat da IA por texto e por microfone (STT): envio de mensagens, streaming e Sacola IA. */
class ChatViewModel(
    private val aiChatRepository: AiChatRepository,
    chatHistoryRepository: ChatHistoryRepository,
    private val restaurantRepository: RestaurantRepository,
    private val speechRepository: SpeechRepository,
    profileRepository: ProfileRepository,
    private val conversationStore: ConversationStore,
    private val cartStore: CartStore,
    private val cartRestaurantLoader: CartRestaurantLoader,
    private val catalogStore: CatalogStore,
    private val navigation: NavigationStore,
) : ViewModel() {

    val conversation: StateFlow<ConversationState> = conversationStore.state

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val userProfile: StateFlow<UserProfile> =
        profileRepository.userProfile.stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    init {
        viewModelScope.launch {
            chatHistoryRepository.messages.collect { messages ->
                if (messages.isNotEmpty()) conversationStore.update { it.copy(messages = messages) }
            }
        }
    }

    fun onQueryChange(text: String) {
        // Digitação manual: a resposta da IA não deve ser falada.
        conversationStore.setTextInput(text)
        _state.update { it.copy(lastInputWasVoice = false) }
    }

    fun updateInputFromVoice(text: String) {
        if (text.isNotBlank()) {
            conversationStore.setTextInput(text)
            _state.update { it.copy(lastInputWasVoice = true) }
        }
    }

    fun clearSearch() {
        conversationStore.clear(viewModelScope)
        _state.update { it.copy(error = null) }
    }

    fun addAiMessage(text: String, restaurants: List<Restaurant> = emptyList(), products: List<Product> = emptyList()) {
        conversationStore.addAiMessage(text, restaurants, products)
        saveChatMessages()
    }

    private fun saveChatMessages() {
        viewModelScope.launch { conversationStore.persist() }
    }

    fun sendSearch() {
        val currentQuery = conversationStore.state.value.textInput

        if (currentQuery.isBlank()) {
            return
        }

        // Prevent duplicate calls while a search is already in progress
        if (_state.value.isLoading) return

        // Clear textInput immediately to prevent duplicate calls (e.g. voice race condition)
        conversationStore.setTextInput("")

        fetchSearch(currentQuery.trim())
    }

    /**
     * Envia ao chat da IA a escolha de um produto sugerido, como se o usuário tivesse digitado.
     * A quantidade vem explícita na mensagem para a IA adicionar direto ao carrinho (usuário
     * escolhe no modal de quantidade antes de chamar isto), sem perguntar de novo. Durante a
     * ligação de voz quem trata é o LiveCallViewModel.orderInCall.
     */
    fun chooseProductInChat(product: Product, quantity: Int = 1) {
        if (_state.value.isLoading) return
        fetchSearch(buildAddToCartPrompt(product, quantity))
    }

    /** Envia um prompt pré-definido ao chat da IA (ex.: atalhos rápidos como "Caixa Surpresa"). */
    fun sendQuickPrompt(prompt: String) {
        if (_state.value.isLoading) return
        fetchSearch(prompt)
    }

    /** Pede sugestões à IA considerando a personalização alimentar salva localmente no perfil. */
    fun requestPersonalizedSuggestions() {
        if (_state.value.isLoading) return
        fetchSearch(buildPersonalizedSuggestionsPrompt(userProfile.value))
    }

    fun clearPendingVoiceAudio() {
        _state.update { it.copy(pendingVoiceAudio = null) }
    }

    /** Troca a mensagem [id] (ou acrescenta, se ainda não existe). */
    private fun upsertMessage(messages: List<ChatMessage>, id: String, transform: (ChatMessage?) -> ChatMessage): List<ChatMessage> {
        val updated = messages.toMutableList()
        val idx = updated.indexOfFirst { it.id == id }
        if (idx != -1) updated[idx] = transform(updated[idx]) else updated.add(transform(null))
        return updated
    }

    private fun fetchSearch(resolvedQuery: String) {
        // Turno por voz (microfone): não revelamos o texto aos poucos como no streaming normal —
        // esperamos a resposta completa, sintetizamos a fala e só então mostramos a mensagem,
        // pra ela aparecer no exato momento em que a IA começa a falar (ver bloco pós-stream).
        val isVoiceTurn = _state.value.lastInputWasVoice

        // Adiciona a mensagem do usuário ao chat
        conversationStore.addUserMessage(resolvedQuery)
        saveChatMessages()

        _state.update { it.copy(isLoading = true, isStreaming = false, error = null) }
        catalogStore.setSuggestionMode(false)

        viewModelScope.launch {
            try {
                val currentAiMessageId = "ai_${kotlin.random.Random.nextLong()}"
                var fullResponseText = ""
                var lastChunk: ChatReply? = null

                // Consome o stream da IA
                aiChatRepository.streamMessage(resolvedQuery.trim()).collect { chunk ->
                    lastChunk = chunk

                    // Limpa tags internas e reduz espaços em branco excessivos
                    fullResponseText += chunk.fragment.replace("[[CONFIRM_ORDER]]", "")

                    // Tratamento final para exibição: remove triplas quebras de linha e trim no início/fim
                    val displayResponseText = fullResponseText
                        .replace(Regex("\\n{3,}"), "\n\n")
                        .trimStart()

                    if (isVoiceTurn) {
                        // Só acompanha a sessão/streaming; a mensagem em si só é revelada
                        // depois, junto com o áudio já sintetizado (ver pós-stream abaixo).
                        _state.update { it.copy(isStreaming = true) }
                        conversationStore.update { it.copy(currentAiSessionId = chunk.sessionId ?: it.currentAiSessionId) }
                    } else {
                        conversationStore.update { state ->
                            val messages = upsertMessage(state.messages, currentAiMessageId) { existing ->
                                existing?.copy(
                                    text = displayResponseText,
                                    restaurants = chunk.restaurants.ifEmpty { existing.restaurants },
                                    products = chunk.products.ifEmpty { existing.products }
                                ) ?: ChatMessage(
                                    id = currentAiMessageId,
                                    type = ChatMessageType.AI,
                                    text = displayResponseText,
                                    restaurants = chunk.restaurants,
                                    products = chunk.products
                                )
                            }
                            state.copy(
                                messages = messages.takeLast(20),
                                currentAiSessionId = chunk.sessionId ?: state.currentAiSessionId
                            )
                        }
                        // Oculta o indicador de "pensando" assim que o texto começa a chegar
                        _state.update { it.copy(isStreaming = true, isLoading = false) }
                    }
                }

                // Processamento pós-stream
                val finalResponse = lastChunk ?: return@launch

                _state.update { it.copy(isStreaming = false) }

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

                    conversationStore.update { state ->
                        val messages = upsertMessage(state.messages, currentAiMessageId) {
                            ChatMessage(id = currentAiMessageId, type = ChatMessageType.AI, text = textoLimpo)
                        }
                        state.copy(messages = messages.takeLast(20))
                    }
                    _state.update { it.copy(pendingVoiceAudio = audioSintetizado) }
                }

                // Verifica se deve mostrar a sacola IA integrada (Master Final Object ou Tag)
                if (finalResponse.showCart || fullResponseText.contains("[[CONFIRM_ORDER]]")) {
                    // Garante que o texto final não tenha a tag e remove brancos excessivos
                    val finalCleanedText = fullResponseText
                        .replace("[[CONFIRM_ORDER]]", "")
                        .replace(Regex("\\n{3,}"), "\n\n")
                        .trim()

                    conversationStore.update { state ->
                        val messages = state.messages.map { if (it.id == currentAiMessageId) it.copy(text = finalCleanedText) else it }
                        state.copy(
                            messages = messages,
                            currentAiSessionId = finalResponse.sessionId ?: state.currentAiSessionId
                        )
                    }

                    handleShowCart(finalResponse)
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

                // Atualiza o Home se necessário
                if (resolvedQuery.equals("ver todos", ignoreCase = true) && aiRestaurantResults.isNotEmpty()) {
                    catalogStore.setAllRestaurants(aiRestaurantResults)
                }

                // Atualiza a última mensagem com os resultados finais consolidados
                conversationStore.update { state ->
                    val messages = state.messages.map {
                        if (it.id == currentAiMessageId) it.copy(restaurants = aiRestaurantResults, products = finalResponse.products) else it
                    }
                    state.copy(messages = messages, textInput = "", lastSearchQuery = resolvedQuery)
                }
                _state.update { it.copy(isLoading = false) }

                saveChatMessages()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Mensagem amigável em PT-PT para o utilizador — o detalhe técnico
                // (e.message, muitas vezes em inglês/stacktrace) só vai para o log.
                e.printStackTrace()
                val errorMessage = "Ups! Não foi possível ligar aos nossos servidores neste momento. Verifique a sua ligação à internet e tente novamente daqui a pouco."
                addAiMessage(text = errorMessage)
                _state.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    /**
     * Processa a exibição da sacola integrada pela IA.
     * Sincroniza os produtos da IA com o carrinho e permite continuar editando.
     */
    private fun handleShowCart(reply: ChatReply) {
        // Se a IA enviou uma lista de produtos na sacola, ela representa o estado ATUAL.
        cartStore.replaceWithAiCart(reply.cartProducts).forEach { gid ->
            viewModelScope.launch { cartRestaurantLoader.load(gid) }
        }
        cartStore.showAiCart()
        _state.update { it.copy(isLoading = false) }
        navigation.navigateTo(MainTab.AI) // Mantém no Chat para exibir a Sacola IA integrada
    }
}
