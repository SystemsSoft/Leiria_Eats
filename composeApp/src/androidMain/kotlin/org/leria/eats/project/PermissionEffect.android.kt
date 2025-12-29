package org.leria.eats.project

import androidx.compose.runtime.Composable
import org.leria.eats.project.permissions.PermissionManager


import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import org.leria.eats.project.permissions.AndroidPermissionManager

@Composable
actual fun BindPermissionController(permissionManager: PermissionManager) {
    // Só executa se for a instância Android
    if (permissionManager is AndroidPermissionManager) {

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permissionManager.onPermissionResult(isGranted)
        }

        DisposableEffect(permissionManager) {
            permissionManager.launcher = {
                launcher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
            onDispose { permissionManager.launcher = null }
        }
    }
}