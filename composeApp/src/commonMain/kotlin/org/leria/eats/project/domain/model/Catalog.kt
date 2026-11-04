package org.leria.eats.project.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Entidades do catálogo. Continuam @Serializable porque o mesmo formato é decodificado da API
// (LeriaApiClient, LiveConversationClient) e persistido no histórico do chat (ChatMessage no
// DataStore) — kotlinx.serialization é uma lib Kotlin pura, sem Ktor/Compose/Android.

@Serializable
data class Product(
    val gid: String = "", // MUDOU: id: Int -> gid: String
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    @SerialName("image_url")
    val image_url: String? = null,
    @SerialName("restaurant_gid")
    val restaurant_gid: String? = null, // Permite nulo para evitar erro de desserialização
    @SerialName("restaurant_name")
    val restaurantName: String? = null,
    @SerialName("restaurant_plan")
    val restaurantPlan: String? = null,
    val category: String = "",
    @SerialName("preparation_time")
    val preparationTime: String = "",
    val quantity: Int = 1,
    val rating: Double? = null,
    @SerialName("is_surprise_box")
    val isSurpriseBox: Boolean = false,
    @SerialName("surprise_box_pickup_start")
    val surpriseBoxPickupStart: String? = null,
    @SerialName("surprise_box_pickup_end")
    val surpriseBoxPickupEnd: String? = null
)

@Serializable
data class Restaurant(
    val gid: String = "", // MUDOU: id: Int -> gid: String
    val name: String = "",
    val category: String = "",
    val rating: Double? = null,
    @SerialName("is_closed")
    val isClosed: Boolean? = null,
    val image_url: String? = null,
    val products: List<Product> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val plan: String? = null
)

/** Resultado de POST /orders/delivery-fee para um restaurante. */
data class DeliveryFeeQuote(
    val distanceKm: Double,
    val deliveryFee: Double,
    val tier: Int
)
