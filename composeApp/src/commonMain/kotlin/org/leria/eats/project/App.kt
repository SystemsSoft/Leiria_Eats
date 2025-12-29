import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject // Importante: biblioteca koin-compose
import org.leria.eats.project.BindPermissionController
import org.leria.eats.project.MainScreenWithAI
import org.leria.eats.project.permissions.PermissionManager

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {

        val permissionManager = koinInject<PermissionManager>()
        BindPermissionController(permissionManager)

        MainScreenWithAI(permissionManager = permissionManager)
    }
}