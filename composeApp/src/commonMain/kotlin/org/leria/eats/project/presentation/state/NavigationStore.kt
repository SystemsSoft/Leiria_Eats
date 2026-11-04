package org.leria.eats.project.presentation.state

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.leria.eats.project.presentation.MainTab

data class NavigationState(
    val currentTab: MainTab = MainTab.AI,
    val isBottomNavVisible: Boolean = false // Controla se o BottomBar está visível no chat
)

/** Aba atual do app. Compartilhada pelos ViewModels que mudam de aba (ex.: pagamento → Pedidos). */
class NavigationStore : SessionStore {
    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    private val _tabSelections = MutableSharedFlow<MainTab>(extraBufferCapacity = 8)
    /** Abas escolhidas pelo utilizador ([onTabSelected]) — não inclui as trocas automáticas ([navigateTo]). */
    val tabSelections: SharedFlow<MainTab> = _tabSelections.asSharedFlow()

    val currentTab: MainTab get() = _state.value.currentTab

    /** O utilizador escolheu a aba: os ViewModels interessados reagem via [tabSelections]. */
    fun onTabSelected(tab: MainTab) {
        navigateTo(tab)
        _tabSelections.tryEmit(tab)
    }

    /** Troca de aba feita pelo próprio app, sem os efeitos de uma escolha do utilizador. */
    fun navigateTo(tab: MainTab) {
        _state.update { it.copy(currentTab = tab) }
    }

    fun toggleBottomNav() {
        _state.update { it.copy(isBottomNavVisible = !it.isBottomNavVisible) }
    }

    override fun reset() {
        _state.value = NavigationState()
    }
}
