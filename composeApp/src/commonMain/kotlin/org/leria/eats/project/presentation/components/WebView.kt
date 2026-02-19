package org.leria.eats.project.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun WebView(
    modifier: Modifier,
    url: String,
    onSuccess: (orderId: String) -> Unit,
    onCancel: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit = {}
)
