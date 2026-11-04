package org.leria.eats.project.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.leria.eats.project.domain.model.ChatMessage
import org.leria.eats.project.domain.model.ChatMessageType
import org.leria.eats.project.domain.model.LiveEvent
import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.domain.model.UserProfile
import org.leria.eats.project.domain.repository.AiChatRepository
import org.leria.eats.project.domain.repository.LiveConversationRepository
import org.leria.eats.project.domain.repository.ProfileRepository
import org.leria.eats.project.domain.usecase.buildAddToCartPrompt
import org.leria.eats.project.domain.usecase.mesclarSugestoesDaLigacao
import org.leria.eats.project.domain.voice.VoiceAudioInput
import org.leria.eats.project.domain.voice.VoiceAudioOutput
import org.leria.eats.project.presentation.state.CartRestaurantLoader
import org.leria.eats.project.presentation.state.CartStore
import org.leria.eats.project.presentation.state.ConversationStore

data class LiveCallState(
    // Conversa de voz em tempo real (Gemini Live API via services/gemini_live_bridge.py) —
    // modo adicional ao microfone (STT) e ao campo de texto já existentes, não substitui nenhum dos dois.
    val isActive: Boolean = false,
    // true enquanto a IA está com áudio tocando (entre o primeiro chunk de um turno e o
    // turn_complete), false enquanto ela aguarda o usuário falar. A tela da ligação não mostra a
    // conversa em texto, só esse status.
    val isAiSpeaking: Boolean = false,
    // A IA está "a pensar" na ligação: depois de ouvir o usuário (ou de rodar uma ferramenta, como
    // adicionar à sacola) e ANTES de o primeiro áudio da resposta chegar; também no início, até a saudação.
    val isAiThinking: Boolean = false,
    // Produtos que a IA destacou na resposta atual (tool "sugerir_produtos" no servidor) — só usado
    // na tela da ligação de voz, já que ali não há bolhas de chat pra mostrar os cartões de produto.
    val suggestedProducts: List<Product> = emptyList(),
    // true = o áudio do microfone continua sendo capturado (necessário pro cancelamento de
    // eco) mas NÃO é enviado ao servidor/IA — o usuário fica "mudo" na ligação sem encerrá-la.
    val isMicMuted: Boolean = false,
    // A fala da IA está a ser transcrita para a conversa (mesmo papel do isStreaming do chat).
    val isStreaming: Boolean = false
)

/**
 * Ligação de voz em tempo real (Gemini Live API). Modo ADICIONAL ao microfone (STT) e ao campo de
 * texto — usa a MESMA sessão (AiChatRepository.currentSessionId) do chat por texto, então a
 * sacola e o histórico continuam unificados entre os três jeitos de montar um pedido. Ver
 * services/gemini_live_bridge.py no servidor para o protocolo de eventos.
 */
class LiveCallViewModel(
    private val liveConversationRepository: LiveConversationRepository,
    private val aiChatRepository: AiChatRepository,
    private val liveAudioRecorder: VoiceAudioInput,
    private val liveAudioPlayer: VoiceAudioOutput,
    profileRepository: ProfileRepository,
    private val conversationStore: ConversationStore,
    private val cartStore: CartStore,
    private val cartRestaurantLoader: CartRestaurantLoader,
) : ViewModel() {

    private val _state = MutableStateFlow(LiveCallState())
    val state: StateFlow<LiveCallState> = _state.asStateFlow()

    private val userProfile: StateFlow<UserProfile> =
        profileRepository.userProfile.stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    private var liveConversationJob: Job? = null
    private var liveAudioSendJob: Job? = null

    private var liveAiTranscriptBuffer = ""
    private var liveAiMessageId: String? = null

    fun startLiveConversation() {
        if (_state.value.isActive) return
        _state.update {
            it.copy(
                isActive = true,
                isAiSpeaking = false,
                isAiThinking = true, // até a saudação chegar
                suggestedProducts = emptyList(),
                isMicMuted = true // a ligação sempre começa com o microfone mudo
            )
        }

        // Prepara a saída de áudio para a chamada (caminho de comunicação, alto-falante, volume
        // alinhado ao de mídia) ANTES de o microfone abrir — o cancelamento de eco precisa do modo
        // de áudio já definido quando a captura começa. Desfeito em stopLiveConversation().
        liveAudioPlayer.startCall()
        conversationStore.markBuiltByLiveCall()

        val sessionId = aiChatRepository.currentSessionId()
        val nomeUsuario = userProfile.value.name

        liveConversationJob = viewModelScope.launch {
            launch {
                liveConversationRepository.events.collect { evento -> handleLiveEvent(evento) }
            }
            liveConversationRepository.connect(sessionId, nomeUsuario)
        }

        // Começa a mandar áudio do microfone assim que a ligação for aberta —
        // sendAudioChunk() não faz nada sozinho enquanto a sessão não conectar
        // (os frames anteriores ao LiveEvent.Connected são só descartados, sem erro).
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
                    if (_state.value.isMicMuted) ByteArray(chunk.size) else chunk
                )
            }
        }
    }

    fun toggleMicMuted() {
        if (!_state.value.isActive) return
        _state.update { it.copy(isMicMuted = !it.isMicMuted) }
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
        _state.update {
            it.copy(
                isActive = false,
                isAiSpeaking = false,
                isAiThinking = false,
                suggestedProducts = emptyList(),
                isMicMuted = false
            )
        }
        viewModelScope.launch { liveConversationRepository.disconnect() }
    }

    /**
     * Pedido de um produto DENTRO da ligação (toque no + de um card, com a quantidade escolhida no
     * modal): mesma sessão Live, então a IA adiciona à sacola e responde falando. Corta a fala em
     * andamento — o servidor interrompe a IA.
     */
    fun orderInCall(product: Product, quantity: Int = 1) {
        liveAudioPlayer.stop()
        _state.update { it.copy(isAiSpeaking = false, isAiThinking = true) }
        val pedido = buildAddToCartPrompt(product, quantity)
        viewModelScope.launch { runCatching { liveConversationRepository.sendText(pedido) } }
    }

    private fun saveChatMessages() {
        viewModelScope.launch { conversationStore.persist() }
    }

    /** Ferramenta da IA rodou (sacola/sugestão): se ela ainda não começou a falar, está pensando. */
    private fun marcarIaPensando() {
        if (!_state.value.isAiSpeaking) _state.update { it.copy(isAiThinking = true) }
    }

    /** Fim do turno da IA (completo ou interrompido): a fala transcrita vira a mensagem final. */
    private fun fecharTurno() {
        if (liveAiMessageId != null) saveChatMessages()
        liveAiTranscriptBuffer = ""
        liveAiMessageId = null
        _state.update { it.copy(isStreaming = false, isAiSpeaking = false, isAiThinking = false) }
    }

    private fun handleLiveEvent(evento: LiveEvent) {
        when (evento) {
            is LiveEvent.Transcript -> {
                if (evento.role == "user") {
                    conversationStore.addUserMessage(evento.text)
                    saveChatMessages()
                    // Ouviu o usuário e ainda não está falando: está processando a resposta.
                    if (!_state.value.isAiSpeaking) _state.update { it.copy(isAiThinking = true) }
                } else {
                    // Acumula o texto da IA no MESMO padrão de streaming do chat por
                    // texto (isStreaming = true, atualiza a última mensagem por id),
                    // pra UI (ChatMessagesView) funcionar igual não importa a origem.
                    liveAiTranscriptBuffer += evento.text
                    val id = liveAiMessageId ?: "ai_live_${kotlin.random.Random.nextLong()}".also { liveAiMessageId = it }
                    conversationStore.update { state ->
                        val messages = state.messages.toMutableList()
                        val idx = messages.indexOfFirst { it.id == id }
                        val msg = ChatMessage(id = id, type = ChatMessageType.AI, text = liveAiTranscriptBuffer)
                        if (idx != -1) messages[idx] = msg else messages.add(msg)
                        state.copy(messages = messages.takeLast(20))
                    }
                    _state.update { it.copy(isStreaming = true) }
                }
            }
            is LiveEvent.CartUpdated -> {
                cartStore.replaceWithAiCart(evento.cart).forEach { gid ->
                    viewModelScope.launch { cartRestaurantLoader.load(gid) }
                }
                marcarIaPensando() // a ferramenta rodou; a fala de resposta vem logo depois
            }
            is LiveEvent.ShowCart -> {
                cartStore.showAiCart()
                marcarIaPensando()
            }
            is LiveEvent.ProductsSuggested -> {
                _state.update { it.copy(suggestedProducts = mesclarSugestoesDaLigacao(it.suggestedProducts, evento.products)) }
                marcarIaPensando()
            }
            is LiveEvent.Audio -> {
                liveAudioPlayer.play(evento.pcm)
                // Primeiro chunk de áudio de um turno = a IA começou a falar. Só volta a
                // "ouvindo" no turn_complete abaixo — é uma aproximação (o chunk já
                // recebido ainda leva um instante pra tocar de verdade), mas suficiente
                // pra UI acompanhar o ritmo da conversa.
                if (!_state.value.isAiSpeaking) {
                    _state.update { it.copy(isAiSpeaking = true, isAiThinking = false) }
                }
            }
            is LiveEvent.TurnComplete -> fecharTurno()
            is LiveEvent.Interrupted -> {
                // O usuário falou por cima da IA: a Gemini já parou de gerar, mas o áudio
                // que ela mandou mais rápido que o tempo real ainda está na fila do
                // player — sem limpar, a IA continuaria falando por cima dele até a fila
                // esvaziar.
                liveAudioPlayer.stop()
                fecharTurno()
            }
            is LiveEvent.Error -> {
                conversationStore.addAiMessage(text = evento.message)
                saveChatMessages()
                stopLiveConversation()
            }
            is LiveEvent.Connected -> { /* nada a fazer — já em isActive */ }
            is LiveEvent.Disconnected -> {
                // A ligação acabou pelo lado do servidor (ou caiu): faz a MESMA limpeza de um
                // "desligar" — senão o microfone continuaria capturando e o aparelho ficaria em
                // modo de comunicação (botões de volume presos no volume de chamada) até fechar o app.
                if (_state.value.isActive) {
                    stopLiveConversation()
                }
            }
        }
    }
}
