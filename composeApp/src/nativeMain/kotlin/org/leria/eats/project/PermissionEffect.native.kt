package org.leria.eats.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import org.leria.eats.project.permissions.PermissionManager

@Composable
actual fun BindPermissionController(permissionManager: PermissionManager) {
    // Poll permission status every second so that when the user returns from
    // the iOS Settings app the UI reflects the updated state immediately.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            permissionManager.refreshStatus()
        }
    }
}