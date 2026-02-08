import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.leria.eats.project.BindPermissionController
import org.leria.eats.project.MainScreenWithAI
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.presentation.SplashScreen
import org.leria.eats.project.theme.KomaAITheme

@Composable
@Preview
fun App() {
    var showSplash by remember { mutableStateOf(true) }

    KomaAITheme {
        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        } else {
            val permissionManager = koinInject<PermissionManager>()
            BindPermissionController(permissionManager)
            MainScreenWithAI(permissionManager = permissionManager)
        }
    }
}
