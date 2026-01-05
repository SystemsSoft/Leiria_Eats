package org.leria.eats.project.data


import kotlinx.serialization.Serializable

@Serializable
data class UserRequest(
    val text: String,
    val user_id: String = "mobile_user"
)

@Serializable
data class AIResponse(
    val reply: String,
    val intent: String,
    val suggested_items: List<String> = emptyList()
)