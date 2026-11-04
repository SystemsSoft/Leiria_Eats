package org.leria.eats.project.domain.usecase

import org.leria.eats.project.domain.model.Product
import kotlin.test.Test
import kotlin.test.assertEquals

class SugestoesDaLigacaoTest {
    private fun p(gid: String) = Product(gid = gid, name = gid)
    private fun gids(l: List<Product>) = l.map { it.gid }

    @Test
    fun primeiraListaEntraComoVeio() {
        assertEquals(listOf("a", "b"), gids(mesclarSugestoesDaLigacao(emptyList(), listOf(p("a"), p("b")))))
    }

    @Test
    fun escolherUmCardNaoEncolheAVitrine() {
        val vitrine = listOf(p("carne"), p("frango"), p("vegetariana"))
        val depoisDoPedido = listOf(p("vegetariana"))
        assertEquals(listOf("carne", "frango", "vegetariana"), gids(mesclarSugestoesDaLigacao(vitrine, depoisDoPedido)))
    }

    @Test
    fun listaIgualMantemAVitrine() {
        val vitrine = listOf(p("a"), p("b"))
        assertEquals(listOf("a", "b"), gids(mesclarSugestoesDaLigacao(vitrine, listOf(p("b"), p("a")))))
    }

    @Test
    fun pedirAlgoForaDaVitrineSubstituiPelaNovaLista() {
        val vitrine = listOf(p("carne"), p("frango"))
        assertEquals(listOf("pizza1", "pizza2"), gids(mesclarSugestoesDaLigacao(vitrine, listOf(p("pizza1"), p("pizza2")))))
    }

    @Test
    fun listaVaziaNuncaApagaAVitrine() {
        assertEquals(listOf("a"), gids(mesclarSugestoesDaLigacao(listOf(p("a")), emptyList())))
    }
}
