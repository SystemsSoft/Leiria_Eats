package org.leria.eats.project.domain.usecase

import org.leria.eats.project.domain.model.Address
import org.leria.eats.project.domain.model.CheckoutSession
import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.domain.model.Restaurant
import org.leria.eats.project.domain.model.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlaceOrderUseCaseTest {
    private val r1 = Restaurant(gid = "r1", name = "Pizzaria", category = "Pizza", image_url = "img1")
    private val r2 = Restaurant(gid = "r2", name = "Sushi", category = "Japonesa")
    private val profile = UserProfile(id = "U-1", name = "Ana", phone = "999")
    private val address = Address(name = "Casa", address = "Rua A", latitude = 1.0, longitude = 2.0)

    private val cart = listOf(
        Product(gid = "a", name = "Margherita", price = 5.0, quantity = 2, restaurant_gid = "r1", preparationTime = "15 min"),
        Product(gid = "b", name = "Calzone", price = 7.0, restaurant_gid = "r1", preparationTime = "20"),
        Product(gid = "c", name = "Temaki", price = 9.0, restaurant_gid = "r2"),
    )

    private fun place(
        orders: FakeOrderRepository,
        restaurants: FakeRestaurantRepository = FakeRestaurantRepository(listOf(r2)),
        ai: FakeAiChatRepository = FakeAiChatRepository(),
        aiSessionId: String? = "sess",
    ) = runSuspend {
        PlaceOrderUseCase(orders, restaurants, ai)(
            cartItems = cart,
            cartRestaurants = listOf(r1),
            profile = profile,
            address = address,
            savePaymentMethod = false,
            deliveryType = "delivery",
            deliveryFee = 5.0,
            serviceFee = 1.0,
            deliveryFeesMap = mapOf("r1" to 2.0),
            searchQuery = "pizza",
            aiSessionId = aiSessionId,
        )
    }

    @Test
    fun montaUmSubPedidoPorRestaurante() {
        val orders = FakeOrderRepository(checkoutSession = CheckoutSession(orderId = 42, autoPaid = true))
        val restaurants = FakeRestaurantRepository(listOf(r2))
        val ai = FakeAiChatRepository()

        val result = place(orders, restaurants, ai)

        assertEquals(CheckoutSession(orderId = 42, autoPaid = true), assertIs<PlaceOrderResult.Started>(result).session)
        assertEquals(listOf("sess"), ai.confirmedSessions)
        // r1 já estava na sacola; só r2 é buscado
        assertEquals(listOf("r2"), restaurants.requestedGids)

        val draft = orders.drafts.single()
        assertEquals("U-1", draft.userId)
        assertEquals("Rua A", draft.userAddress)
        assertEquals(1.0, draft.deliveryLatitude)
        assertEquals("pizza", draft.searchQuery)
        assertTrue(draft.gid.startsWith("mst_"))
        assertEquals(9, draft.trackingCode.length)

        val (sub1, sub2) = draft.subOrders
        assertEquals("r1", sub1.restaurantGid)
        assertEquals(draft.gid, sub1.orderGid)
        assertEquals(listOf("a", "b"), sub1.items.map { it.productGid })
        assertEquals(2.0, sub1.deliveryFee) // valor do mapa
        assertEquals(20, sub1.baseTime) // maior tempo de preparo
        assertEquals("r2", sub2.restaurantGid)
        assertEquals(2.5, sub2.deliveryFee) // sem valor no mapa: taxa total / nº de restaurantes
        assertEquals(0, sub2.baseTime)

        assertEquals(mapOf("42" to "pizza"), orders.orderSearchQueries.value)
        assertEquals(mapOf("42" to "r1"), orders.orderRestaurantGids.value)
    }

    @Test
    fun semSessaoDaIaNaoConfirmaNada() {
        val ai = FakeAiChatRepository()
        place(FakeOrderRepository(), ai = ai, aiSessionId = null)
        assertTrue(ai.confirmedSessions.isEmpty())
    }

    @Test
    fun restauranteDesconhecidoFicaDeFora() {
        val orders = FakeOrderRepository()
        place(orders, restaurants = FakeRestaurantRepository())
        assertEquals(listOf("r1"), orders.drafts.single().subOrders.map { it.restaurantGid })
    }

    @Test
    fun nenhumItemValidoNaoIniciaCheckout() {
        val orders = FakeOrderRepository()
        val result = runSuspend {
            PlaceOrderUseCase(orders, FakeRestaurantRepository(), FakeAiChatRepository())(
                cartItems = listOf(Product(gid = "x", restaurant_gid = null)),
                cartRestaurants = emptyList(),
                profile = profile,
                address = address,
                savePaymentMethod = false,
                deliveryType = "delivery",
                deliveryFee = 0.0,
                serviceFee = 0.0,
                deliveryFeesMap = emptyMap(),
                searchQuery = "",
                aiSessionId = null,
            )
        }
        assertEquals(PlaceOrderResult.NoValidItems, result)
        assertTrue(orders.drafts.isEmpty())
    }

    @Test
    fun servidorSemSessaoDePagamento() {
        val orders = FakeOrderRepository(checkoutSession = null)
        assertEquals(PlaceOrderResult.CheckoutUnavailable, place(orders))
        assertTrue(orders.orderSearchQueries.value.isEmpty())
    }
}
