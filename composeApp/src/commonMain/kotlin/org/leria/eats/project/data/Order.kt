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
    val date: String = "Hoje",
    val isFavorite: Boolean = false
)