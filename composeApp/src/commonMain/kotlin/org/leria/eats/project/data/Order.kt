package org.leria.eats.project.data

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    val items: List<Product>,
    val total: Double,
    val status: String = "Em preparo",
    val date: String = "Hoje"
)