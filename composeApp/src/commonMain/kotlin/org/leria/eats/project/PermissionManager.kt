package org.leria.eats.project.permissions

import kotlinx.coroutines.flow.StateFlow

enum class PermissionStatus {
    GRANTED,
    DENIED,
    IDLE
}

interface PermissionManager {
    val status: StateFlow<PermissionStatus>
    fun askForPermission()
    fun openSettings()
}