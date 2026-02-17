package org.leria.eats.project.service

interface MapService {
    fun openMapAndGetAddress(onAddressSelected: (String?) -> Unit)
}
