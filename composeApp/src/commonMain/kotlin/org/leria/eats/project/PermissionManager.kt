package org.leria.eats.project

// shared/src/commonMain/kotlin/.../PermissionManager.kt
import kotlinx.coroutines.flow.StateFlow

enum class PermissionStatus {
    GRANTED, DENIED, NOT_DETERMINED
}

interface MicrophonePermission {
    val status: StateFlow<PermissionStatus>
    fun requestPermission()
    fun openSettings() // Opcional: para mandar o usuário para as configs se ele negar
}