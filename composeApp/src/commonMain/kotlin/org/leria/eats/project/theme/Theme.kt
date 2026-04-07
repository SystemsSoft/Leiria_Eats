package org.leria.eats.project.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KomaAiColorScheme = lightColorScheme(
    primary          = KomaGold,
    onPrimary        = KomaGoldOnDark,
    secondary        = KomaBrandGreen,
    onSecondary      = KomaWhite,
    background       = KomaBg,
    onBackground     = KomaTextPrimary,
    surface          = KomaSurface,
    onSurface        = KomaTextPrimary,
    surfaceVariant   = KomaCard,
    onSurfaceVariant = KomaTextSec,
    error            = KomaError,
    onError          = KomaWhite,
    outline          = KomaDivider
)

@Composable
fun KomaAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KomaAiColorScheme,
        content = content
    )
}