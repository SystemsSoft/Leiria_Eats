package org.leria.eats.project.presentation.util

import org.leria.eats.project.data.DeliveryFeeResponse
import org.leria.eats.project.data.Restaurant
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_KM = 6371.0

private fun Double.toRadians(): Double = this * PI / 180.0

/** Distância em linha reta entre duas coordenadas (fórmula de Haversine), em km. */
fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = (lat2 - lat1).toRadians()
    val dLon = (lon2 - lon1).toRadians()
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1.toRadians()) * cos(lat2.toRadians()) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_KM * c
}

/**
 * Taxa de recolha extra (custo de desvio) cobrada por restaurante adicional em um pedido
 * multi-restaurante, de acordo com a distância até o primeiro restaurante da rota.
 * Acima de 6.0 km (limite da operação) o valor é limitado ao teto de € 3,00.
 */
fun recolhaExtraFee(distanceKm: Double): Double = when {
    distanceKm <= 1.0 -> 0.80
    distanceKm <= 2.0 -> 1.20
    distanceKm <= 3.0 -> 1.60
    distanceKm <= 4.0 -> 2.00
    distanceKm <= 5.0 -> 2.50
    else -> 3.00
}

/**
 * Monta o mapa de taxas efetivamente cobradas por restaurante em um pedido multi-restaurante:
 * o primeiro restaurante da lista cobra a taxa de entrega normal (vinda da API), e os demais
 * cobram a taxa de recolha extra calculada pela distância até o primeiro. Um restaurante só
 * entra no mapa depois que sua entrada em [deliveryFeesMap] confirma que ele entrega no
 * endereço em questão (mesma validação de área usada para a taxa normal).
 */
fun buildChargedFeesMap(
    restaurants: List<Restaurant>,
    deliveryFeesMap: Map<String, DeliveryFeeResponse>
): Map<String, Double> {
    if (restaurants.isEmpty()) return emptyMap()
    val firstRestaurant = restaurants.first()
    return buildMap {
        deliveryFeesMap[firstRestaurant.gid]?.let { put(firstRestaurant.gid, it.delivery_fee) }
        restaurants.drop(1).forEach { restaurant ->
            val firstLat = firstRestaurant.latitude
            val firstLon = firstRestaurant.longitude
            val lat = restaurant.latitude
            val lon = restaurant.longitude
            if (deliveryFeesMap.containsKey(restaurant.gid) &&
                firstLat != null && firstLon != null && lat != null && lon != null
            ) {
                put(restaurant.gid, recolhaExtraFee(haversineDistanceKm(firstLat, firstLon, lat, lon)))
            }
        }
    }
}
