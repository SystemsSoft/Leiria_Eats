package org.leria.eats.project.data.remote.dto

import org.leria.eats.project.domain.model.OrderDraft
import org.leria.eats.project.domain.model.OrderItemDraft
import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.domain.model.SubOrderDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MappersTest {

    @Test
    fun chunkDoChatUsaTextoOuResponse() {
        assertEquals("olá", ChatResponse(text = "olá", response = "x").toDomain().fragment)
        assertEquals("x", ChatResponse(response = "x").toDomain().fragment)
        assertEquals("", ChatResponse().toDomain().fragment)
    }

    @Test
    fun produtosDoChatCaemParaProductResults() {
        val a = Product(gid = "a")
        val b = Product(gid = "b")
        assertEquals(listOf(a), ChatResponse(products = listOf(a), productResults = listOf(b)).toDomain().products)
        assertEquals(listOf(b), ChatResponse(productResults = listOf(b)).toDomain().products)
    }

    @Test
    fun detalheDoRestauranteNaoTrazPlano() {
        val restaurant = CompanyResponse(gid = "r1", name = "R", imageUrl = "img", plan = "SMART").toRestaurant()
        assertEquals("img", restaurant.image_url)
        assertNull(restaurant.plan)
    }

    @Test
    fun rascunhoDoPedidoViraRequest() {
        val draft = OrderDraft(
            gid = "mst_1", userId = "U-1", userName = "Ana", userAddress = "Rua A", userPhone = "9",
            savePaymentMethod = false, searchQuery = "pizza", trackingCode = "123456789",
            deliveryType = "delivery", deliveryLatitude = 1.0, deliveryLongitude = 2.0,
            totalDeliveryFee = 3.0, totalServiceFee = 0.5,
            subOrders = listOf(
                SubOrderDraft(
                    gid = "sub_1", orderGid = "mst_1", restaurantGid = "r1", restaurantName = "R",
                    restaurantImageUrl = null, restaurantCategory = "Pizza", deliveryFee = 3.0, baseTime = 20,
                    items = listOf(OrderItemDraft("p1", 2, null, "Pizza", 5.0, null, "desc"))
                )
            )
        )
        val request = draft.toRequest()
        assertEquals("U-1", request.user_id)
        assertEquals("pizza", request.search_query)
        assertEquals("123456789", request.tracking_code)
        assertEquals("r1", request.subOrders.single().restaurantGid)
        assertEquals(OrderItemRequest("p1", 2, null, "Pizza", 5.0, null, "desc"), request.subOrders.single().items.single())
    }
}
