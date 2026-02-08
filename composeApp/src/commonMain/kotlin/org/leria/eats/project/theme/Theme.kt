package org.leria.eats.project.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KomaAiColorScheme = darkColorScheme(
    primary = KomaYellow,
    onPrimary = KomaBlack,
    secondary = KomaLightGreen,
    background = KomaGreen,
    surface = KomaLightGreen,
    onBackground = KomaWhite,
    onSurface = KomaWhite,
    // Define other colors as needed, for example:
    // primaryContainer = KomaYellow,
    // onPrimaryContainer = KomaBlack,
    // error = Color.Red
)

@Composable
fun KomaAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KomaAiColorScheme,
        // typography = KomaTypography, // Future: Define custom typography if needed
        // shapes = KomaShapes,       // Future: Define custom shapes if needed
        content = content
    )
}