package org.leria.eats.project.data.remote.dto

import org.leria.eats.project.domain.model.ChatReply
import org.leria.eats.project.domain.model.CheckoutSession
import org.leria.eats.project.domain.model.DeliveryFeeQuote
import org.leria.eats.project.domain.model.OrderDraft
import org.leria.eats.project.domain.model.Restaurant

fun ChatResponse.toDomain(): ChatReply = ChatReply(
    fragment = text ?: response ?: "",
    sessionId = sessionId,
    restaurants = restaurantResults,
    products = products.ifEmpty { productResults },
    cartProducts = cartProducts,
    showCart = showCart
)

/** Restaurante a partir de GET /companies/{gid}. Sem `plan`, como sempre foi no detalhe. */
fun CompanyResponse.toRestaurant(): Restaurant = Restaurant(
    gid = gid,
    name = name,
    category = category,
    image_url = imageUrl,
    products = products,
    latitude = latitude,
    longitude = longitude
)

fun PaymentIntentResponse.toDomain(): CheckoutSession = CheckoutSession(
    url = url,
    autoPaid = auto_paid,
    orderId = order_id,
    paymentIntentId = payment_intent_id,
    publishableKey = publishableKey,
    clientSecret = clientSecret,
    customerId = customerId,
    ephemeralKey = ephemeralKey
)

fun DeliveryFeeResponse.toDomain(): DeliveryFeeQuote = DeliveryFeeQuote(
    distanceKm = distance_km,
    deliveryFee = delivery_fee,
    tier = tier
)

fun OrderDraft.toRequest(): OrderRequest = OrderRequest(
    gid = gid,
    user_id = userId,
    user_name = userName,
    user_address = userAddress,
    user_phone = userPhone,
    save_payment_method = savePaymentMethod,
    search_query = searchQuery,
    tracking_code = trackingCode,
    deliveryType = deliveryType,
    deliveryLatitude = deliveryLatitude,
    deliveryLongitude = deliveryLongitude,
    totalDeliveryFee = totalDeliveryFee,
    totalServiceFee = totalServiceFee,
    subOrders = subOrders.map { sub ->
        SubOrderRequest(
            gid = sub.gid,
            orderGid = sub.orderGid,
            restaurantGid = sub.restaurantGid,
            restaurantName = sub.restaurantName,
            restaurantImageUrl = sub.restaurantImageUrl,
            restaurantCategory = sub.restaurantCategory,
            items = sub.items.map { item ->
                OrderItemRequest(
                    product_gid = item.productGid,
                    quantity = item.quantity,
                    observation = item.observation,
                    product_name = item.productName,
                    price = item.price,
                    image_url = item.imageUrl,
                    description = item.description
                )
            },
            deliveryFee = sub.deliveryFee,
            baseTime = sub.baseTime
        )
    }
)
