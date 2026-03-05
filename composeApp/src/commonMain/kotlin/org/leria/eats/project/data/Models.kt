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
    val quantity: Int = 1
)

@Serializable
data class Restaurant(
    val id: Int,
    val name: String,
    val category: String,
    val rating: Double? = null,
    val image_url: String?,
    val products: List<Product> = emptyList()
)

@Serializable
data class SearchResponse(
    val reply: String,
    val intent: String,
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
    val save_payment_method: Boolean = false
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
    val observation: String? = null
)

@Serializable
data class Address(
    val name: String,
    val address: String,
    val isDefault: Boolean = false
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
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val addresses: List<Address> = emptyList(),
    @SerialName("saved_payment_methods")
    val savedPaymentMethods: List<SavedPaymentMethod> = emptyList()
)