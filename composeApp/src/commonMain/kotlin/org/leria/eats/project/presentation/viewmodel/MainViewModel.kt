package org.leria.eats.project.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.leria.eats.project.presentation.MainTab
import org.leria.eats.project.presentation.state.NavigationState
import org.leria.eats.project.presentation.state.NavigationStore
import org.leria.eats.project.presentation.state.SessionStore

/** Navegação entre as abas da tela principal e ciclo de vida do estado compartilhado. */
class MainViewModel(
    private val navigation: NavigationStore,
    private val sessionStores: List<SessionStore>,
) : ViewModel() {

    val state: StateFlow<NavigationState> = navigation.state

    fun onTabSelected(tab: MainTab) = navigation.onTabSelected(tab)

    fun toggleBottomNav() = navigation.toggleBottomNav()

    // A tela principal acabou (Activity finalizada): descarta sacola, conversa, catálogo e aba,
    // como acontecia quando tudo vivia num único ViewModel.
    override fun onCleared() {
        sessionStores.forEach { it.reset() }
    }
}
