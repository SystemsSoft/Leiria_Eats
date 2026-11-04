package org.leria.eats.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.leria.eats.project.domain.model.Address
import org.leria.eats.project.domain.model.ChatMessage
import org.leria.eats.project.domain.model.ChatReply
import org.leria.eats.project.domain.model.CheckoutSession
import org.leria.eats.project.domain.model.DeliveryFeeQuote
import org.leria.eats.project.domain.model.LiveEvent
import org.leria.eats.project.domain.model.Order
import org.leria.eats.project.domain.model.OrderDraft
import org.leria.eats.project.domain.model.Restaurant
import org.leria.eats.project.domain.model.SavedPaymentMethod
import org.leria.eats.project.domain.model.UserProfile

// Portas do domínio: o que as regras de negócio e a apresentação precisam, sem saber se vem de
// Ktor, DataStore ou WebSocket. As implementações ficam em data/repository.

interface RestaurantRepository {
    /** Todos os restaurantes (/restaurants). Lista vazia em caso de falha. */
    suspend fun getAllRestaurants(): List<Restaurant>

    /** Restaurante com cardápio completo, ou null se não encontrado / falha de rede. */
    suspend fun getRestaurant(gid: String): Restaurant?
}

/** Conversa com a IA de vendas (texto). A sessão é a mesma usada pela ligação de voz. */
interface AiChatRepository {
    fun currentSessionId(): String
    fun streamMessage(text: String): Flow<ChatReply>
    suspend fun sendMessage(text: String): ChatReply
    suspend fun confirmSession(sessionId: String): Boolean
    suspend fun restartSession(sessionId: String): Boolean
}

/** Histórico local das últimas mensagens do chat da IA. */
interface ChatHistoryRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun save(messages: List<ChatMessage>)
    suspend fun clear()
}

interface OrderRepository {
    suspend fun getCustomerOrders(userId: String): List<Order>
    suspend fun initiateCheckout(order: OrderDraft): CheckoutSession?
    suspend fun updateOrderStatus(orderId: String, newStatus: String): Boolean
    suspend fun submitRating(orderId: String, restaurantGid: String, productGid: String, rating: Int): Boolean

    /** Lança exceção com a mensagem do servidor quando o endereço está fora da área de entrega. */
    suspend fun getDeliveryFee(
        customerLat: Double,
        customerLon: Double,
        restaurantLat: Double,
        restaurantLon: Double,
        restaurantGid: String
    ): DeliveryFeeQuote

    // Metadados guardados localmente por pedido (a API nem sempre os devolve).
    /** key = orderId, value = texto da pesquisa que originou o pedido */
    val orderSearchQueries: Flow<Map<String, String>>
    /** key = "orderId::productName", value = 1..5 */
    val orderItemRatings: Flow<Map<String, Int>>
    /** key = "orderId::productName", value = productGid */
    val orderProductGids: Flow<Map<String, String>>
    /** key = orderId, value = restaurantGid */
    val orderRestaurantGids: Flow<Map<String, String>>

    suspend fun saveOrderSearchQuery(orderId: String, searchQuery: String)
    suspend fun saveOrderItemRating(orderId: String, productName: String, rating: Int)
    suspend fun saveOrderProductGids(orderId: String, items: List<Pair<String, String>>)
    suspend fun saveOrderRestaurantGid(orderId: String, restaurantGid: String)
}

interface ProfileRepository {
    val userProfile: Flow<UserProfile>

    suspend fun saveProfile(
        id: String,
        name: String,
        email: String,
        phone: String,
        addresses: List<Address>,
        photoUrl: String? = null,
        allergies: String = "",
        lifestyles: String = ""
    )

    /** Cartões salvos no servidor (Stripe), ou null se não houver nenhum / falha. */
    suspend fun fetchRemoteSavedPaymentMethods(userId: String): List<SavedPaymentMethod>?
    suspend fun savePaymentMethods(methods: List<SavedPaymentMethod>)
}

/** Síntese de fala com a mesma voz da ligação ao vivo (PCM16 mono 24kHz). */
interface SpeechRepository {
    /** null se a síntese falhar. */
    suspend fun synthesize(text: String): ByteArray?
}

/**
 * Conversa de voz em tempo real. [connect] só retorna quando a ligação fecha — quem chama roda
 * num coroutine próprio e cancela esse job para encerrar.
 */
interface LiveConversationRepository {
    val events: Flow<LiveEvent>
    suspend fun connect(sessionId: String, nomeUsuario: String = "")
    suspend fun disconnect()
    suspend fun sendAudioChunk(pcm: ByteArray)
    suspend fun sendText(texto: String)
}
