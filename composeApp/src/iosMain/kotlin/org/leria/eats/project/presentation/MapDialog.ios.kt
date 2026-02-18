package org.leria.eats.project.presentation

import androidx.compose.runtime.Composable

@Composable
actual fun MapDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double) -> Unit
) {
    // A implementação do mapa não está disponível para iOS nesta versão.
    // O Dialog simplesmente será fechado.
    onDismiss()
}