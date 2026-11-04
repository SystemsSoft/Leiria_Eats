package org.leria.eats.project.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val addresses: List<Address> = emptyList(),
    @SerialName("saved_payment_methods")
    val savedPaymentMethods: List<SavedPaymentMethod> = emptyList(),
    val allergies: String = "",
    val lifestyles: String = ""
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
