package org.leria.eats.project.presentation.util

import androidx.compose.runtime.Composable

/**
 * Alterna os ícones da status bar (window bar) para claros enquanto a tela que
 * chamou esta função estiver visível, revertendo para o padrão do app ao sair.
 * Usado por telas com uma barra superior escura (ex.: AiSearchScreen).
 */
@Composable
expect fun StatusBarLightIcons(enabled: Boolean)
