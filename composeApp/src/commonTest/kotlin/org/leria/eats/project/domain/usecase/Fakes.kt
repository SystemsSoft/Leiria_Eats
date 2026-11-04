package org.leria.eats.project.domain.usecase

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.leria.eats.project.domain.model.ChatReply
import org.leria.eats.project.domain.model.CheckoutSession
import org.leria.eats.project.domain.model.DeliveryFeeQuote
import org.leria.eats.project.domain.model.Order
import org.leria.eats.project.domain.model.OrderDraft
import org.leria.eats.project.domain.model.Restaurant
import org.leria.eats.project.domain.repository.AiChatRepository
import org.leria.eats.project.domain.repository.OrderRepository
import org.leria.eats.project.domain.repository.RestaurantRepository

/**
 * Roda um bloco suspend até o fim, sem kotlinx-coroutines-test: os fakes abaixo nunca suspendem
 * de verdade, então a corrotina termina antes de startCoroutine retornar.
 */
fun <T> runSuspend(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return checkNotNull(result) { "o bloco suspendeu de verdade" }.getOrThrow()
}

class FakeOrderRepository(
    var checkoutSession: CheckoutSession? = CheckoutSession(),
    var ratingAccepted: Boolean = true,
) : OrderRepository {
    val drafts = mutableListOf<OrderDraft>()
    val submittedRatings = mutableListOf<List<Any>>()

    override val orderSearchQueries = MutableStateFlow<Map<String, String>>(emptyMap())
    override val orderItemRatings = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val orderProductGids = MutableStateFlow<Map<String, String>>(emptyMap())
    override val orderRestaurantGids = MutableStateFlow<Map<String, String>>(emptyMap())

    override suspend fun getCustomerOrders(userId: String): List<Order> = emptyList()

    override suspend fun initiateCheckout(order: OrderDraft): CheckoutSession? {
        drafts += order
        return checkoutSession
    }

    override suspend fun updateOrderStatus(orderId: String, newStatus: String) = true

    override suspend fun submitRating(orderId: String, restaurantGid: String, productGid: String, rating: Int): Boolean {
        submittedRatings += listOf(orderId, restaurantGid, productGid, rating)
        return ratingAccepted
    }

    override suspend fun getDeliveryFee(
        customerLat: Double, customerLon: Double, restaurantLat: Double, restaurantLon: Double, restaurantGid: String
    ) = DeliveryFeeQuote(distanceKm = 1.0, deliveryFee = 1.0, tier = 1)

    override suspend fun saveOrderSearchQuery(orderId: String, searchQuery: String) {
        orderSearchQueries.value += orderId to searchQuery
    }

    override suspend fun saveOrderItemRating(orderId: String, productName: String, rating: Int) {
        orderItemRatings.value += "$orderId::$productName" to rating
    }

    override suspend fun saveOrderProductGids(orderId: String, items: List<Pair<String, String>>) {
        orderProductGids.value += items.associate { (name, gid) -> "$orderId::$name" to gid }
    }

    override suspend fun saveOrderRestaurantGid(orderId: String, restaurantGid: String) {
        orderRestaurantGids.value += orderId to restaurantGid
    }
}

class FakeRestaurantRepository(private val restaurants: List<Restaurant> = emptyList()) : RestaurantRepository {
    val requestedGids = mutableListOf<String>()

    override suspend fun getAllRestaurants() = restaurants

    override suspend fun getRestaurant(gid: String): Restaurant? {
        requestedGids += gid
        return restaurants.find { it.gid == gid }
    }
}

class FakeAiChatRepository : AiChatRepository {
    val confirmedSessions = mutableListOf<String>()

    override fun currentSessionId() = "session_test"
    override fun streamMessage(text: String): Flow<ChatReply> = emptyFlow()
    override suspend fun sendMessage(text: String) = ChatReply()
    override suspend fun confirmSession(sessionId: String): Boolean {
        confirmedSessions += sessionId
        return true
    }
    override suspend fun restartSession(sessionId: String) = true
}
