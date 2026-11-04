package org.leria.eats.project.presentation.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.domain.model.Restaurant
import org.leria.eats.project.domain.repository.RestaurantRepository
import org.leria.eats.project.domain.usecase.CartRules

data class CartState(
    val items: List<Product> = emptyList(),
    val restaurants: List<Restaurant> = emptyList(), // Metadados dos restaurantes no carrinho
    val restaurantGid: String? = null, // restaurante "principal" da sacola
    val isAiCartFlow: Boolean = false, // Indica se o carrinho atual veio de um fluxo da IA
    val error: String? = null,
    val aiMessage: String? = null,
    val aiMessageSpoken: Boolean = false // Track if AI message has been spoken
)

/**
 * A sacola é uma só para o app inteiro: Explorar, chat da IA, ligação de voz e checkout mexem
 * nela. Só guarda estado; as regras ficam em [CartRules].
 */
class CartStore : SessionStore {
    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state.asStateFlow()

    /**
     * Adiciona o produto (somando a quantidade se já estiver na sacola). Retorna false se a regra
     * de exclusividade da Caixa Surpresa bloqueou — nesse caso [CartState.error] explica o motivo.
     *
     * @param fromAiTab adicionar a partir da aba da IA mantém/ativa o fluxo de Sacola IA.
     */
    fun add(product: Product, fromAiTab: Boolean): Boolean {
        // Exclusividade da Caixa Surpresa (ver CartRules) — só UX; o gate real é o checkout no servidor.
        if (CartRules.violatesSurpriseBoxExclusivity(_state.value.items, product)) {
            _state.update {
                it.copy(
                    error = "Um pedido com Caixa Surpresa é exclusivo — não é possível " +
                        "adicionar outro produto. Finalize este pedido ou remova o item de " +
                        "Caixa Surpresa para continuar a comprar."
                )
            }
            return false
        }
        _state.update { current ->
            current.copy(
                items = CartRules.addProduct(current.items, product),
                restaurantGid = product.restaurant_gid,
                isAiCartFlow = if (fromAiTab) true else current.isAiCartFlow
            )
        }
        return true
    }

    fun removeOne(product: Product) {
        _state.update { current ->
            val updatedCart = CartRules.removeOneUnit(current.items, product)

            // Atualiza a lista de metadados se um restaurante não tiver mais produtos
            val remainingRestaurantGids = updatedCart.mapNotNull { it.restaurant_gid }.toSet()
            val updatedCartRestaurants = current.restaurants.filter { it.gid in remainingRestaurantGids }

            val newRestaurantGid = if (updatedCart.isEmpty()) null else {
                // Se o restaurante removido era o "principal", escolhe o próximo disponível
                if (product.restaurant_gid == current.restaurantGid && product.restaurant_gid !in remainingRestaurantGids) {
                    remainingRestaurantGids.firstOrNull()
                } else {
                    current.restaurantGid
                }
            }

            current.copy(
                items = updatedCart,
                restaurantGid = newRestaurantGid,
                restaurants = updatedCartRestaurants
            )
        }
    }

    /**
     * Substitui a sacola pelo estado ATUAL enviado pela IA (chat ou ligação), permitindo editar a
     * sacola pela conversa. Retorna os restaurantes cujos metadados devem ser carregados.
     */
    fun replaceWithAiCart(products: List<Product>): List<String> {
        val filteredProducts = CartRules.sanitizeAiCart(products)
        if (filteredProducts.isEmpty()) return emptyList()

        _state.update { current ->
            current.copy(
                items = filteredProducts,
                restaurantGid = filteredProducts.firstOrNull()?.restaurant_gid ?: current.restaurantGid,
                isAiCartFlow = true
            )
        }
        return filteredProducts.mapNotNull { it.restaurant_gid }.distinct()
    }

    fun showAiCart() {
        _state.update { it.copy(isAiCartFlow = true) }
    }

    fun putRestaurant(restaurant: Restaurant) {
        _state.update { current ->
            val updated = if (current.restaurants.none { it.gid == restaurant.gid }) {
                current.restaurants + restaurant
            } else {
                current.restaurants.map { if (it.gid == restaurant.gid) restaurant else it }
            }
            current.copy(restaurants = updated)
        }
    }

    fun clear() {
        _state.update {
            it.copy(
                items = emptyList(),
                restaurantGid = null,
                restaurants = emptyList(),
                isAiCartFlow = false
            )
        }
    }

    /** Esvazia os itens (sugestão de outro restaurante / pagamento), mantendo o resto do estado. */
    fun clearItems() {
        _state.update { it.copy(items = emptyList(), restaurantGid = null, aiMessage = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun dismissAiMessage() {
        _state.update { it.copy(aiMessage = null) }
    }

    fun markAiMessageAsSpoken() {
        _state.update { it.copy(aiMessageSpoken = true) }
    }

    override fun reset() {
        _state.value = CartState()
    }
}

/**
 * Carrega os metadados (nome, foto, cardápio, coordenadas) de um restaurante que entrou na
 * sacola. Também atualiza o restaurante aberto no Explorar quando ele é o mesmo ou ainda não tem
 * cardápio.
 */
class CartRestaurantLoader(
    private val restaurantRepository: RestaurantRepository,
    private val cartStore: CartStore,
    private val catalogStore: CatalogStore,
) {
    suspend fun load(gid: String) {
        val restaurant = restaurantRepository.getRestaurant(gid) ?: return
        cartStore.putRestaurant(restaurant)
        catalogStore.offerSelectedRestaurant(restaurant)
    }
}
