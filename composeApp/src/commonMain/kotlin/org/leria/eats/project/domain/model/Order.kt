package org.leria.eats.project.domain.model

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
    val date: String = "Hoje"
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

@Serializable
data class OrderItem(
    val product_name: String = "",
    val description: String = "",
    @SerialName("image_url")
    val imageUrl: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val observation: String? = null,
    @SerialName("product_gid")
    val productGid: String = "" // MUDOU: productId: Int -> productGid: String
)

// ─── Novo pedido (checkout) ─────────────────────────────────────────────────

/** Pedido "mestre" a enviar no checkout: um sub-pedido por restaurante. */
data class OrderDraft(
    val gid: String,
    val userId: String,
    val userName: String,
    val userAddress: String,
    val userPhone: String,
    val savePaymentMethod: Boolean,
    val searchQuery: String,
    val trackingCode: String,
    val deliveryType: String,
    val deliveryLatitude: Double?,
    val deliveryLongitude: Double?,
    val totalDeliveryFee: Double,
    val totalServiceFee: Double,
    val subOrders: List<SubOrderDraft>
)

data class SubOrderDraft(
    val gid: String,
    val orderGid: String,
    val restaurantGid: String,
    val restaurantName: String,
    val restaurantImageUrl: String?,
    val restaurantCategory: String,
    val items: List<OrderItemDraft>,
    val deliveryFee: Double,
    val baseTime: Int
)

data class OrderItemDraft(
    val productGid: String,
    val quantity: Int,
    val observation: String?,
    val productName: String,
    val price: Double,
    val imageUrl: String?,
    val description: String?
)

/** Resposta do servidor ao iniciar o checkout: pago na hora, PaymentSheet da Stripe ou URL. */
data class CheckoutSession(
    val url: String? = null,
    val autoPaid: Boolean = false,
    val orderId: Int? = null,
    val paymentIntentId: String? = null,
    val publishableKey: String? = null,
    val clientSecret: String? = null,
    val customerId: String? = null,
    val ephemeralKey: String? = null
)
