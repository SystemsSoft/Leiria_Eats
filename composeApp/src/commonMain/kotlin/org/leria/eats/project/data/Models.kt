package org.leria.eats.project.data

import kotlinx.serialization.Serializable

@Serializable
data class UserRequest(
    val text: String,
    val user_id: String = "mobile_user"
)


@Serializable
data class Product(
    val name: String,
    val price: Double,
    val description: String
)

@Serializable
data class Restaurant(
    val id: Int,
    val name: String,
    val category: String,
    val rating: Double,
    val menu: List<Product>
)

@Serializable
data class SearchResponse(
    val reply: String,
    val intent: String,
    val results: List<Restaurant> = emptyList()
)