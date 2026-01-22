import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.leria.eats.project.BindPermissionController
import org.leria.eats.project.MainScreenWithAI
import org.leria.eats.project.permissions.PermissionManager

private val GrayColorScheme = darkColorScheme(
    primary = Color(0xFFBDBDBD), // Cinza claro/Prateado
    secondary = Color(0xFF757575), // Cinza médio
    background = Color(0xFF121212), // Cinza quase preto
    surface = Color(0xFF1E1E1E), // Cinza escuro
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Color(0xFF424242),
    onPrimaryContainer = Color.White
)

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = GrayColorScheme) {
        val permissionManager = koinInject<PermissionManager>()
        BindPermissionController(permissionManager)

        MainScreenWithAI(permissionManager = permissionManager)
    }
}