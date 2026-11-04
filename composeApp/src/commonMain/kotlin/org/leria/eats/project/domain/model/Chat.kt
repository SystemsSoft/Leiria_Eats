package org.leria.eats.project.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChatMessageType {
    USER,
    AI
}

@Serializable
data class ChatMessage(
    val id: String,
    val type: ChatMessageType,
    val text: String,
    val restaurants: List<Restaurant> = emptyList(),
    val products: List<Product> = emptyList()
)

/**
 * Um pedaço (chunk) ou a resposta final da IA de vendas (/chat/sales).
 *
 * @param fragment texto deste pedaço (`text` do chunk ou, na falta dele, `response`).
 * @param products produtos destacados — `products` da API ou, se vazio, `productResults`.
 * @param cartProducts estado ATUAL da sacola da IA quando [showCart] é true.
 */
data class ChatReply(
    val fragment: String = "",
    val sessionId: String? = null,
    val restaurants: List<Restaurant> = emptyList(),
    val products: List<Product> = emptyList(),
    val cartProducts: List<Product> = emptyList(),
    val showCart: Boolean = false
)

/**
 * Eventos vindos do bridge de voz (services/gemini_live_bridge.py no servidor) via
 * WebSocket — protocolo simplificado próprio, não o JSON cru da Gemini Live API (o
 * backend traduz; se a Gemini mudar de protocolo, só o backend muda, não o app).
 */
sealed class LiveEvent {
    data class Transcript(val role: String, val text: String) : LiveEvent() // role: "user" | "ai"
    data class CartUpdated(val cart: List<Product>) : LiveEvent()
    data object ShowCart : LiveEvent()
    // Produtos que a IA destaca na resposta (tool "sugerir_produtos" no servidor) —
    // diferente de CartUpdated: são sugestões, ainda não necessariamente no carrinho.
    data class ProductsSuggested(val products: List<Product>) : LiveEvent()
    data class Audio(val pcm: ByteArray, val mimeType: String) : LiveEvent()
    data object TurnComplete : LiveEvent()
    // O usuário começou a falar por cima da IA — a Gemini já parou de gerar, mas o áudio que
    // ela mandou mais rápido que o tempo real ainda está na fila do player e precisa ser limpo.
    data object Interrupted : LiveEvent()
    data class Error(val message: String) : LiveEvent()
    data object Connected : LiveEvent()
    data object Disconnected : LiveEvent()
}
