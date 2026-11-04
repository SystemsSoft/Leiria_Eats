package org.leria.eats.project.presentation.state

/**
 * Estado em memória compartilhado entre os ViewModels das abas (sacola, conversa, catálogo,
 * navegação). Os stores são `single` no Koin, mas vivem o mesmo que a tela principal: o
 * MainViewModel chama [reset] em todos quando é destruído — igual ao estado que antes ficava
 * dentro de um único ViewModel.
 */
interface SessionStore {
    fun reset()
}
