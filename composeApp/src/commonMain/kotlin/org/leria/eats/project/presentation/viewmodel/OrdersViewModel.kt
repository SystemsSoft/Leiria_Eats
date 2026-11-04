package org.leria.eats.project.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.leria.eats.project.domain.model.Order
import org.leria.eats.project.domain.model.UserProfile
import org.leria.eats.project.domain.repository.OrderRepository
import org.leria.eats.project.domain.repository.ProfileRepository
import org.leria.eats.project.domain.usecase.RateOrderItemUseCase
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.state.NavigationStore
import org.leria.eats.project.presentation.state.OrderEvents

data class OrdersState(
    val orders: List<Order> = emptyList(),
    val selectedOrder: Order? = null,
    val isFilterEnabled: Boolean = false,
    val isLoading: Boolean = false,
    // key = "orderId::productName", value = 1..5
    val orderItemRatings: Map<String, Int> = emptyMap(),
    val error: String? = null
)

/** Aba Pedidos: histórico, acompanhamento de status e avaliações. */
class OrdersViewModel(
    private val orderRepository: OrderRepository,
    profileRepository: ProfileRepository,
    private val rateOrderItemUseCase: RateOrderItemUseCase,
    private val navigation: NavigationStore,
    orderEvents: OrderEvents,
) : ViewModel() {

    private val _state = MutableStateFlow(OrdersState())
    val state: StateFlow<OrdersState> = _state.asStateFlow()

    private val userProfile: StateFlow<UserProfile> =
        profileRepository.userProfile.stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    init {
        viewModelScope.launch {
            // Lê o id do próprio flow: o userProfile (stateIn) pode ainda não ter recebido o valor
            val profile = profileRepository.userProfile.first { it.id.isNotBlank() }
            refreshOrdersInternal(profile.id)
        }
        startStatusPolling()
        viewModelScope.launch {
            orderRepository.orderItemRatings.collect { ratings ->
                _state.update { it.copy(orderItemRatings = ratings) }
            }
        }
        viewModelScope.launch {
            navigation.tabSelections.collect { tab ->
                if (tab == MainTab.ORDERS && userProfile.value.id.isNotBlank()) refreshOrdersInternal()
            }
        }
        viewModelScope.launch {
            orderEvents.orderPlaced.collect { followStatus ->
                refreshOrders()
                if (followStatus) startBackgroundPolling()
            }
        }
    }

    private fun startStatusPolling() {
        viewModelScope.launch {
            while (true) {
                if (userProfile.value.id.isNotBlank() && navigation.currentTab == MainTab.ORDERS) {
                    refreshOrdersInternal()
                }
                delay(10000)
            }
        }
    }

    private suspend fun refreshOrdersInternal(userId: String = userProfile.value.id) {
        if (userId.isBlank()) return

        try {
            val updatedOrders = orderRepository.getCustomerOrders(userId)
            _state.update { state ->
                // Mantém o pedido aberto com o status mais recente
                val updatedSelected = state.selectedOrder?.let { selected -> updatedOrders.find { it.gid == selected.gid } }
                state.copy(orders = updatedOrders, selectedOrder = updatedSelected ?: state.selectedOrder)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshOrders() {
        if (userProfile.value.id.isBlank()) return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            refreshOrdersInternal()
            _state.update { it.copy(isLoading = false) }
        }
    }

    /** Depois de um pagamento automático: atualiza a cada 2s por até 30s para pegar a mudança de status. */
    private fun startBackgroundPolling() {
        viewModelScope.launch {
            repeat(15) {
                delay(2000)
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

    fun selectOrder(order: Order) {
        _state.update { it.copy(selectedOrder = order) }
    }

    fun clearOrderSelection() {
        _state.update { it.copy(selectedOrder = null) }
    }

    fun toggleFilter() {
        _state.update { it.copy(isFilterEnabled = !it.isFilterEnabled) }
    }

    fun rateOrderItem(orderId: String, productGid: String, restaurantGid: String, productName: String, rating: Int) {
        viewModelScope.launch {
            rateOrderItemUseCase(orderId, productGid, restaurantGid, productName, rating)
        }
    }

    fun markOrderAsDelivered(orderId: String) {
        viewModelScope.launch {
            val success = orderRepository.updateOrderStatus(orderId, "Entregue")
            if (success) {
                refreshOrdersInternal()
            } else {
                _state.update { it.copy(error = "Erro ao marcar pedido como entregue.") }
            }
        }
    }
}
