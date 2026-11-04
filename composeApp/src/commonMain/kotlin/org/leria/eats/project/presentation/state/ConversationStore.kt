package org.leria.eats.project.presentation.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.leria.eats.project.domain.model.ChatMessage
import org.leria.eats.project.domain.model.ChatMessageType
import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.domain.model.Restaurant
import org.leria.eats.project.domain.repository.AiChatRepository
import org.leria.eats.project.domain.repository.ChatHistoryRepository

data class ConversationState(
    // Lista de mensagens de chat da IA (últimas 20)
    val messages: List<ChatMessage> = emptyList(),
    val textInput: String = "",
    val currentAiSessionId: String? = null, // ID da sessão atual da IA (chat por texto) para confirmação final
    val lastSearchQuery: String = "",
    // true depois de uma ligação de voz: ao confirmar o pagamento, a conversa da ligação é limpa
    // (mesma função do "limpar conversa" do chat). Zerado em clear().
    val builtByLiveCall: Boolean = false
)

/**
 * A conversa com a IA — a mesma para o chat por texto, o microfone e a ligação de voz (mesma
 * sessão no servidor, ver AiChatRepository.currentSessionId). O checkout lê daqui a sessão a
 * confirmar e limpa a conversa depois do pagamento.
 */
class ConversationStore(
    private val aiChatRepository: AiChatRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
) : SessionStore {
    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    fun addUserMessage(text: String) {
        addMessage(ChatMessage(id = "user_${kotlin.random.Random.nextLong()}", type = ChatMessageType.USER, text = text))
    }

    fun addAiMessage(text: String, restaurants: List<Restaurant> = emptyList(), products: List<Product> = emptyList()) {
        val cleanedText = text.replace(Regex("\\n{3,}"), "\n\n").trim()
        addMessage(
            ChatMessage(
                id = "ai_${kotlin.random.Random.nextLong()}",
                type = ChatMessageType.AI,
                text = cleanedText,
                restaurants = restaurants,
                products = products
            )
        )
    }

    private fun addMessage(message: ChatMessage) {
        _state.update { it.copy(messages = (it.messages + message).takeLast(20)) }
    }

    fun update(transform: (ConversationState) -> ConversationState) {
        _state.update(transform)
    }

    fun setTextInput(text: String) {
        _state.update { it.copy(textInput = text) }
    }

    fun markBuiltByLiveCall() {
        _state.update { it.copy(builtByLiveCall = true) }
    }

    /**
     * Sessão da IA no servidor a confirmar/limpar: a do chat por texto ou, se o pedido foi montado
     * por ligação de voz, a sessão da ligação (currentAiSessionId só é preenchido pelo chat).
     */
    fun aiSessionToConfirm(): String? {
        val current = _state.value
        return current.currentAiSessionId ?: if (current.builtByLiveCall) aiChatRepository.currentSessionId() else null
    }

    /** Grava as mensagens atuais no histórico local. */
    suspend fun persist() {
        chatHistoryRepository.save(_state.value.messages)
    }

    /**
     * "Limpar conversa": zera a conversa na hora e, em [scope], reinicia a sessão no servidor e
     * apaga o histórico local.
     */
    fun clear(scope: CoroutineScope) {
        val sessionId = aiSessionToConfirm()
        scope.launch {
            // Notifica o servidor para reiniciar a sessão se houver uma ativa
            sessionId?.let {
                println("🧹 Reiniciando sessão da IA no servidor: $it")
                aiChatRepository.restartSession(it)
            }
            chatHistoryRepository.clear()
        }
        _state.update {
            it.copy(
                messages = emptyList(),
                textInput = "",
                currentAiSessionId = null, // Reseta o ID da sessão localmente
                builtByLiveCall = false
            )
        }
    }

    override fun reset() {
        _state.value = ConversationState()
    }
}
