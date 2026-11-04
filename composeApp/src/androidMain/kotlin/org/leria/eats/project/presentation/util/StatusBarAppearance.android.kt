package org.leria.eats.project.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import org.leria.eats.project.ActivityHolder

@Composable
actual fun StatusBarLightIcons(enabled: Boolean) {
    DisposableEffect(enabled) {
        val window = ActivityHolder.activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = !enabled
        onDispose {
            if (previous != null) controller.isAppearanceLightStatusBars = previous
        }
    }
}
