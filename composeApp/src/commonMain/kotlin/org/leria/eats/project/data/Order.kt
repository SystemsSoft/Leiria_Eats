package org.leria.eats.project.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    val items: List<OrderItem> = emptyList(),
    val total: Double,
    val status: String = "Em preparo",
    @SerialName("restaurant_name")
    val restaurantName: String = "",
    @SerialName("restaurant_category")
    val restaurantCategory: String = "",
    @SerialName("restaurant_image_url")
    val restaurantImageUrl: String = "",
    val date: String = "Hoje",
    val isFavorite: Boolean = false,

    @SerialName("payment_intent_id")
    val paymentIntentId: String? = ""
)
