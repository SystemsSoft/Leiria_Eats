package org.leria.eats.project

import App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
// Importe o App do commonMain
// Certifique-se de que o pacote está correto

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // A mágica acontece aqui: 
            // 1. O App() chama koinInject() internamente para pegar o Manager.
            // 2. O BindPermissionController() conecta o launcher.
            // A Activity não precisa fazer nada!
            App()
        }
    }
}