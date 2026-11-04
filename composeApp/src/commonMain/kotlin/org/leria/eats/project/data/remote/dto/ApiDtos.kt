package org.leria.eats.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.domain.model.Restaurant
import org.leria.eats.project.domain.model.SavedPaymentMethod

// Formato de rede da API (api.leiriaeats.com). Convertidos para o domínio em Mappers.kt.

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

@Serializable
data class CompanyResponse(
    val gid: String = "", // MUDOU: id: Int -> gid: String
    val name: String = "",
    val category: String = "",
    @SerialName("image_url")
    val imageUrl: String? = null,
    val products: List<Product> = emptyList(),
    val plan: String? = null,
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
    // O backend (POST /orders/ratings) não manda essa chave — a resposta real é
    // {"message": ..., "rated_products": [...]}. Sem default aqui, a desserialização
    // sempre falhava (MissingFieldException) mesmo quando a avaliação era salva com
    // sucesso, fazendo o client sempre logar "falha ao enviar avaliação".
    val success: Boolean = true,
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
data class PaymentIntentResponse(
    val url: String? = null,
    val auto_paid: Boolean = false,
    val order_id: Int? = null,
    val payment_intent_id: String? = null,
    val publishableKey: String? = null,
    val clientSecret: String? = null,
    val customerId: String? = null,
    val ephemeralKey: String? = null
)
