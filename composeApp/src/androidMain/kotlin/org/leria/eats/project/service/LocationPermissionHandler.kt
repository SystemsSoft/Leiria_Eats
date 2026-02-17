package org.leria.eats.project.service

interface LocationPermissionHandler {
    fun requestLocationPermission(onResult: (Boolean) -> Unit)
}
