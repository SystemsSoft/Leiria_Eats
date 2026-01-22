package org.leria.eats.project.data

import kotlinx.serialization.Serializable

@Serializable
data class UserRequest(
    val text: String,
    val user_id: String = "mobile_user"
)

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
    val image_url: String? = null
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
    val results: List<Restaurant> = emptyList()
)

@Serializable
data class OrderRequest(
    val user_name: String,
    val user_address: String,
    val user_phone: String,
    val restaurant_id: Int,
    val items: List<OrderItemRequest>
)

@Serializable
data class OrderItemRequest(
    val product_id: Int,
    val quantity: Int,
    val observation: String? = null
)

@Serializable
data class UserProfile(
    val name: String = "",
    val phone: String = "",
    val address: String = ""
)