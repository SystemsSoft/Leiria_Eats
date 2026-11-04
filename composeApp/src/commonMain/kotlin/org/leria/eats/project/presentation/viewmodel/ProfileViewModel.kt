package org.leria.eats.project.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.leria.eats.project.domain.model.Address
import org.leria.eats.project.domain.model.UserProfile
import org.leria.eats.project.domain.repository.ProfileRepository
import org.leria.eats.project.domain.usecase.SaveUserProfileUseCase
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.state.NavigationStore

/** Perfil do utilizador (cadastro/onboarding e edição). */
class ProfileViewModel(
    profileRepository: ProfileRepository,
    private val saveUserProfile: SaveUserProfileUseCase,
    private val navigation: NavigationStore,
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> =
        profileRepository.userProfile.stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    init {
        viewModelScope.launch {
            profileRepository.userProfile.collect { profile ->
                // Utilizador sem perfil (nome + pelo menos um endereço) → abrir diretamente no Profile
                val isUserRegistered = profile.name.isNotBlank() && profile.addresses.isNotEmpty()
                if (!isUserRegistered) navigation.navigateTo(MainTab.PROFILE)
            }
        }
    }

    fun updateUserProfile(
        name: String,
        email: String,
        phone: String,
        addresses: List<Address>,
        photoUrl: String? = null,
        allergies: String = "",
        lifestyles: String = ""
    ) {
        viewModelScope.launch {
            saveUserProfile(userProfile.value.id, name, email, phone, addresses, photoUrl, allergies, lifestyles)
        }
    }
}
