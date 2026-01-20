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
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val image_url: String?
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