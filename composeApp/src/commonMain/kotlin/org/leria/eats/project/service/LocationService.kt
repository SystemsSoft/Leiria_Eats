package org.leria.eats.project.service

interface LocationService {
    suspend fun getCurrentAddress(): String?
    fun getAddressFromCoordinates(latitude: Double, longitude: Double): String?
}