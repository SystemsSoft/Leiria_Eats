import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject // Importante: biblioteca koin-compose
import org.leria.eats.project.BindPermissionController
import org.leria.eats.project.permissions.PermissionManager

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {

        // 1. INJEÇÃO: O Koin decide se entrega AndroidPermissionManager ou IosPermissionManager
        val permissionManager = koinInject<PermissionManager>()

        // 2. REGISTRO DO LAUNCHER:
        // Precisamos dessa função auxiliar para conectar o launcher do Android
        // (Vou explicar como criar ela logo abaixo)
        BindPermissionController(permissionManager)

        // 3. UI: Passamos o manager pronto para uso
        MainScreenWithAI(permissionManager = permissionManager)
    }
}