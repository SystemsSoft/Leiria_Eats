package org.leria.eats.project.domain.usecase

import org.leria.eats.project.domain.model.Address
import org.leria.eats.project.domain.model.CheckoutSession
import org.leria.eats.project.domain.model.OrderDraft
import org.leria.eats.project.domain.model.OrderItemDraft
import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.domain.model.Restaurant
import org.leria.eats.project.domain.model.SubOrderDraft
import org.leria.eats.project.domain.model.UserProfile
import org.leria.eats.project.domain.repository.AiChatRepository
import org.leria.eats.project.domain.repository.OrderRepository
import org.leria.eats.project.domain.repository.RestaurantRepository
import org.leria.eats.project.util.Ulid

sealed interface PlaceOrderResult {
    /** O servidor aceitou o pedido; [session] diz como o pagamento continua. */
    data class Started(val session: CheckoutSession) : PlaceOrderResult
    /** Nenhum item da sacola pôde ser associado a um restaurante. */
    data object NoValidItems : PlaceOrderResult
    /** O servidor não devolveu uma sessão de pagamento. */
    data object CheckoutUnavailable : PlaceOrderResult
}

/**
 * Monta o pedido "mestre" (um sub-pedido por restaurante da sacola), inicia o checkout no
 * servidor e guarda localmente os metadados do pedido usados depois nas avaliações.
 */
class PlaceOrderUseCase(
    private val orderRepository: OrderRepository,
    private val restaurantRepository: RestaurantRepository,
    private val aiChatRepository: AiChatRepository,
) {
    suspend operator fun invoke(
        cartItems: List<Product>,
        cartRestaurants: List<Restaurant>,
        profile: UserProfile,
        address: Address,
        savePaymentMethod: Boolean,
        deliveryType: String,
        deliveryFee: Double,
        serviceFee: Double,
        deliveryFeesMap: Map<String, Double>,
        searchQuery: String,
        aiSessionId: String?,
    ): PlaceOrderResult {
        // Se o pedido veio da IA, notifica o servidor para confirmar a sessão
        aiSessionId?.let { sessionId ->
            println("🚀 Confirmando sessão da IA no servidor: $sessionId")
            aiChatRepository.confirmSession(sessionId)
        }

        val itemsByRestaurantGid = cartItems.groupBy { it.restaurant_gid }
        val masterGid = generateGid("mst")
        val trackingCode = generateTrackingCode()
        val subOrders = mutableListOf<SubOrderDraft>()

        for ((restaurantGid, products) in itemsByRestaurantGid) {
            if (restaurantGid == null) continue

            // 1. Obter metadados do restaurante
            val restaurant = cartRestaurants.find { it.gid == restaurantGid }
                ?: restaurantRepository.getRestaurant(restaurantGid)
                ?: continue

            // 2. Preparar itens do sub-pedido
            val orderItems = products.map { product ->
                OrderItemDraft(
                    productGid = product.gid,
                    quantity = product.quantity,
                    observation = null,
                    productName = product.name,
                    price = product.price,
                    imageUrl = product.image_url,
                    description = product.description
                )
            }

            val subBaseTime = products.maxOfOrNull { p ->
                p.preparationTime.filter { it.isDigit() || it == '.' }.toDoubleOrNull()?.toInt() ?: 0
            } ?: 0

            val subDeliveryFee = deliveryFeesMap[restaurantGid] ?: (deliveryFee / itemsByRestaurantGid.size)

            subOrders.add(
                SubOrderDraft(
                    gid = generateGid("sub"),
                    orderGid = masterGid,
                    restaurantGid = restaurant.gid,
                    restaurantName = restaurant.name,
                    restaurantImageUrl = restaurant.image_url,
                    restaurantCategory = restaurant.category,
                    items = orderItems,
                    deliveryFee = subDeliveryFee,
                    baseTime = subBaseTime
                )
            )
        }

        if (subOrders.isEmpty()) return PlaceOrderResult.NoValidItems

        // 3. Criar Master Request
        val draft = OrderDraft(
            gid = masterGid,
            userId = profile.id,
            userName = profile.name,
            userAddress = address.address,
            userPhone = profile.phone,
            savePaymentMethod = savePaymentMethod,
            searchQuery = searchQuery,
            trackingCode = trackingCode,
            deliveryType = deliveryType,
            deliveryLatitude = address.latitude,
            deliveryLongitude = address.longitude,
            totalDeliveryFee = deliveryFee,
            totalServiceFee = serviceFee,
            subOrders = subOrders
        )

        // 4. Iniciar checkout único
        val session = orderRepository.initiateCheckout(draft) ?: return PlaceOrderResult.CheckoutUnavailable

        session.orderId?.let { orderId ->
            // Guardar mapeamentos para o pedido principal
            orderRepository.saveOrderSearchQuery(orderId.toString(), searchQuery)
            // Para múltiplos restaurantes, usamos o primeiro restaurante como referência para a UI
            subOrders.firstOrNull()?.let { firstSub ->
                orderRepository.saveOrderRestaurantGid(orderId.toString(), firstSub.restaurantGid)
            }
        }
        return PlaceOrderResult.Started(session)
    }

    /** Gera um código de pedido numérico único com 9 dígitos */
    private fun generateTrackingCode(): String = (100000000..999999999).random().toString()

    /** Gera um identificador único de string (ULID) */
    private fun generateGid(prefix: String): String = "${prefix}_${Ulid.nextUlid()}"
}
