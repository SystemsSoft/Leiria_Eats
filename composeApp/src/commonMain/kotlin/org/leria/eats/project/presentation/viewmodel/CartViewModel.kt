package org.leria.eats.project.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.state.CartRestaurantLoader
import org.leria.eats.project.presentation.state.CartState
import org.leria.eats.project.presentation.state.CartStore
import org.leria.eats.project.presentation.state.NavigationStore

/** Sacola: adicionar/remover itens a partir de qualquer aba e mensagens da sacola. */
class CartViewModel(
    private val cartStore: CartStore,
    private val cartRestaurantLoader: CartRestaurantLoader,
    private val navigation: NavigationStore,
) : ViewModel() {

    val state: StateFlow<CartState> = cartStore.state

    fun addToCart(product: Product) {
        val added = cartStore.add(product, fromAiTab = navigation.currentTab == MainTab.AI)
        if (added) product.restaurant_gid?.let { gid -> viewModelScope.launch { cartRestaurantLoader.load(gid) } }
    }

    fun removeFromCart(product: Product) = cartStore.removeOne(product)

    fun clearCart() = cartStore.clear()

    fun clearCartError() = cartStore.clearError()

    fun clearCartAiMessage() = cartStore.dismissAiMessage()

    fun markCartAiMessageAsSpoken() = cartStore.markAiMessageAsSpoken()
}
