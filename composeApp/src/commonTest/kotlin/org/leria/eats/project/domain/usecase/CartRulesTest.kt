package org.leria.eats.project.domain.usecase

import org.leria.eats.project.domain.model.Product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CartRulesTest {
    private val pizza = Product(gid = "pizza", name = "Pizza", quantity = 1)
    private val sumo = Product(gid = "sumo", name = "Sumo", quantity = 1)
    private val caixa = Product(gid = "caixa", name = "Caixa", quantity = 1, isSurpriseBox = true)

    @Test
    fun caixaSurpresaEExclusiva() {
        assertFalse(CartRules.violatesSurpriseBoxExclusivity(emptyList(), caixa))
        assertFalse(CartRules.violatesSurpriseBoxExclusivity(listOf(pizza), sumo))
        assertTrue(CartRules.violatesSurpriseBoxExclusivity(listOf(pizza), caixa))
        assertTrue(CartRules.violatesSurpriseBoxExclusivity(listOf(caixa), pizza))
        // mais unidades do próprio item continua permitido
        assertFalse(CartRules.violatesSurpriseBoxExclusivity(listOf(caixa), caixa))
    }

    @Test
    fun adicionarSomaQuantidadeDoMesmoProduto() {
        val cart = CartRules.addProduct(listOf(pizza, sumo), pizza.copy(quantity = 2))
        assertEquals(listOf(3, 1), cart.map { it.quantity })
        assertEquals(listOf("pizza", "sumo", "caixa"), CartRules.addProduct(cart, caixa).map { it.gid })
    }

    @Test
    fun removerTiraUmaUnidadeEDescartaZerados() {
        val cart = listOf(pizza.copy(quantity = 2), sumo)
        assertEquals(listOf(1, 1), CartRules.removeOneUnit(cart, pizza).map { it.quantity })
        assertEquals(listOf("pizza"), CartRules.removeOneUnit(cart, sumo).map { it.gid })
    }

    @Test
    fun sacolaDaIaDescartaItensZerados() {
        assertEquals(listOf("pizza"), CartRules.sanitizeAiCart(listOf(pizza, sumo.copy(quantity = 0))).map { it.gid })
    }
}
