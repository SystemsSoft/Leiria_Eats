package org.leria.eats.project.domain.usecase

import kotlinx.coroutines.flow.first
import org.leria.eats.project.domain.repository.OrderRepository

/**
 * Guarda a nota localmente (a UI atualiza na hora) e envia ao servidor. Quando a API devolveu o
 * pedido sem productGid/restaurantGid, usa os mapeamentos guardados no checkout.
 */
class RateOrderItemUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(
        orderId: String,
        productGid: String,
        restaurantGid: String,
        productName: String,
        rating: Int
    ) {
        orderRepository.saveOrderItemRating(orderId, productName, rating)

        val resolvedProductGid = productGid.ifBlank {
            orderRepository.orderProductGids.first()["$orderId::$productName"] ?: ""
        }
        val resolvedRestaurantGid = restaurantGid.ifBlank {
            orderRepository.orderRestaurantGids.first()[orderId] ?: ""
        }

        if (resolvedProductGid.isBlank()) {
            println("⚠️ productGid não encontrado para: orderId=$orderId, productName=$productName")
            return
        }
        if (resolvedRestaurantGid.isBlank()) {
            println("⚠️ restaurantGid não encontrado para: orderId=$orderId")
            return
        }

        val sent = orderRepository.submitRating(orderId, resolvedRestaurantGid, resolvedProductGid, rating)
        if (!sent) {
            println("⚠️ Falha ao enviar avaliação: orderId=$orderId, productGid=$resolvedProductGid, restaurantGid=$resolvedRestaurantGid")
        } else {
            println("✅ Avaliação enviada: orderId=$orderId, productGid=$resolvedProductGid, restaurantGid=$resolvedRestaurantGid, rating=$rating")
        }
    }
}
