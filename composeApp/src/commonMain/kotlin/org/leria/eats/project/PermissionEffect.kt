package org.leria.eats.project

import androidx.compose.runtime.Composable
import org.leria.eats.project.permissions.PermissionManager

@Composable
expect fun BindPermissionController(permissionManager: PermissionManager)