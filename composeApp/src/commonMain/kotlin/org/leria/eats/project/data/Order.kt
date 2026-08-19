package org.leria.eats.project.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: Int,
    val gid: String,
    @SerialName("customer_name")
    val customerName: String = "",
    @SerialName("delivery_address")
    val deliveryAddress: String = "",
    val total: Double,
    val status: String,
    @SerialName("tracking_code")
    val trackingCode: String = "",
    @SerialName("delivery_type")
    val deliveryType: String? = null,
    @SerialName("delivery_latitude")
    val deliveryLatitude: Double? = null,
    @SerialName("delivery_longitude")
    val deliveryLongitude: Double? = null,
    @SerialName("total_delivery_fee")
    val totalDeliveryFee: Double = 0.0,
    @SerialName("total_service_fee")
    val totalServiceFee: Double = 0.0,
    @SerialName("sub_orders")
    val subOrders: List<SubOrder> = emptyList(),

    // UI Local state / compatibility
    val date: String = "Hoje",
    val isFavorite: Boolean = false,
    val nickname: String = "",
    @SerialName("search_query")
    val searchQuery: String = ""
)

@Serializable
data class SubOrder(
    val id: Int,
    val gid: String,
    @SerialName("restaurant_gid")
    val restaurantGid: String,
    @SerialName("restaurant_name")
    val restaurantName: String,
    @SerialName("restaurant_category")
    val restaurantCategory: String = "",
    @SerialName("restaurant_image_url")
    val restaurantImageUrl: String? = "",
    val status: String,
    val total: Double,
    @SerialName("delivery_fee")
    val deliveryFee: Double = 0.0,
    @SerialName("base_time")
    val baseTime: Int = 0,
    @SerialName("driver_name")
    val driverName: String? = null,
    val items: List<OrderItem> = emptyList()
)
