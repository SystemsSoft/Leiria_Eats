package org.leria.eats.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.leria.eats.project.data.local.UserPreferencesDataSource
import org.leria.eats.project.data.remote.LeriaApiClient
import org.leria.eats.project.data.remote.dto.DeliveryFeeRequest
import org.leria.eats.project.data.remote.dto.RatingItemRequest
import org.leria.eats.project.data.remote.dto.RatingRequest
import org.leria.eats.project.data.remote.dto.toDomain
import org.leria.eats.project.data.remote.dto.toRequest
import org.leria.eats.project.data.remote.dto.toRestaurant
import org.leria.eats.project.domain.model.Address
import org.leria.eats.project.domain.model.ChatReply
import org.leria.eats.project.domain.model.CheckoutSession
import org.leria.eats.project.domain.model.DeliveryFeeQuote
import org.leria.eats.project.domain.model.Order
import org.leria.eats.project.domain.model.OrderDraft
import org.leria.eats.project.domain.model.Restaurant
import org.leria.eats.project.domain.model.SavedPaymentMethod
import org.leria.eats.project.domain.model.UserProfile
import org.leria.eats.project.domain.repository.AiChatRepository
import org.leria.eats.project.domain.repository.OrderRepository
import org.leria.eats.project.domain.repository.ProfileRepository
import org.leria.eats.project.domain.repository.RestaurantRepository
import org.leria.eats.project.domain.repository.SpeechRepository

class RestaurantRepositoryImpl(private val api: LeriaApiClient) : RestaurantRepository {
    override suspend fun getAllRestaurants(): List<Restaurant> = api.getAllRestaurants()

    override suspend fun getRestaurant(gid: String): Restaurant? = api.getCompanyByGid(gid)?.toRestaurant()
}

class AiChatRepositoryImpl(private val api: LeriaApiClient) : AiChatRepository {
    override fun currentSessionId(): String = api.currentSessionId()

    override fun streamMessage(text: String): Flow<ChatReply> =
        api.sendChatMessageStream(text).map { it.toDomain() }

    override suspend fun sendMessage(text: String): ChatReply = api.sendChatMessage(text).toDomain()

    override suspend fun confirmSession(sessionId: String): Boolean = api.confirmOrderSession(sessionId)

    override suspend fun restartSession(sessionId: String): Boolean = api.restartChatSession(sessionId)
}

class SpeechRepositoryImpl(private val api: LeriaApiClient) : SpeechRepository {
    override suspend fun synthesize(text: String): ByteArray? = api.synthesizeSpeech(text)
}

class OrderRepositoryImpl(
    private val api: LeriaApiClient,
    private val preferences: UserPreferencesDataSource,
) : OrderRepository {
    override suspend fun getCustomerOrders(userId: String): List<Order> = api.getCustomerOrders(userId)

    override suspend fun initiateCheckout(order: OrderDraft): CheckoutSession? =
        api.initiateCheckout(order.toRequest())?.toDomain()

    override suspend fun updateOrderStatus(orderId: String, newStatus: String): Boolean =
        api.updateOrderStatus(orderId, newStatus)

    override suspend fun submitRating(orderId: String, restaurantGid: String, productGid: String, rating: Int): Boolean {
        val request = RatingRequest(
            orderId = orderId,
            restaurantGid = restaurantGid,
            ratings = listOf(RatingItemRequest(productGid = productGid, rating = rating))
        )
        return api.submitRatings(request)?.success == true
    }

    override suspend fun getDeliveryFee(
        customerLat: Double,
        customerLon: Double,
        restaurantLat: Double,
        restaurantLon: Double,
        restaurantGid: String
    ): DeliveryFeeQuote = api.getDeliveryFee(
        DeliveryFeeRequest(
            customer_latitude = customerLat,
            customer_longitude = customerLon,
            restaurant_latitude = restaurantLat,
            restaurant_longitude = restaurantLon,
            restaurant_gid = restaurantGid
        )
    ).toDomain()

    override val orderSearchQueries: Flow<Map<String, String>> = preferences.orderSearchQueriesFlow
    override val orderItemRatings: Flow<Map<String, Int>> = preferences.orderItemRatingsFlow
    override val orderProductGids: Flow<Map<String, String>> = preferences.orderProductGidsFlow
    override val orderRestaurantGids: Flow<Map<String, String>> = preferences.orderRestaurantGidsFlow

    override suspend fun saveOrderSearchQuery(orderId: String, searchQuery: String) =
        preferences.saveOrderSearchQuery(orderId, searchQuery)

    override suspend fun saveOrderItemRating(orderId: String, productName: String, rating: Int) =
        preferences.saveOrderItemRating(orderId, productName, rating)

    override suspend fun saveOrderProductGids(orderId: String, items: List<Pair<String, String>>) =
        preferences.saveOrderProductGids(orderId, items)

    override suspend fun saveOrderRestaurantGid(orderId: String, restaurantGid: String) =
        preferences.saveOrderRestaurantGid(orderId, restaurantGid)
}

class ProfileRepositoryImpl(
    private val api: LeriaApiClient,
    private val preferences: UserPreferencesDataSource,
) : ProfileRepository {
    override val userProfile: Flow<UserProfile> = preferences.userProfileFlow

    override suspend fun saveProfile(
        id: String,
        name: String,
        email: String,
        phone: String,
        addresses: List<Address>,
        photoUrl: String?,
        allergies: String,
        lifestyles: String
    ) = preferences.saveProfile(id, name, email, phone, addresses, photoUrl, allergies, lifestyles)

    override suspend fun fetchRemoteSavedPaymentMethods(userId: String): List<SavedPaymentMethod>? =
        api.getSavedPaymentMethods(userId)?.takeIf { it.hasSavedMethods }?.methods

    override suspend fun savePaymentMethods(methods: List<SavedPaymentMethod>) =
        preferences.savePaymentMethods(methods)
}
