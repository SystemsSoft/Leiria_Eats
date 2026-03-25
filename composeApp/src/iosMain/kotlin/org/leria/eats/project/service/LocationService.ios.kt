package org.leria.eats.project.service

actual class LocationService {
    actual suspend fun getCurrentAddress(): String? {
        return "Endereço não disponível no iOS"
    }
    actual fun getAddressFromCoordinates(latitude: Double, longitude: Double): String? {
        return "Endereço não disponível no iOS"
    }
    actual suspend fun getCurrentCoordinates(): Pair<Double, Double>? {
        return null
    }
}
