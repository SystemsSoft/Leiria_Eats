package org.leria.eats.project.data


import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val name: String = "",
    val phone: String = "",
    val address: String = ""
)