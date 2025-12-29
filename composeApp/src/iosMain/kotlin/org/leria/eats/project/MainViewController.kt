package org.leria.eats.project

import App
import androidx.compose.ui.window.ComposeUIViewController

// O iOS chama essa função para desenhar a tela
fun MainViewController() = ComposeUIViewController {
    // Chama o App comum.
    // O App() vai chamar koinInject(), que vai pegar o módulo que definimos no passo 1.
    App()
}