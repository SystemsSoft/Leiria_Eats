package org.leria.eats.project.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchRequest(
    val query: String
)


@Serializable
data class Product(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val image_url: String? = null,
    val restaurant_id: Int = 0,
    val category: String = "",
    @SerialName("preparation_time")
    val preparationTime: String = "",
    val quantity: Int = 1,
    val rating: Double? = null
)

@Serializable
data class Restaurant(
    val id: Int,
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

@Serializable
data class SearchResponse(
    val reply: String? = null,
    val intent: String? = null,
    val restaurantResults: List<Restaurant> = emptyList(),
    val productResults: List<Product> = emptyList()
)

@Serializable
data class CompanyResponse(
    val id: Int,
    val name: String,
    val category: String,
    @SerialName("image_url")
    val imageUrl: String,
    val products: List<Product> = emptyList()
)


@Serializable
data class OrderRequest(
    val user_id: String,
    val user_name: String,
    val user_address: String,
    val user_phone: String,
    val restaurant_id: Int,
    val restaurant_name: String,
    @SerialName("restaurant_image_url")
    val restaurant_image_url: String?,
    @SerialName("restaurant_category")
    val restaurant_category: String,
    val items: List<OrderItemRequest>,
    val save_payment_method: Boolean = false,
    @SerialName("search_query")
    val search_query: String = "",
    @SerialName("tracking_code")
    val tracking_code: String = "",
    @SerialName("delivery_type")
    val deliveryType: String = "",
    @SerialName("base_time")
    val baseTime: Int = 0,
    @SerialName("delivery_latitude")
    val deliveryLatitude: Double? = null,
    @SerialName("delivery_longitude")
    val deliveryLongitude: Double? = null,
    @SerialName("delivery_fee")
    val deliveryFee: Double = 0.0,
    @SerialName("service_fee")
    val serviceFee: Double = 0.0
)

@Serializable
data class OrderItemRequest(
    val product_id: Int,
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
    @SerialName("product_id")
    val productId: Int = 0
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
    @SerialName("product_id")
    val productId: Int,
    val rating: Int
)

@Serializable
data class RatingRequest(
    @SerialName("order_id")
    val orderId: String,
    @SerialName("restaurant_id")
    val restaurantId: Int,
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
    val restaurant_id: Int
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
    val addresses: List<Address> = emptyList(),
    @SerialName("saved_payment_methods")
    val savedPaymentMethods: List<SavedPaymentMethod> = emptyList()
)