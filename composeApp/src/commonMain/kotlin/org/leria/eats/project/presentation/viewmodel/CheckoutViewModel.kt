package org.leria.eats.project.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.leria.eats.project.domain.model.Address
import org.leria.eats.project.domain.model.CheckoutSession
import org.leria.eats.project.domain.model.DeliveryFeeQuote
import org.leria.eats.project.domain.model.UserProfile
import org.leria.eats.project.domain.repository.OrderRepository
import org.leria.eats.project.domain.repository.ProfileRepository
import org.leria.eats.project.domain.usecase.PlaceOrderResult
import org.leria.eats.project.domain.usecase.PlaceOrderUseCase
import org.leria.eats.project.domain.usecase.SyncSavedPaymentMethodsUseCase
import org.leria.eats.project.payment.StripePaymentManager
import org.leria.eats.project.payment.StripePaymentResult
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.state.CartStore
import org.leria.eats.project.presentation.state.CatalogStore
import org.leria.eats.project.presentation.state.ConversationStore
import org.leria.eats.project.presentation.state.NavigationStore
import org.leria.eats.project.presentation.state.OrderEvents

data class CheckoutState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val checkoutUrl: String? = null,
    val isAddressSheetVisible: Boolean = false,
    val showSavePaymentSheet: Boolean = false,
    val showPaymentConfirmSheet: Boolean = false,
    val pendingDeliveryType: String = "delivery",
    val pendingCheckoutSavePaymentMethod: Boolean = false,
    val selectedAddressForCheckout: Address? = null,
    val pendingDeliveryFee: Double = 0.0,
    val pendingServiceFee: Double = 0.0,
    val pendingDeliveryFeesMap: Map<String, Double> = emptyMap(),
    val isProcessingAutoPayment: Boolean = false,
    val pendingSavePaymentMethod: Boolean = false,
    val orderJustPlaced: Boolean = false // Flag to trigger voice feedback when order is placed
)

/** Checkout: taxas de entrega, escolha de pagamento, criação do pedido e pós-pagamento. */
class CheckoutViewModel(
    private val placeOrder: PlaceOrderUseCase,
    private val syncSavedPaymentMethods: SyncSavedPaymentMethodsUseCase,
    private val orderRepository: OrderRepository,
    profileRepository: ProfileRepository,
    private val stripePaymentManager: StripePaymentManager,
    private val cartStore: CartStore,
    private val catalogStore: CatalogStore,
    private val conversationStore: ConversationStore,
    private val navigation: NavigationStore,
    private val orderEvents: OrderEvents,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckoutState())
    val state: StateFlow<CheckoutState> = _state.asStateFlow()

    private val userProfile: StateFlow<UserProfile> =
        profileRepository.userProfile.stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    suspend fun getDeliveryFee(
        customerLat: Double,
        customerLon: Double,
        restaurantLat: Double,
        restaurantLon: Double,
        restaurantGid: String
    ): DeliveryFeeQuote = orderRepository.getDeliveryFee(customerLat, customerLon, restaurantLat, restaurantLon, restaurantGid)

    fun checkout() {
        if (userProfile.value.addresses.isEmpty()) {
            _state.update { it.copy(error = "Por favor, adicione um endereço no seu perfil antes de continuar.") }
            navigation.onTabSelected(MainTab.PROFILE)
            return
        }
        // Always show address selection sheet — no default concept
        _state.update { it.copy(isAddressSheetVisible = true) }
    }

    fun checkoutWithAddress(
        address: Address,
        deliveryFee: Double = 0.0,
        serviceFee: Double = 0.0,
        deliveryType: String = "delivery",
        deliveryFeesMap: Map<String, Double> = emptyMap()
    ) {
        val profile = userProfile.value
        if (profile.name.isBlank()) {
            _state.update { it.copy(error = "Por favor, preencha seu Nome no Perfil.") }
            navigation.onTabSelected(MainTab.PROFILE)
            return
        }
        if (cartStore.state.value.items.isEmpty()) return
        // Store the delivery type chosen in the summary
        _state.update {
            it.copy(
                pendingDeliveryType = deliveryType,
                pendingDeliveryFee = deliveryFee,
                pendingServiceFee = serviceFee,
                selectedAddressForCheckout = address
            )
        }
        if (profile.savedPaymentMethods.isNotEmpty()) {
            showPaymentConfirmForAddress(address, deliveryFee, serviceFee, deliveryFeesMap)
        } else {
            showSavePaymentSheetForAddress(address, deliveryFee, serviceFee, deliveryFeesMap)
        }
    }

    fun dismissPaymentConfirmSheet() {
        _state.update { it.copy(showPaymentConfirmSheet = false) }
    }

    /** Called when user confirms payment with saved card (useSavedCard=true) or chooses another method (false). */
    fun onPaymentConfirmResult(useSavedCard: Boolean) {
        if (_state.value.selectedAddressForCheckout == null) return
        val deliveryType = _state.value.pendingDeliveryType
        _state.update {
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
        if (conversationStore.state.value.builtByLiveCall) conversationStore.clear(viewModelScope)
    }

    fun dismissSavePaymentSheet() {
        _state.update { it.copy(showSavePaymentSheet = false) }
    }

    fun showPaymentConfirmForAddress(address: Address, deliveryFee: Double = 0.0, serviceFee: Double = 0.0, deliveryFeesMap: Map<String, Double> = emptyMap()) {
        _state.update {
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
        _state.update {
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
        val deliveryType = _state.value.pendingDeliveryType
        _state.update {
            it.copy(
                showSavePaymentSheet = false,
                // Sempre passa false para não salvar cartão como padrão
                pendingCheckoutSavePaymentMethod = false
            )
        }
        // Use the delivery type already chosen in the summary
        proceedWithDeliveryType(deliveryType)
    }

    fun dismissAddressSheet() {
        _state.update { it.copy(isAddressSheetVisible = false) }
    }

    private fun proceedWithDeliveryType(deliveryType: String) {
        val currentState = _state.value
        val address = currentState.selectedAddressForCheckout ?: return
        val savePaymentMethod = currentState.pendingCheckoutSavePaymentMethod
        val deliveryFee = currentState.pendingDeliveryFee
        val serviceFee = currentState.pendingServiceFee
        val deliveryFeesMap = currentState.pendingDeliveryFeesMap

        _state.update {
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

    private fun confirmCheckout(
        selectedAddress: Address,
        savePaymentMethod: Boolean,
        deliveryType: String,
        deliveryFee: Double,
        serviceFee: Double,
        deliveryFeesMap: Map<String, Double>
    ) {
        _state.update {
            it.copy(
                isAddressSheetVisible = false,
                isLoading = true,
                error = null,
                pendingSavePaymentMethod = savePaymentMethod
            )
        }

        val cart = cartStore.state.value
        val profile = userProfile.value
        val conversation = conversationStore.state.value

        if (cart.items.isEmpty()) {
            _state.update { it.copy(isLoading = false, error = "Sua sacola está vazia.") }
            return
        }

        if (profile.name.isBlank()) {
            _state.update { it.copy(isLoading = false, error = "Por favor, preencha seu Nome no Perfil.") }
            navigation.onTabSelected(MainTab.PROFILE)
            return
        }

        // Lido AQUI (antes do launch) porque limpar a conversa logo em seguida zera builtByLiveCall.
        val sessaoIa = conversationStore.aiSessionToConfirm()
        viewModelScope.launch {
            try {
                val result = placeOrder(
                    cartItems = cart.items,
                    cartRestaurants = cart.restaurants,
                    profile = profile,
                    address = selectedAddress,
                    savePaymentMethod = savePaymentMethod,
                    deliveryType = deliveryType,
                    deliveryFee = deliveryFee,
                    serviceFee = serviceFee,
                    deliveryFeesMap = deliveryFeesMap,
                    searchQuery = conversation.lastSearchQuery,
                    aiSessionId = sessaoIa
                )
                when (result) {
                    is PlaceOrderResult.Started -> continueCheckout(result.session)
                    PlaceOrderResult.NoValidItems ->
                        _state.update { it.copy(isLoading = false, error = "Erro ao processar itens da sacola.") }
                    PlaceOrderResult.CheckoutUnavailable ->
                        _state.update { it.copy(isLoading = false, error = "Erro ao iniciar sessão de pagamento.") }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isLoading = false, error = "Erro crítico: ${e.message}") }
            }
        }
    }

    /** O servidor aceitou o pedido: segue para pago na hora, PaymentSheet da Stripe ou WebView. */
    private fun continueCheckout(session: CheckoutSession) {
        if (session.autoPaid) {
            finalizeOrder(checkoutUrl = null, autoPaid = true)
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
                            finalizeOrder(checkoutUrl = null, autoPaid = false)
                        }
                        is StripePaymentResult.Canceled -> {
                            _state.update { it.copy(isLoading = false, error = "Pagamento cancelado.") }
                        }
                        is StripePaymentResult.Failed -> {
                            _state.update { it.copy(isLoading = false, error = "Erro no pagamento: ${result.error}") }
                        }
                    }
                }
            )
        } else if (session.url != null) {
            // Fallback para WebView se necessário
            finalizeOrder(checkoutUrl = session.url, autoPaid = false)
        } else {
            _state.update { it.copy(isLoading = false, error = "Erro ao iniciar sessão de pagamento.") }
        }
    }

    /** Pedido criado: limpa sacola, restaurante aberto e pesquisa; sem WebView, vai para "Pedidos". */
    private fun clearAfterOrder() {
        cartStore.clearItems()
        catalogStore.resetSelection()
        conversationStore.update { it.copy(textInput = "", lastSearchQuery = "") }
    }

    /** Pagamento concluído e o app indo para "Meus pedidos": limpa o chat da IA com a MESMA função do
     * botão de limpar conversa (conversa + sacola), para voltar na aba da IA numa conversa nova. */
    private fun limparChatAposPagamento() {
        conversationStore.clear(viewModelScope)
        cartStore.clear()
    }

    private fun finalizeOrder(checkoutUrl: String?, autoPaid: Boolean) {
        clearAfterOrder()
        cartStore.clear()
        _state.update {
            it.copy(
                isLoading = false,
                isProcessingAutoPayment = false,
                checkoutUrl = checkoutUrl,
                error = null,
                pendingSavePaymentMethod = false,
                orderJustPlaced = autoPaid
            )
        }
        if (checkoutUrl == null) navigation.navigateTo(MainTab.ORDERS)

        // checkoutUrl == null: o app foi direto para "Pedidos" (com WebView o fim do pagamento chega em
        // onPaymentResult, que também limpa)
        if (checkoutUrl == null) limparChatAposPagamento()

        if (autoPaid) orderEvents.orderPlaced(followStatus = true)
    }

    fun onPaymentResult(isSuccess: Boolean, orderId: String?) {
        if (isSuccess) {
            val shouldFetchMethods = _state.value.pendingSavePaymentMethod
            val userId = userProfile.value.id
            val pendingSearchQuery = conversationStore.state.value.lastSearchQuery
            val cartItems = cartStore.state.value.items

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
                    val restaurantGid = cartStore.state.value.restaurantGid
                    if (restaurantGid != null) {
                        orderRepository.saveOrderRestaurantGid(orderId, restaurantGid)
                    }
                }
            }

            clearAfterOrder()
            _state.update {
                it.copy(
                    isLoading = false,
                    checkoutUrl = null,
                    error = null,
                    pendingSavePaymentMethod = false,
                    orderJustPlaced = true // Trigger voice feedback
                )
            }
            navigation.navigateTo(MainTab.ORDERS)
            limparChatAposPagamento()

            // Fetch saved payment methods if user chose to save
            if (shouldFetchMethods && userId.isNotBlank()) {
                viewModelScope.launch {
                    syncSavedPaymentMethods(userId)
                }
            }

            orderEvents.orderPlaced(followStatus = false)
        } else {
            _state.update {
                it.copy(
                    checkoutUrl = null,
                    pendingSavePaymentMethod = false,
                    error = if (orderId == null) "Pagamento cancelado." else "Erro: ID do pedido não encontrado."
                )
            }
        }
    }

    fun cancelAutoPayment() {
        _state.update { it.copy(isProcessingAutoPayment = false) }
    }

    fun resetOrderJustPlacedFlag() {
        _state.update { it.copy(orderJustPlaced = false) }
    }
}
