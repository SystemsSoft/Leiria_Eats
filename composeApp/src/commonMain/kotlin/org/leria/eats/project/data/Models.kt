package org.leria.eats.project.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ANTES: SearchRequest
// DEPOIS: ChatRequest com IA Generativa
@Serializable
data class ChatRequest(
    val message: String,                    // MUDOU: query → message
    @SerialName("restaurant_gid")
    val restaurantGid: String? = null,         // NOVO: de restaurantId: Int? para restaurantGid: String?
    @SerialName("session_id")
    val sessionId: String? = null          // NOVO
)

// Mantido para compatibilidade com código legado, mas não mais usado
@Deprecated("Use ChatRequest instead", ReplaceWith("ChatRequest(message = query)"))
@Serializable
data class SearchRequest(
    val query: String
)


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
    val category: String = "",
    @SerialName("preparation_time")
    val preparationTime: String = "",
    val quantity: Int = 1,
    val rating: Double? = null
)

@Serializable
data class Restaurant(
    val gid: String, // MUDOU: id: Int -> gid: String
    val name: String,
    val category: String,
    val rating: Double? = null,
    @SerialName("is_closed")
    val isClosed: Boolean? = null,
    val image_url: String?,
    val products: List<Product> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val plan: String? = null
)

// ANTES: SearchResponse
// DEPOIS: ChatResponse com IA Generativa
@Serializable
data class ChatResponse(
    val type: String? = null,               // NOVO: "chunk" ou "final"
    val text: String? = null,               // NOVO: usado em chunks de stream
    val response: String? = null,
    val intent: String? = null,
    val restaurantResults: List<Restaurant> = emptyList(),
    val productResults: List<Product> = emptyList(),
    val products: List<Product> = emptyList(),
    val cartProducts: List<Product> = emptyList(),
    @SerialName("show_cart")
    val showCart: Boolean = false,
    @SerialName("session_id")
    val sessionId: String? = null,
    val cart: JsonElement? = null           // NOVO: Captura o objeto cart se presente
)

@Deprecated("Use ChatResponse instead", ReplaceWith("ChatResponse(response = reply, products = productResults)"))
@Serializable
data class SearchResponse(
    val reply: String? = null,
    val intent: String? = null,
    val restaurantResults: List<Restaurant> = emptyList(),
    val productResults: List<Product> = emptyList()
)

@Serializable
data class CompanyResponse(
    val gid: String, // MUDOU: id: Int -> gid: String
    val name: String,
    val category: String,
    @SerialName("image_url")
    val imageUrl: String,
    val products: List<Product> = emptyList(),
    val plan: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)


@Serializable
data class OrderRequest(
    val gid: String = "", // Identificador único global do pedido (Master GID)
    val user_id: String,
    val user_name: String,
    val user_address: String,
    val user_phone: String,
    val save_payment_method: Boolean = false,
    @SerialName("search_query")
    val search_query: String = "",
    @SerialName("tracking_code")
    val tracking_code: String = "", // Código principal para o estafeta
    @SerialName("delivery_type")
    val deliveryType: String = "",
    @SerialName("delivery_latitude")
    val deliveryLatitude: Double? = null,
    @SerialName("delivery_longitude")
    val deliveryLongitude: Double? = null,
    @SerialName("total_delivery_fee")
    val totalDeliveryFee: Double = 0.0,
    @SerialName("total_service_fee")
    val totalServiceFee: Double = 0.0,
    
    // Lista de sub-pedidos (um para cada restaurante)
    @SerialName("sub_orders")
    val subOrders: List<SubOrderRequest> = emptyList()
)

@Serializable
data class SubOrderRequest(
    val gid: String = "", // Identificador próprio do sub-pedido
    @SerialName("order_gid")
    val orderGid: String = "", // Relacionamento com o pedido pai (OrderRequest.gid)
    @SerialName("restaurant_gid")
    val restaurantGid: String,
    @SerialName("restaurant_name")
    val restaurantName: String,
    @SerialName("restaurant_image_url")
    val restaurantImageUrl: String?,
    @SerialName("restaurant_category")
    val restaurantCategory: String,
    val items: List<OrderItemRequest>,
    @SerialName("delivery_fee")
    val deliveryFee: Double = 0.0,
    @SerialName("base_time")
    val baseTime: Int = 0
)

@Serializable
data class OrderItemRequest(
    val product_gid: String, // MUDOU: product_id: Int -> product_gid: String
    val quantity: Int,
    val observation: String? = null,
    @SerialName("product_name")
    val product_name: String,
    val price: Double,
    @SerialName("image_url")
    val image_url: String?,
    val description: String?
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

@Serializable
data class Address(
    val name: String,
    val address: String,
    val isDefault: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class SavedPaymentMethod(
    val id: Int,
    val brand: String,
    val last4: String,
    @SerialName("exp_month")
    val expMonth: Int,
    @SerialName("exp_year")
    val expYear: Int,
    @SerialName("stripe_payment_method_id")
    val stripePaymentMethodId: String
)

@Serializable
data class SavedPaymentMethodsResponse(
    @SerialName("has_saved_methods")
    val hasSavedMethods: Boolean,
    val methods: List<SavedPaymentMethod> = emptyList()
)

@Serializable
data class RatingItemRequest(
    @SerialName("product_gid")
    val productGid: String, // MUDOU: productId: Int -> productGid: String
    val rating: Int
)

@Serializable
data class RatingRequest(
    @SerialName("order_id")
    val orderId: String,
    @SerialName("restaurant_gid")
    val restaurantGid: String, // MUDOU: restaurantId: Int -> restaurantGid: String
    val ratings: List<RatingItemRequest>
)

@Serializable
data class RatingResponse(
    val success: Boolean,
    val message: String = ""
)

@Serializable
data class DeliveryFeeRequest(
    val customer_latitude: Double,
    val customer_longitude: Double,
    val restaurant_latitude: Double,
    val restaurant_longitude: Double,
    @SerialName("restaurant_gid")
    val restaurant_gid: String // MUDOU: restaurant_id: Int -> restaurant_gid: String
)

@Serializable
data class DeliveryFeeResponse(
    val distance_km: Double,
    val delivery_fee: Double,
    val tier: Int
)

@Serializable
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val addresses: List<Address> = emptyList(),
    @SerialName("saved_payment_methods")
    val savedPaymentMethods: List<SavedPaymentMethod> = emptyList(),
    val allergies: String = "",
    val lifestyles: String = ""
)