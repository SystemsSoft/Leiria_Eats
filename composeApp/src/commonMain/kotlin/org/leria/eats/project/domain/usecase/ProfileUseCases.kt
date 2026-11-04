package org.leria.eats.project.domain.usecase

import org.leria.eats.project.domain.model.Address
import org.leria.eats.project.domain.repository.ProfileRepository

/** Salva o perfil local, gerando um id ("U-12345") no primeiro cadastro. */
class SaveUserProfileUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(
        currentId: String,
        name: String,
        email: String,
        phone: String,
        addresses: List<Address>,
        photoUrl: String? = null,
        allergies: String = "",
        lifestyles: String = ""
    ) {
        val newId = if (currentId.isBlank()) "U-${(10000..99999).random()}" else currentId
        profileRepository.saveProfile(newId, name, email, phone, addresses, photoUrl, allergies, lifestyles)
    }
}

/** Traz do servidor os cartões salvos do utilizador e guarda no perfil local. Nunca lança. */
class SyncSavedPaymentMethodsUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(userId: String) {
        try {
            val methods = profileRepository.fetchRemoteSavedPaymentMethods(userId)
            if (methods != null) {
                profileRepository.savePaymentMethods(methods)
                println("✅ Métodos de pagamento salvos: ${methods.size}")
            } else {
                println("ℹ️ Nenhum método de pagamento encontrado")
            }
        } catch (e: Exception) {
            println("⚠️ Erro ao buscar métodos de pagamento: ${e.message}")
            // Don't block the flow, just log the error
        }
    }
}
