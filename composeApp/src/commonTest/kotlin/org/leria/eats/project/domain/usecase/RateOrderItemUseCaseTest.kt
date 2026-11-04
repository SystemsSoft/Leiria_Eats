package org.leria.eats.project.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RateOrderItemUseCaseTest {

    @Test
    fun usaOsGidsDaApiQuandoVieram() {
        val orders = FakeOrderRepository()
        runSuspend { RateOrderItemUseCase(orders)("7", "p1", "r1", "Pizza", 5) }
        assertEquals(mapOf("7::Pizza" to 5), orders.orderItemRatings.value)
        assertEquals(listOf(listOf<Any>("7", "r1", "p1", 5)), orders.submittedRatings)
    }

    @Test
    fun completaGidsVaziosComOsGuardadosNoCheckout() {
        val orders = FakeOrderRepository()
        orders.orderProductGids.value = mapOf("7::Pizza" to "p-local")
        orders.orderRestaurantGids.value = mapOf("7" to "r-local")
        runSuspend { RateOrderItemUseCase(orders)("7", "", "", "Pizza", 4) }
        assertEquals(listOf(listOf<Any>("7", "r-local", "p-local", 4)), orders.submittedRatings)
    }

    @Test
    fun semGidGuardaSoLocalmente() {
        val orders = FakeOrderRepository()
        runSuspend { RateOrderItemUseCase(orders)("7", "", "r1", "Pizza", 3) }
        assertEquals(mapOf("7::Pizza" to 3), orders.orderItemRatings.value)
        assertTrue(orders.submittedRatings.isEmpty())
    }
}
