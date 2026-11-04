package org.leria.eats.project.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.leria.eats.project.domain.model.Restaurant
import org.leria.eats.project.domain.repository.AiChatRepository
import org.leria.eats.project.domain.repository.ProfileRepository
import org.leria.eats.project.domain.repository.RestaurantRepository
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.state.CartStore
import org.leria.eats.project.presentation.state.CatalogState
import org.leria.eats.project.presentation.state.CatalogStore
import org.leria.eats.project.presentation.state.ConversationStore
import org.leria.eats.project.presentation.state.NavigationStore

data class ExploreState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/** Aba Explorar: lista de restaurantes, cardápio aberto e "sugerir outro restaurante". */
class ExploreViewModel(
    private val restaurantRepository: RestaurantRepository,
    private val aiChatRepository: AiChatRepository,
    profileRepository: ProfileRepository,
    private val catalogStore: CatalogStore,
    private val cartStore: CartStore,
    private val conversationStore: ConversationStore,
    private val navigation: NavigationStore,
) : ViewModel() {

    val catalog: StateFlow<CatalogState> = catalogStore.state

    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state.asStateFlow()

    private var initialRestaurantsLoaded = false

    init {
        viewModelScope.launch {
            profileRepository.userProfile.collect { profile ->
                // Utilizador registado: carrega todos os restaurantes automaticamente na primeira abertura
                val isUserRegistered = profile.name.isNotBlank() && profile.addresses.isNotEmpty()
                if (isUserRegistered && !initialRestaurantsLoaded) loadAllRestaurantsForHome()
            }
        }
        viewModelScope.launch {
            navigation.tabSelections.collect { tab ->
                if (tab == MainTab.HOME && !initialRestaurantsLoaded) loadAllRestaurantsForHome()
            }
        }
    }

    /**
     * Carrega todos os restaurantes para o Home. Só marca [initialRestaurantsLoaded] em caso de
     * sucesso — se a chamada falhar (rede instável, servidor reiniciando, etc.), a próxima
     * tentativa (ex.: reabrir a aba Explorar) tenta de novo, em vez de deixar a lista vazia pro
     * resto da sessão do app.
     */
    private fun loadAllRestaurantsForHome() {
        viewModelScope.launch {
            try {
                val restaurants = restaurantRepository.getAllRestaurants()
                if (restaurants.isNotEmpty()) {
                    catalogStore.setAllRestaurants(restaurants)
                    initialRestaurantsLoaded = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectRestaurant(restaurant: Restaurant) {
        if (restaurant.products.isEmpty()) {
            // Se o restaurante não tem produtos carregados, buscar pela API
            _state.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val resolvedRestaurant = restaurantRepository.getRestaurant(restaurant.gid) ?: restaurant
                catalogStore.select(resolvedRestaurant)
                _state.update { it.copy(isLoading = false) }
            }
        } else {
            // Restaurante já tem produtos carregados
            catalogStore.select(restaurant)
        }
    }

    fun selectRestaurantOrAddToCart(restaurant: Restaurant) {
        if (catalogStore.state.value.isSuggestionMode) {
            // Modo sugestão: abrir cardápio do restaurante selecionado
            _state.update { it.copy(isLoading = true) }
            catalogStore.setSuggestionMode(false)
            viewModelScope.launch {
                val resolvedRestaurant = restaurantRepository.getRestaurant(restaurant.gid) ?: restaurant
                catalogStore.select(resolvedRestaurant)
                _state.update { it.copy(isLoading = false) }
                navigation.navigateTo(MainTab.HOME)
            }
        } else {
            selectRestaurant(restaurant)
        }
    }

    fun selectCategory(category: String?) = catalogStore.selectCategory(category)

    fun clearSelection() = catalogStore.select(null)

    fun clearSelectionAndCart() {
        catalogStore.select(null)
        cartStore.clear()
    }

    fun suggestAnotherRestaurant() {
        val lastQuery = conversationStore.state.value.lastSearchQuery.trim()
        val suggestionQuery = if (lastQuery.isNotBlank()) "sugestão $lastQuery" else "sugestão"
        // Clear restaurant detail, cart and AI message, go HOME immediately
        _state.update { it.copy(isLoading = true, error = null) }
        cartStore.clearItems()
        catalogStore.select(null)
        catalogStore.setSuggestionMode(true)
        navigation.navigateTo(MainTab.HOME)
        viewModelScope.launch {
            try {
                // A resposta não é exibida (o Explorar mostra a lista completa) e nunca adiciona à
                // sacola; o pedido só regista a sugestão na sessão da IA.
                aiChatRepository.sendMessage(suggestionQuery)
                _state.update { it.copy(isLoading = false) }
                conversationStore.setTextInput("")
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Erro ao conectar: ${e.message}") }
                e.printStackTrace()
            }
        }
    }
}
