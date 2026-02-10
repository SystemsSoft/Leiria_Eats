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
    val preparationTime: String = ""
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
    val user_id: String, // NOVO CAMPO: ID do usuário
    val user_name: String,
    val user_address: String,
    val user_phone: String,
    val restaurant_id: Int,
    val restaurant_name: String,
    val items: List<OrderItemRequest>
)

@Serializable
data class OrderItemRequest(
    val product_id: Int,
    val quantity: Int,
    val observation: String? = null
)

@Serializable
data class OrderItem(
    val product_name: String = "",
    val quantity: Int = 0,
    val observation: String? = null
)

@Serializable
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = ""
)