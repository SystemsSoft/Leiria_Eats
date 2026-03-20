package org.leria.eats.project.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    val items: List<OrderItem> = emptyList(),
    val total: Double,
    val status: String = "Em preparo",
    @SerialName("restaurant_id")
    val restaurantId: Int = 0,
    @SerialName("restaurant_name")
    val restaurantName: String = "",
    @SerialName("restaurant_category")
    val restaurantCategory: String = "",
    @SerialName("restaurant_image_url")
    val restaurantImageUrl: String = "",
    @SerialName("delivery_address")
    val deliveryAddress: String = "",
    val date: String = "Hoje",
    val isFavorite: Boolean = false,
    val nickname: String = "",
    @SerialName("search_query")
    val searchQuery: String = "",

    @SerialName("payment_intent_id")
    val paymentIntentId: String? = "",

    @SerialName("tracking_code")
    val trackingCode: String = "",

    @SerialName("delivery_type")
    val deliveryType: String = ""
)
