package org.leria.eats.project.presentation.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.leria.eats.project.domain.model.Restaurant

data class CatalogState(
    // Lista exclusiva do Home — carregada uma vez e nunca sobrescrita por pesquisas da IA
    val allRestaurants: List<Restaurant> = emptyList(),
    val selectedRestaurant: Restaurant? = null,
    val selectedCategory: String? = null,
    // Explorar aberto por "sugerir outro restaurante": tocar num restaurante abre o cardápio
    val isSuggestionMode: Boolean = false
)

/** Restaurantes do Explorar e o restaurante/categoria abertos. */
class CatalogStore : SessionStore {
    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    fun setAllRestaurants(restaurants: List<Restaurant>) {
        _state.update { it.copy(allRestaurants = restaurants) }
    }

    fun select(restaurant: Restaurant?) {
        _state.update { it.copy(selectedRestaurant = restaurant, selectedCategory = null) }
    }

    fun selectCategory(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
    }

    fun setSuggestionMode(enabled: Boolean) {
        _state.update { it.copy(isSuggestionMode = enabled) }
    }

    /** Fecha o restaurante/categoria e sai do modo sugestão (após um pedido). */
    fun resetSelection() {
        _state.update { it.copy(selectedRestaurant = null, selectedCategory = null, isSuggestionMode = false) }
    }

    /** Restaurante recém-carregado para a sacola: vira o aberto se não houver outro com cardápio. */
    fun offerSelectedRestaurant(restaurant: Restaurant) {
        _state.update { state ->
            val shouldUpdateRestaurant =
                state.selectedRestaurant == null ||
                    state.selectedRestaurant.gid == restaurant.gid ||
                    state.selectedRestaurant.products.isEmpty()
            if (shouldUpdateRestaurant) state.copy(selectedRestaurant = restaurant) else state
        }
    }

    override fun reset() {
        _state.value = CatalogState()
    }
}
