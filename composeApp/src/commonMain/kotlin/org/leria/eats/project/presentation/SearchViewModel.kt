package org.leria.eats.project.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.leria.eats.project.data.LeriaApiClient
import org.leria.eats.project.data.Restaurant

class SearchViewModel(private val apiClient: LeriaApiClient) : ViewModel() {

    // O Estado único da tela (Carregando, Lista, Erros, etc)
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Função chamada quando o usuário digita
    fun onQueryChange(text: String) {
        _uiState.update { it.copy(textInput = text) }
    }

    // Função chamada quando a Voz atualiza o texto
    fun updateInputFromVoice(text: String) {
        if (text.isNotBlank()) {
            _uiState.update { it.copy(textInput = text) }
        }
    }

    fun selectRestaurant(restaurant: Restaurant) {
        _uiState.update { it.copy(selectedRestaurant = restaurant) }
    }

    // Função chamada ao clicar em Voltar
    fun clearSelection() {
        _uiState.update { it.copy(selectedRestaurant = null) }
    }

    // A Lógica de Busca (que estava na tela antes)
    fun sendSearch() {
        val currentQuery = _uiState.value.textInput
        if (currentQuery.isBlank()) return

        // Inicia o carregamento
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val response = apiClient.sendChat(currentQuery)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        aiReply = response.reply,
                        restaurants = response.results,
                        textInput = "" // Limpa o campo após enviar (opcional)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erro ao conectar: ${e.message}"
                    )
                }
            }
        }
    }
}