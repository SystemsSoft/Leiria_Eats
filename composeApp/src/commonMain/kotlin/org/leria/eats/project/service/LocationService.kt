package org.leria.eats.project.service

interface LocationService {
    suspend fun getCurrentAddress(): String?
}