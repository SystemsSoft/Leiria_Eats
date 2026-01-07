package org.leria.eats.project.presentation


import org.leria.eats.project.data.Restaurant

data class SearchUiState(
    val isLoading: Boolean = false,
    val textInput: String = "",
    val aiReply: String = "Olá! O que vamos comer hoje?",
    val restaurants: List<Restaurant> = emptyList(), // A lista de cards
    val error: String? = null
)