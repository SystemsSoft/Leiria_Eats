package org.leria.eats.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.leria.eats.project.data.ProfileRepository

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfile())

    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.userName.collect { name ->
                _uiState.update { it.copy(name = name) }
            }
        }

        viewModelScope.launch {
            repository.userAddress.collect { address ->
                _uiState.update { it.copy(address = address) }
            }
        }
    }

    fun saveProfile(name: String, address: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(name = name, address = address) }

            repository.saveProfile(name, address)
        }
    }
}