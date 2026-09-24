package org.leria.eats.project.presentation

import kotlinx.serialization.Serializable
import org.leria.eats.project.data.Address
import org.leria.eats.project.data.Order
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.data.UserProfile

enum class MainTab {
    HOME,
    AI,
    CART,
    ORDERS,
    PROFILE
}

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

data class SearchUiState(
    val isLoading: Boolean = false,
    val textInput: String = "",
    val aiReply: String = "",
    val restaurantResults: List<Restaurant> = emptyList(),
    val productResults: List<Product> = emptyList(),
    // Lista exclusiva do Home — carregada uma vez e nunca sobrescrita por pesquisas da IA
    val allRestaurants: List<Restaurant> = emptyList(),
    val error: String? = null,
    val selectedRestaurant: Restaurant? = null,
    var restaurant: Restaurant? = null,
    val selectedCategory: String? = null,
    val selectedOrder: Order? = null,
    val cartRestaurantGid: String? = null,
    val cartItems: List<Product> = emptyList(),
    val cartRestaurants: List<Restaurant> = emptyList(), // Metadados dos restaurantes no carrinho
    val currentTab: MainTab = MainTab.AI,
    val orderHistory: List<Order> = emptyList(),
    val userProfile: UserProfile = UserProfile(),
    val isFilterEnabled: Boolean = false,
    val checkoutUrl: String? = null,
    val isAddressSheetVisible: Boolean = false,
    val cartError: String? = null,
    val cartMessage: String? = null,
    val cartAiMessage: String? = null,
    val cartAiMessageSpoken: Boolean = false, // Track if AI message has been spoken
    val isAiCartFlow: Boolean = false, // Indica se o carrinho atual veio de um fluxo da IA
    val lastSearchQuery: String = "",
    val isSuggestionMode: Boolean = false,
    val showSavePaymentSheet: Boolean = false,
    val showPaymentConfirmSheet: Boolean = false,
    val showDeliveryTypeSheet: Boolean = false,
    val pendingDeliveryType: String = "delivery",
    val pendingCheckoutSavePaymentMethod: Boolean = false,
    val selectedAddressForCheckout: Address? = null,
    val pendingDeliveryFee: Double = 0.0,
    val pendingServiceFee: Double = 0.0,
    val pendingDeliveryFeesMap: Map<String, Double> = emptyMap(),
    val isProcessingAutoPayment: Boolean = false,
    val isStreaming: Boolean = false, // Indica se a IA está enviando texto em tempo real
    val isBottomNavVisible: Boolean = false, // Controla se o BottomBar está visível no chat
    val currentAiSessionId: String? = null, // ID da sessão atual da IA para confirmação final
    val autoPaymentOrderId: Int? = null,
    val autoPaymentIntentId: String? = null,
    val pendingSavePaymentMethod: Boolean = false,
    val pendingProfileNavigation: Boolean = false, // Flag to navigate to profile after TTS finishes
    val orderJustPlaced: Boolean = false, // Flag to trigger voice feedback when order is placed
    val orderSearchQueries: Map<String, String> = emptyMap(),
    // key = "orderId::productName", value = 1..5
    val orderItemRatings: Map<String, Int> = emptyMap(),
    // key = "orderId::productName", value = productGid
    val orderProductGids: Map<String, String> = emptyMap(),
    // key = orderId, value = restaurantGid
    val orderRestaurantGids: Map<String, String> = emptyMap(),
    // Lista de mensagens de chat da IA
    val chatMessages: List<ChatMessage> = emptyList(),
    // Conversa de voz em tempo real (Gemini Live API via services/gemini_live_bridge.py) —
    // modo adicional ao microfone (STT) e ao campo de texto já existentes, não substitui nenhum dos dois.
    val isLiveConversationActive: Boolean = false,
    // true quando o texto atual/último enviado veio do microfone (STT), e não de digitação.
    // Usado para restringir a voz da IA (TTS) a apenas quando o usuário usou o microfone ou
    // está em ligação de voz — não deve falar respostas a mensagens digitadas.
    val lastInputWasVoice: Boolean = false,
    // Áudio (PCM16) já sintetizado para a última resposta de um turno por voz, pronto pra
    // tocar sem espera adicional — ver fetchSearch(): sintetizamos ANTES de revelar o texto,
    // pra mensagem e voz aparecerem juntas. Consumido (e zerado) assim que tocado.
    val pendingVoiceAudio: ByteArray? = null,
    // Status da ligação de voz ao vivo — true enquanto a IA está com áudio tocando (entre o
    // primeiro chunk de um turno e o turn_complete), false enquanto ela aguarda o usuário
    // falar. A tela da ligação não mostra a conversa em texto, só esse status.
    val isLiveAiSpeaking: Boolean = false,
    // A IA está "a pensar" na ligação: depois de ouvir o usuário (ou de rodar uma ferramenta, como
    // adicionar à sacola) e ANTES de o primeiro áudio da resposta chegar; também no início, até a saudação.
    val isLiveAiThinking: Boolean = false,
    // Produtos que a IA destacou na resposta atual da ligação ao vivo (tool "sugerir_produtos"
    // no servidor) — só usado na tela da ligação de voz, já que ali não há bolhas de chat
    // pra mostrar os cartões de produto do jeito normal.
    val liveSuggestedProducts: List<Product> = emptyList(),
    // true = o áudio do microfone continua sendo capturado (necessário pro cancelamento de
    // eco) mas NÃO é enviado ao servidor/IA — o usuário fica "mudo" na ligação sem encerrá-la.
    val isMicMuted: Boolean = false
) {
    val cartCount: Int get() = cartItems.size
}