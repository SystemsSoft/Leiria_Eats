package org.leria.eats.project.data.voice

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.encodeURLParameter
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.leria.eats.project.domain.model.LiveEvent
import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.domain.repository.LiveConversationRepository
import org.leria.eats.project.util.fromBase64
import org.leria.eats.project.util.toBase64

/**
 * Cliente WebSocket da conversa de voz em tempo real — fala com
 * wss://api.leiriaeats.com/chat/voice/{session_id} (services/gemini_live_bridge.py
 * no servidor), NUNCA direto com a Gemini (a chave da API não sai do backend).
 *
 * `connect()` é uma suspend function que só retorna quando a ligação fecha —
 * quem chama deve rodar num coroutine próprio (viewModelScope.launch) e cancelar
 * esse job para encerrar a conversa.
 */
class LiveConversationClient : LiveConversationRepository {
    private val client = HttpClient {
        install(WebSockets)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // Nome deliberadamente diferente de "outgoing": WebSocketSession já tem uma
    // propriedade própria chamada `outgoing` (o SendChannel<Frame> da ligação) —
    // usar o mesmo nome aqui colidia com ela dentro do bloco de `client.webSocket`.
    private var enviarFrame: (suspend (Frame) -> Unit)? = null

    private val _events = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 128)
    override val events: SharedFlow<LiveEvent> = _events

    /**
     * @param nomeUsuario nome cadastrado no perfil do app — a IA usa pra se apresentar já
     * chamando o cliente pelo nome na primeira fala (ver gemini_live_bridge.py, parâmetro
     * de query `nome`). Vazio/branco = sem nome cadastrado ainda, cai na saudação genérica.
     */
    override suspend fun connect(sessionId: String, nomeUsuario: String) {
        val query = if (nomeUsuario.isNotBlank()) "?nome=${nomeUsuario.encodeURLParameter()}" else ""
        try {
            try {
                client.webSocket(urlString = "wss://api.leiriaeats.com/chat/voice/$sessionId$query") {
                    enviarFrame = { frame -> send(frame) }
                    _events.emit(LiveEvent.Connected)
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                parseEvento(frame.readText())?.let { _events.emit(it) }
                            }
                        }
                    } finally {
                        enviarFrame = null
                    }
                }
            } catch (e: Exception) {
                // Sem este catch, uma falha ao ABRIR a ligação (rede indisponível,
                // servidor fora do ar, TLS/handshake, etc.) subia como exceção não
                // tratada até o viewModelScope de quem chamou connect() — sem nenhum
                // LiveEvent.Error emitido (esse evento só nasce de uma mensagem JSON
                // recebida DEPOIS de conectado) e sem feedback nenhum pro usuário, só
                // um "não funciona" silencioso. Agora qualquer falha vira um erro
                // visível de verdade.
                _events.emit(
                    LiveEvent.Error(
                        "Não foi possível iniciar a ligação de voz. Verifique a sua ligação à internet e tente novamente."
                    )
                )
            }
        } finally {
            _events.emit(LiveEvent.Disconnected)
        }
    }

    override suspend fun disconnect() {
        runCatching { enviarFrame?.invoke(Frame.Close()) }
    }

    /** Envia um chunk de áudio PCM16 mono 16kHz capturado do microfone. */
    override suspend fun sendAudioChunk(pcm: ByteArray) {
        val texto = JsonObject(
            mapOf(
                "type" to JsonPrimitive("audio"),
                "data" to JsonPrimitive(pcm.toBase64()),
            )
        ).toString()
        // best-effort: um chunk perdido no meio de uma ligação que está caindo não
        // deve derrubar o job de captura do microfone (a falha "de verdade" já é
        // reportada pelo catch em connect(), que fecha a sessão e emite o erro).
        runCatching { enviarFrame?.invoke(Frame.Text(texto)) }
    }

    /** Pedido escrito dentro da ligação (ex.: "Adicionar 2 Pizza X ao meu carrinho." ao tocar no + de
     * um card): entra na conversa como se o usuário tivesse dito, e a IA responde por voz. */
    override suspend fun sendText(texto: String) {
        val payload = JsonObject(
            mapOf(
                "type" to JsonPrimitive("text"),
                "text" to JsonPrimitive(texto),
            )
        ).toString()
        enviarFrame?.invoke(Frame.Text(payload))
    }

    private fun parseEvento(raw: String): LiveEvent? {
        val obj = try {
            json.parseToJsonElement(raw).jsonObject
        } catch (e: Exception) {
            return null
        }
        return when (obj["type"]?.jsonPrimitive?.content) {
            "transcript" -> LiveEvent.Transcript(
                role = obj["role"]?.jsonPrimitive?.content ?: "ai",
                text = obj["text"]?.jsonPrimitive?.content ?: "",
            )
            "cart_updated" -> {
                val cartJson = (obj["cart"] as? JsonArray) ?: JsonArray(emptyList())
                val produtos = try {
                    json.decodeFromString<List<Product>>(cartJson.toString())
                } catch (e: Exception) {
                    emptyList()
                }
                LiveEvent.CartUpdated(produtos)
            }
            "show_cart" -> LiveEvent.ShowCart
            "products_suggested" -> {
                val produtosJson = (obj["products"] as? JsonArray) ?: JsonArray(emptyList())
                val produtos = try {
                    json.decodeFromString<List<Product>>(produtosJson.toString())
                } catch (e: Exception) {
                    emptyList()
                }
                LiveEvent.ProductsSuggested(produtos)
            }
            "audio" -> {
                val data = obj["data"]?.jsonPrimitive?.content ?: return null
                val mime = obj["mime_type"]?.jsonPrimitive?.content ?: "audio/pcm;rate=24000"
                LiveEvent.Audio(pcm = data.fromBase64(), mimeType = mime)
            }
            "turn_complete" -> LiveEvent.TurnComplete
            "interrupted" -> LiveEvent.Interrupted
            "error" -> LiveEvent.Error(obj["message"]?.jsonPrimitive?.content ?: "Erro desconhecido")
            else -> null
        }
    }
}
